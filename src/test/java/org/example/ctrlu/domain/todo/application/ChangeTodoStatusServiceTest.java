package org.example.ctrlu.domain.todo.application;

import org.example.ctrlu.domain.friendship.repository.FriendShipRepository;
import org.example.ctrlu.domain.todo.dto.request.CompleteTodoRequest;
import org.example.ctrlu.domain.todo.entity.Todo;
import org.example.ctrlu.domain.todo.entity.TodoStatus;
import org.example.ctrlu.domain.todo.exception.TodoException;
import org.example.ctrlu.domain.todo.repository.TodoRepository;
import org.example.ctrlu.domain.user.entity.User;
import org.example.ctrlu.domain.user.repository.UserRepository;
import org.example.ctrlu.global.s3.AwsS3Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.mock.web.MockMultipartFile;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.example.ctrlu.domain.todo.exception.TodoErrorCode.NOT_IN_PROGRESS_TODO;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class ChangeTodoStatusServiceTest {

    /**
     * 할 일 완료(포기) 테스트 시나리오
     * - 진행 중인 할 일 성공
     * - 이미 완료/포기/삭제한 할 일 실패
     */

    private TodoService todoService;
    private TodoRepository todoRepository;
    private UserRepository userRepository;
    private AwsS3Service awsS3Service;
    private FriendShipRepository friendShipRepository;

    private final long userId = 1L;
    private final long todoId = 100L;

    private User user;
    private Todo todo;

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

        user = User.builder().nickname("닉네임").email("test@gmail.com").password("pass").build();
        todo = mock(Todo.class);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(todoRepository.findById(todoId)).willReturn(Optional.of(todo));
        given(todo.getUser()).willReturn(user);
    }

    @Nested
    @DisplayName("할 일 완료")
    class CompleteTodoTest {

        @Test
        @DisplayName("진행 중인 할 일 완료 성공")
        void complete_inProgress_success() {
            // given
            given(todo.getStatus()).willReturn(TodoStatus.IN_PROGRESS);
            CompleteTodoRequest request = new CompleteTodoRequest(3600000);
            MockMultipartFile endImage = new MockMultipartFile("endImage", "end.png", "image/png", "test".getBytes());
            given(awsS3Service.uploadImage(endImage)).willReturn("url");

            // when
            todoService.completeTodo(userId, todoId, request, endImage);

            // then
            verify(todo).complete(eq(3600000), eq("url"));
        }

        @ParameterizedTest
        @EnumSource(value = TodoStatus.class, names = {"COMPLETED", "GIVEN_UP", "DELETED"})
        @DisplayName("이미 완료/포기/삭제된 할 일 완료 시 실패")
        void complete_invalidStatus_fail(TodoStatus status) {
            // given
            given(todo.getStatus()).willReturn(status);

            CompleteTodoRequest request = new CompleteTodoRequest(1000);
            MockMultipartFile endImage = new MockMultipartFile("endImage", "end.png", "image/png", "test".getBytes());

            // when & then
            assertThatThrownBy(() -> todoService.completeTodo(userId, todoId, request, endImage))
                    .isInstanceOf(TodoException.class)
                    .hasMessageContaining(NOT_IN_PROGRESS_TODO.getMessage(), status.name());
        }
    }

    @Nested
    @DisplayName("할 일 포기")
    class GiveUpTodoTest {

        @Test
        @DisplayName("진행 중인 할 일 포기 성공")
        void giveUp_inProgress_success() {
            // given
            given(todo.getStatus()).willReturn(TodoStatus.IN_PROGRESS);

            // when
            todoService.giveUpTodo(userId, todoId);

            // then
            verify(todo).giveUp(any(LocalDateTime.class));
        }

        @ParameterizedTest
        @EnumSource(value = TodoStatus.class, names = {"COMPLETED", "GIVEN_UP", "DELETED"})
        @DisplayName("이미 완료/포기/삭제된 할 일 포기 시 실패")
        void giveUp_invalidStatus_fail(TodoStatus status) {
            // given
            given(todo.getStatus()).willReturn(status);

            // when & then
            assertThatThrownBy(() -> todoService.giveUpTodo(userId, todoId))
                    .isInstanceOf(TodoException.class)
                    .hasMessageContaining(NOT_IN_PROGRESS_TODO.getMessage(), status.name());
        }
    }
}
