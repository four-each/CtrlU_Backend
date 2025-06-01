package org.example.ctrlu.domain.todo.application;

import org.example.ctrlu.domain.friendship.repository.FriendShipRepository;
import org.example.ctrlu.domain.todo.dto.response.GetTodoResponse;
import org.example.ctrlu.domain.todo.entity.Todo;
import org.example.ctrlu.domain.todo.entity.TodoStatus;
import org.example.ctrlu.domain.todo.exception.TodoException;
import org.example.ctrlu.domain.todo.repository.TodoRepository;
import org.example.ctrlu.domain.user.entity.User;
import org.example.ctrlu.domain.user.repository.UserRepository;
import org.example.ctrlu.global.s3.AwsS3Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.*;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.example.ctrlu.domain.todo.exception.TodoErrorCode.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class GetTodoServiceTest {

    /**
     * 할 일 상세조회 테스트 시나리오
     * - 할 일을 성공적으로 조회 (진행중,완료)
     * - 포기한 할 일을 조회 시 실패
     * - 삭제한 할 일을 조회 시 실패
     */

    public static final String TODO_TITLE = "할 일 제목";
    public static final LocalTime TODO_CHALLENGE_TIME = LocalTime.of(10, 30);
    public static final String TEST_IMAGE = "test-image.png";
    public static final String USER_NICKNAME = "닉네임";
    public static final String USER_PASSWORD = "ctrlu1234";
    public static final String USER_EMAIL = "ctrlu@gmail.com";

    private TodoRepository todoRepository;
    private UserRepository userRepository;
    private AwsS3Service awsS3Service;
    private TodoService todoService;
    private FriendShipRepository friendShipRepository;

    public static final User user = User.builder()
            .nickname(USER_NICKNAME)
            .email(USER_EMAIL)
            .password(USER_PASSWORD)
            .build();
    public static final long userId = 1L;
    public Todo todo;
    private final long todoId = 100L;
    private final LocalDateTime createdAt = LocalDateTime.of(2025, 5, 26, 9, 0);


    @BeforeEach
    void setUp() {
        todoRepository = mock(TodoRepository.class);
        userRepository = mock(UserRepository.class);
        awsS3Service = mock(AwsS3Service.class);
        friendShipRepository = mock(FriendShipRepository.class);
        Clock fixedClock = Clock.fixed(
                LocalDateTime.of(2025, 5, 26, 10, 0).atZone(ZoneId.systemDefault()).toInstant(),
                ZoneId.systemDefault());
        todoService = new TodoService(todoRepository, userRepository, awsS3Service, friendShipRepository, fixedClock);

        todo = Todo.builder()
                .title(TODO_TITLE)
                .user(user)
                .startImage(TEST_IMAGE)
                .challengeTime(TODO_CHALLENGE_TIME)
                .build();

        ReflectionTestUtils.setField(user, "id", userId);
        ReflectionTestUtils.setField(todo,"createdAt",createdAt);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
    }

    @Test
    @DisplayName("진행 중(IN_PROGRESS) 할 일 조회 성공")
    void getTodo_inProgress_success() {
        // given
        ReflectionTestUtils.setField(todo, "status", TodoStatus.IN_PROGRESS);
        ReflectionTestUtils.setField(todo, "createdAt", createdAt); // 09:00
        given(todoRepository.findById(todoId)).willReturn(Optional.of(todo));

        // when
        GetTodoResponse response = todoService.getTodo(userId, todoId);

        // then
        assertThat(response.durationTime()).isEqualTo(60 * 60 * 1000);
    }

    @Test
    @DisplayName("완료(COMPLETED) 할 일 조회 성공")
    void getTodo_completed_success() {
        // given
        ReflectionTestUtils.setField(todo, "status", TodoStatus.COMPLETED);
        ReflectionTestUtils.setField(todo, "durationTime", 4500000);
        given(todoRepository.findById(todoId)).willReturn(Optional.of(todo));

        // when
        GetTodoResponse response = todoService.getTodo(userId, todoId);

        // then
        assertThat(response.durationTime()).isEqualTo(4500000);
    }

    @Test
    @DisplayName("포기(GIVEN_UP) 할 일 조회 성공")
    void getTodo_givenUp_success() {
        // given
        ReflectionTestUtils.setField(todo, "status", TodoStatus.GIVEN_UP);
        ReflectionTestUtils.setField(todo, "createdAt", createdAt);
        ReflectionTestUtils.setField(todo, "modifiedAt", createdAt.plusMinutes(30));
        given(todoRepository.findById(todoId)).willReturn(Optional.of(todo));

        // when
        GetTodoResponse response = todoService.getTodo(userId, todoId);

        // then
        assertThat(response.durationTime()).isEqualTo(30 * 60 * 1000);
    }

    @DisplayName("할 일 조회 실패 - 삭제한 할 일")
    @Test
    void getTodo_fail_deletedTodo() {
        // given
        ReflectionTestUtils.setField(todo, "status", TodoStatus.DELETED);
        given(todoRepository.findById(todoId)).willReturn(Optional.of(todo));

        // when
        TodoException exception = assertThrows(TodoException.class, () -> todoService.getTodo(userId, todoId));

        // then
        assertThat(exception.getMessage()).startsWith(FAIL_TO_GET_TODO.getMessage());
        assertThat(exception.getMessage()).contains(TodoStatus.DELETED.name());
    }
}