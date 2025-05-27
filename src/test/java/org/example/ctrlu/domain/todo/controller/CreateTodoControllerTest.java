package org.example.ctrlu.domain.todo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.ctrlu.config.TestMySQLConfig;
import org.example.ctrlu.domain.todo.dto.request.CreateTodoRequest;
import org.example.ctrlu.domain.todo.repository.TodoRepository;
import org.example.ctrlu.domain.user.entity.User;
import org.example.ctrlu.domain.user.repository.UserRepository;
import org.example.ctrlu.global.s3.AwsS3Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Testcontainers
@SpringBootTest
@Transactional
@AutoConfigureMockMvc
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CreateTodoControllerTest {
    public static final String TODO_TITLE = "할 일 제목";
    public static final LocalTime TODO_CHALLENGE_TIME = LocalTime.of(10, 30);
    public static final String TEST_IMAGE = "test-image.png";
    public static final String USER_NICKNAME = "닉네임";
    public static final String USER_PASSWORD = "ctrlu1234";
    public static final String USER_EMAIL = "ctrlu@gmail.com";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AwsS3Service awsS3Service;

    //todo: Mock으로 하면 ConnectionFactory must not be null 오류가 남. 다른 이유를 모르겠음.
//    @MockitoBean
//    private RedisTemplate<String,Object> redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    private MockMultipartFile imageFile;

    private MockMultipartFile jsonPart;

    static final MySQLContainer<?> mySQLContainer = TestMySQLConfig.MYSQL_CONTAINER;

    @Autowired
    private TodoRepository todoRepository;

    @DynamicPropertySource
    public static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mySQLContainer::getJdbcUrl);
        registry.add("spring.datasource.username", mySQLContainer::getUsername);
        registry.add("spring.datasource.password", mySQLContainer::getPassword);
        registry.add("spring.datasource.driver-class-name", mySQLContainer::getDriverClassName);
    }

    @BeforeEach
    void setUp() throws Exception {
        /**
         * @Transactional이 작동은 하지만 여러 메서드를 실행시켰을 때
         * 완벽한 독립성을 보장해주지는 않는다.
         * 따라서 Repository에 저장하는 코드는 각 메서드에 분리하여 독립성을 보장해주었다.
         */

        //given(요청)
        CreateTodoRequest requestDto = new CreateTodoRequest(TODO_TITLE, TODO_CHALLENGE_TIME);
        String requestJson = objectMapper.writeValueAsString(requestDto);

        imageFile = new MockMultipartFile(
                "startImage",
                "start.png",
                MediaType.IMAGE_JPEG_VALUE,
                new ClassPathResource(TEST_IMAGE).getInputStream()
        );

        jsonPart = new MockMultipartFile(
                "request",
                "request",
                MediaType.APPLICATION_JSON_VALUE,
                requestJson.getBytes()
        );

        //given(s3)
        given(awsS3Service.uploadImage(any())).willReturn("https://fake-s3-url.com/fake.png");
    }

    @DisplayName("정상적인 요청으로 할 일이 생성된다")
    @Test
    void createTodo_success() throws Exception {
        //given
        User newUser = User.builder().nickname(USER_NICKNAME).password(USER_PASSWORD).email(USER_EMAIL).build();
        User savedUser = userRepository.save(newUser);

        // when & then
        //todo: 나중에 인증인가 구현 후 userId 받아오는 경로 수정
        mockMvc.perform(multipart("/todos?userId="+savedUser.getId())
                        .file(jsonPart)
                        .file(imageFile)
                        .contentType(MediaType.MULTIPART_FORM_DATA_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.todoId").exists());
        verify(awsS3Service, times(1)).uploadImage(any());
    }

    @DisplayName("진행 중인 Todo가 있을 경우 예외가 발생한다")
    @Test
    void createTodo_whenAlreadyExists_thenThrows() throws Exception {
        //given
        User newUser = User.builder().nickname(USER_NICKNAME).password(USER_PASSWORD).email(USER_EMAIL).build();
        User savedUser = userRepository.save(newUser);

        //todo: 나중에 인증인가 구현 후 userId 받아오는 경로 수정
        mockMvc.perform(multipart("/todos?userId="+savedUser.getId())
                        .file(jsonPart)
                        .file(imageFile)
                        .contentType(MediaType.MULTIPART_FORM_DATA_VALUE))
                .andExpect(status().isOk());

        // when & then
        mockMvc.perform(multipart("/todos")
                        .file(jsonPart)
                        .file(imageFile)
                        .contentType(MediaType.MULTIPART_FORM_DATA_VALUE))
                .andExpect(status().isBadRequest());
        verify(awsS3Service, times(1)).uploadImage(any());
    }
}