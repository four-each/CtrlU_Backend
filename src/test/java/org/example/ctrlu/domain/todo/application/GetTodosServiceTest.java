package org.example.ctrlu.domain.todo.application;

import org.example.ctrlu.domain.friendship.repository.FriendShipRepository;
import org.example.ctrlu.domain.todo.dto.response.GetTodosResponse;
import org.example.ctrlu.domain.todo.entity.Todo;
import org.example.ctrlu.domain.todo.entity.TodoStatus;
import org.example.ctrlu.domain.todo.exception.TodoErrorCode;
import org.example.ctrlu.domain.todo.exception.TodoException;
import org.example.ctrlu.domain.todo.repository.TodoRepository;
import org.example.ctrlu.domain.user.entity.User;
import org.example.ctrlu.domain.user.repository.UserRepository;
import org.example.ctrlu.global.s3.AwsS3Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.example.ctrlu.domain.todo.exception.TodoErrorCode.FAIL_TO_GET_TODO;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

public class GetTodosServiceTest {
    /**
     * - target=me 테스트
     * 진행 중, 완료, 포기 상태 각각 페이징 조회 가능
     * 삭제 상태 조회 시 예외 발생
     * - target=friend 테스트
     * 친구들의 진행 중 할 일 조회 가능
     * IN_PROGRESS가 아닐 경우 예외 발생
     * 친구 없을 경우 빈 응답 반환
     * createdAt 오름차순 정렬 확인
     */

    private TodoRepository todoRepository;
    private UserRepository userRepository;
    private AwsS3Service awsS3Service;
    private FriendShipRepository friendShipRepository;
    private TodoService todoService;
    private RedisTemplate<String, Object> redisTemplate;

    private final long userId = 1L;
    private final User user = User.builder()
            .email("test@gmail.com")
            .nickname("테스터")
            .password("pw")
            .build();

    @BeforeEach
    void setUp() {
        todoRepository = mock(TodoRepository.class);
        userRepository = mock(UserRepository.class);
        awsS3Service = mock(AwsS3Service.class);
        friendShipRepository = mock(FriendShipRepository.class);
        redisTemplate = mock(RedisTemplate.class);

        Clock fixedClock = Clock.fixed(LocalDateTime.of(2025, 5, 26, 10, 0).atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
        todoService = new TodoService(todoRepository, userRepository, awsS3Service, friendShipRepository, fixedClock, redisTemplate);

        ReflectionTestUtils.setField(user, "id", userId);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
    }

    @Test
    @DisplayName("내 할 일 조회 성공 - IN_PROGRESS")
    void getMyTodos_success_sortedByCreatedAt() {
        // given
        Todo todo = makeTodo(user, LocalDateTime.of(2025, 5, 26, 9, 0), TodoStatus.IN_PROGRESS);
        Page<Todo> page = new PageImpl<>(List.of(todo));

        given(todoRepository.findAllByUserIdAndStatus(eq(userId), eq(TodoStatus.IN_PROGRESS), any()))
                .willReturn(page);

        // when
        GetTodosResponse response = todoService.getTodos(userId, "me", TodoStatus.IN_PROGRESS, PageRequest.of(0, 10));

        // then
        assertThat(response.todos()).hasSize(1);
        assertThat(response.todos().get(0).id()).isEqualTo(todo.getId());
        assertThat(response.totalElementCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("내 할 일 조회 실패 - 삭제 상태 요청")
    void getMyTodos_fail_deletedStatus() {
        // when
        TodoException exception = assertThrows(TodoException.class, () ->
                todoService.getTodos(userId, "me", TodoStatus.DELETED, PageRequest.of(0, 10))
        );

        // then
        assertThat(exception.getMessage()).startsWith(FAIL_TO_GET_TODO.getMessage());
        assertThat(exception.getMessage()).contains(TodoStatus.DELETED.name());
    }

    @Test
    @DisplayName("친구의 할 일 조회 성공 - createdAt 오름차순")
    void getFriendTodos_success_sortedByCreatedAt() {
        // given
        long friendId1 = 2L;
        long friendId2 = 3L;
        List<Long> friendIds = List.of(friendId1, friendId2);
        long createdLateTodoId = 201L;
        long createdAheadTodoId = 202L;
        Todo friendTodo1 = makeTodoWithId(friendId1, LocalDateTime.of(2025, 5, 26, 8, 0), TodoStatus.IN_PROGRESS, createdAheadTodoId);
        Todo friendTodo2 = makeTodoWithId(friendId2, LocalDateTime.of(2025, 5, 26, 9, 0), TodoStatus.IN_PROGRESS, createdLateTodoId);
        Page<Todo> page = new PageImpl<>(List.of(friendTodo1, friendTodo2));

        given(friendShipRepository.findAcceptedFriendIds(userId)).willReturn(friendIds);
        given(todoRepository.findAllByUserIdInAndStatus(eq(friendIds), eq(TodoStatus.IN_PROGRESS), any()))
                .willReturn(page);

        // when
        GetTodosResponse response = todoService.getTodos(userId, "friend", TodoStatus.IN_PROGRESS, PageRequest.of(0, 10));

        // then
        assertThat(response.todos()).hasSize(2);
        assertThat(response.todos().get(0).id()).isEqualTo(createdAheadTodoId);
    }

    @DisplayName("친구의 할 일 조회 실패 - 진행중 상태가 아닐 경우 예외 발생")
    @ParameterizedTest(name = "{index} - 상태: {0}")
    @EnumSource(value = TodoStatus.class, names = {"IN_PROGRESS"}, mode = EnumSource.Mode.EXCLUDE)
    void getFriendTodos_fail_notInProgress(TodoStatus status) {
        // when
        TodoException exception = assertThrows(TodoException.class, () ->
                todoService.getTodos(userId, "friend", status, PageRequest.of(0, 10))
        );

        // then
        assertThat(exception.getMessage()).isEqualTo(TodoErrorCode.FAIL_TO_GET_FRIEND_TODOS.getMessage());
    }

    @Test
    @DisplayName("친구 없음 - 빈 리스트 반환")
    void getFriendTodos_empty_whenNoFriends() {
        given(friendShipRepository.findAcceptedFriendIds(userId)).willReturn(List.of());

        // when
        GetTodosResponse response = todoService.getTodos(userId, "friend", TodoStatus.IN_PROGRESS, PageRequest.of(0, 10));

        // then
        assertThat(response.todos()).isEmpty();
        assertThat(response.totalElementCount()).isEqualTo(0);
    }

    private Todo makeTodo(User user, LocalDateTime createdAt, TodoStatus status) {
        Todo todo = Todo.builder()
                .title("test")
                .challengeTime(LocalTime.of(9, 0))
                .startImage("img.png")
                .user(user)
                .build();
        ReflectionTestUtils.setField(todo, "id", (long) (Math.random() * 1000));
        ReflectionTestUtils.setField(todo, "createdAt", createdAt);
        ReflectionTestUtils.setField(todo, "status", status);
        return todo;
    }

    private Todo makeTodoWithId(Long userId, LocalDateTime createdAt, TodoStatus status, Long todoId) {
        User friend = User.builder().email("f@f.com").password("123").nickname("친구").build();
        ReflectionTestUtils.setField(friend, "id", userId);

        Todo todo = Todo.builder()
                .title("friend")
                .challengeTime(LocalTime.of(8, 30))
                .startImage("img.png")
                .user(friend)
                .build();
        ReflectionTestUtils.setField(todo, "id", todoId);
        ReflectionTestUtils.setField(todo, "createdAt", createdAt);
        ReflectionTestUtils.setField(todo, "status", status);
        return todo;
    }
}
