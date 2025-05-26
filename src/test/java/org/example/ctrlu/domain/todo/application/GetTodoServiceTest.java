package org.example.ctrlu.domain.todo.application;

import org.example.ctrlu.domain.todo.dto.response.GetTodoResponse;
import org.example.ctrlu.domain.todo.entity.Todo;
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
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class GetTodoServiceTest {
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

    @BeforeEach
    void setUp() {
        todoRepository = mock(TodoRepository.class);
        userRepository = mock(UserRepository.class);
        awsS3Service = mock(AwsS3Service.class);
        Clock fixedClock = Clock.fixed(
                LocalDateTime.of(2025, 5, 26, 10, 0).atZone(ZoneId.systemDefault()).toInstant(),
                ZoneId.systemDefault());
        todoService = new TodoService(todoRepository, userRepository, awsS3Service, fixedClock);
    }

    @DisplayName("할 일 조회 성공")
    @Test
    void getTodo_success() {
        // given
        long userId = 1L;
        long todoId = 100L;

        User user = User.builder()
                .nickname(USER_NICKNAME)
                .email(USER_EMAIL)
                .password(USER_PASSWORD)
                .build();
        ReflectionTestUtils.setField(user,"id",userId);

        LocalDateTime createdAt = LocalDateTime.of(2025, 5, 26, 9, 0);
        Todo todo = Todo.builder()
                .title(TODO_TITLE)
                .user(user)
                .startImage(TEST_IMAGE)
                .challengeTime(TODO_CHALLENGE_TIME)
                .build();
        ReflectionTestUtils.setField(todo,"id",todoId);
        ReflectionTestUtils.setField(todo,"createdAt",createdAt);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(todoRepository.findById(todoId)).willReturn(Optional.of(todo));

        // when
        GetTodoResponse response = todoService.getTodo(userId, todoId);

        // then
        assertThat(response.title()).isEqualTo(TODO_TITLE);
        assertThat(response.isMine()).isTrue();
        assertThat(response.durationTime()).isEqualTo(60 * 60 * 1000); // 1시간 = 3600000ms
    }

}