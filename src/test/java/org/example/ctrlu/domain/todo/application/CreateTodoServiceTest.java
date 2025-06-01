package org.example.ctrlu.domain.todo.application;

import org.example.ctrlu.domain.friendship.repository.FriendShipRepository;
import org.example.ctrlu.domain.todo.dto.request.CreateTodoRequest;
import org.example.ctrlu.domain.todo.dto.response.CreateTodoResponse;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.example.ctrlu.domain.todo.exception.TodoErrorCode.ALREADY_EXIST_IN_PROGRESS_TODO;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

public class CreateTodoServiceTest {

    /**
     * 할 일 생성 테스트 시나리오
     * - 할 일을 성공적으로 생성
     * - 포기/삭제/완료한 할 일을 존재할 때 성공
     * - 진행 중인 할 일이 존재할 때 실패
     */

    private TodoRepository todoRepository;
    private UserRepository userRepository;
    private AwsS3Service awsS3Service;
    private TodoService todoService;
    private FriendShipRepository friendShipRepository;

    private final long userId = 1L;
    private final String title = "할 일 제목";
    private final LocalTime challengeTime = LocalTime.of(9, 30);
    private final String uploadedImageUrl = "https://s3-bucket/test-image.png";
    private final MockMultipartFile startImage = new MockMultipartFile("startImage", "image.png", "image/png", "fake-image".getBytes());

    private final User user = User.builder()
            .nickname("닉네임")
            .email("email@test.com")
            .password("password")
            .build();

    private final CreateTodoRequest request = new CreateTodoRequest(title, challengeTime);

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

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(awsS3Service.uploadImage(startImage)).willReturn(uploadedImageUrl);
    }

    @DisplayName("할 일 생성 성공 - 진행 중인 할 일이 없을 때")
    @Test
    void createTodo_success() {
        // given
        given(todoRepository.findAllByUserIdAndStatus(userId, TodoStatus.IN_PROGRESS))
                .willReturn(Collections.emptyList());

        Todo savedTodo = mock(Todo.class);
        given(savedTodo.getId()).willReturn(42L);
        given(todoRepository.save(any(Todo.class))).willReturn(savedTodo);

        // when
        CreateTodoResponse response = todoService.createTodo(userId, request, startImage);

        // then
        assertThat(response.todoId()).isEqualTo(42L);
    }

    @DisplayName("할 일 생성 실패 - 진행 중인 할 일이 존재할 때")
    @Test
    void createTodo_fail_alreadyInProgress() {
        // given
        Todo inProgressTodo = mock(Todo.class);
        given(todoRepository.findAllByUserIdAndStatus(userId, TodoStatus.IN_PROGRESS))
                .willReturn(List.of(inProgressTodo));

        // when & then
        TodoException exception = assertThrows(TodoException.class, () -> todoService.createTodo(userId, request, startImage));

        assertThat(exception.getExceptionStatus()).isEqualTo(ALREADY_EXIST_IN_PROGRESS_TODO);
    }
}
