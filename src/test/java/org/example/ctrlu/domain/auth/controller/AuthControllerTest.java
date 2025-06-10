package org.example.ctrlu.domain.auth.controller;

import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.nio.charset.StandardCharsets;

import org.example.ctrlu.config.TestMySQLConfig;
import org.example.ctrlu.config.TestRedisConfig;
import org.example.ctrlu.domain.auth.application.AuthService;
import org.example.ctrlu.domain.auth.application.MailService;
import org.example.ctrlu.domain.auth.dto.request.SignupRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.fasterxml.jackson.databind.ObjectMapper;

@Testcontainers
@SpringBootTest
@Transactional
@AutoConfigureMockMvc
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AuthControllerTest {
	private static final String LOGIN_URL = "http://ctrlu.site/login";
	private static final String ERROR_URL = "http://ctrlu.site/error";
	@Autowired
	private MockMvc mockMvc;
	@MockitoBean
	private MailService mailService;
	@MockitoBean
	private AuthService authService;
	@Autowired
	private ObjectMapper objectMapper;
	static final MySQLContainer<?> mySQLContainer = TestMySQLConfig.MYSQL_CONTAINER;
	// static final GenericContainer<?> redisContainer = TestRedisConfig.REDIS_CONTAINER;

	@Container // Testcontainers가 이 컨테이너의 생명주기를 관리하도록 합니다.
	public static GenericContainer<?> redisContainer = new GenericContainer<>("redis:7-alpine")
		.withExposedPorts(6379);

    @DynamicPropertySource
    public static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mySQLContainer::getJdbcUrl);
        registry.add("spring.datasource.username", mySQLContainer::getUsername);
        registry.add("spring.datasource.password", mySQLContainer::getPassword);
        registry.add("spring.datasource.driver-class-name", mySQLContainer::getDriverClassName);

		registry.add("spring.data.redis.host", redisContainer::getHost);
		registry.add("spring.data.redis.port", () -> redisContainer.getMappedPort(6379));
    }

	@Test
	@DisplayName("회원가입에 성공한다.")
	void signup_success() throws Exception {
		// given
		SignupRequest request = new SignupRequest("test1@example.com", "password123!", "tester");
		String requestJson = objectMapper.writeValueAsString(request);

		MockMultipartFile imageFile = new MockMultipartFile(
			"request",
			"request.json",
			"application/json",
			requestJson.getBytes(StandardCharsets.UTF_8)
		);

		MockMultipartFile jsonPart = new MockMultipartFile(
			"userImage",
			"profile.png",
			"image/png",
			"fake image content".getBytes()
		);

		// when & then
		mockMvc.perform(multipart("/auth/signup")
				.file(jsonPart)
				.file(imageFile)
				.contentType(MediaType.MULTIPART_FORM_DATA_VALUE))
			.andExpect(status().isOk());
	}

	@Test
	@DisplayName("이메일 인증 성공 시 로그인 페이지로 리다이렉트한다.")
	void verifyEmail_ShouldRedirectToLogin_WhenSuccess() throws Exception {
		// given
		String token = "valid_token";
		when(authService.verifyEmail(token)).thenReturn(true);

		// when + then
		mockMvc.perform(get("/auth/verify").param("token", token))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl(LOGIN_URL));
	}

	@Test
	@DisplayName("이메일 인증 실패 시 만료 페이지로 리다이렉트한다.")
	void verifyEmail_ShouldRedirectToError_WhenFailed() throws Exception {
		// given
		String token = "expired_token";
		when(authService.verifyEmail(token)).thenReturn(false);

		// when + then
		mockMvc.perform(get("/auth/verify").param("token", token))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl(ERROR_URL));
	}
}