package org.example.ctrlu.domain.auth.application;

import static org.assertj.core.api.Assertions.*;

import org.example.ctrlu.config.TestMySQLConfig;
import org.example.ctrlu.config.TestRedisConfig;
import org.example.ctrlu.domain.auth.dto.request.DeleteUserRequest;
import org.example.ctrlu.domain.auth.dto.request.SigninRequest;
import org.example.ctrlu.domain.auth.dto.response.TokenInfo;
import org.example.ctrlu.domain.auth.util.JWTUtil;
import org.example.ctrlu.domain.user.entity.User;
import org.example.ctrlu.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import jakarta.servlet.http.Cookie;

@SpringBootTest
@Testcontainers
@Transactional
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class AuthIntegrationTest {
	@MockitoBean
	private MailService mailService;
	@Autowired
	private AuthService authService;
	@Autowired
	private UserRepository userRepository;
	@Autowired
	private RedisTemplate<String, String> redisTemplate;
	@Autowired
	private JWTUtil jwtUtil;
	@Autowired
	private PasswordEncoder passwordEncoder;
	private User user;
	private String refreshToken;

	static final MySQLContainer<?> mySQLContainer = TestMySQLConfig.MYSQL_CONTAINER;
	static final GenericContainer<?> redisContainer = TestRedisConfig.REDIS_CONTAINER;

	@DynamicPropertySource
	public static void overrideProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", mySQLContainer::getJdbcUrl);
		registry.add("spring.datasource.username", mySQLContainer::getUsername);
		registry.add("spring.datasource.password", mySQLContainer::getPassword);
		registry.add("spring.datasource.driver-class-name", mySQLContainer::getDriverClassName);

		registry.add("spring.data.redis.host", redisContainer::getHost);
		registry.add("spring.data.redis.port", redisContainer::getExposedPorts);
	}

	@BeforeEach
	void setUp() {
		refreshToken = jwtUtil.createRefreshToken(30000L);
		user = User.builder()
			.email("test@test.com")
			.password(passwordEncoder.encode("password123"))
			.nickname("nickname")
			.image("image")
			.verifyToken("verifytoken")
			.build();

		User savedUser = userRepository.save(user);
		savedUser.changeUserStatusToActive();
	}

	@Test
	@DisplayName("로그인 시 리프레시 토큰을 레디스에 저장한다.")
	void login_ShouldSaveRefreshToken() {
		// given
		SigninRequest signinRequest = new SigninRequest("test@test.com", "password123");

		// when
		TokenInfo tokenInfo = authService.signin(signinRequest);

		// then
		assertThat(redisTemplate.opsForValue().get("refreshToken:" + tokenInfo.refreshToken())).isEqualTo("userId:" + user.getId());
	}


	@Test
	@DisplayName("리프레시 토큰 재발급 시 새로운 리프레시 토큰을 레디스에 저장한다.")
	void reissue_ShouldSaveRefreshToken() {
		// given
		Cookie refreshTokenCookie = new Cookie("refreshToken", refreshToken);
		redisTemplate.opsForValue().set("refreshToken:" + refreshToken, "userId:" + user.getId());

		// when
		TokenInfo tokenInfo = authService.reissue(refreshTokenCookie);

		// then
		assertThat(redisTemplate.opsForValue().get("refreshToken:" + refreshToken)).isEqualTo(null);
		assertThat(redisTemplate.opsForValue().get("refreshToken:" + tokenInfo.refreshToken())).isEqualTo("userId:" + user.getId());
	}

	@Test
	@DisplayName("회원탈퇴 시 레디스에서 리프레시 토큰을 삭제한다.")
	void deleteUser_ShouldDeleteRefreshToken() {
		// given
		DeleteUserRequest request = new DeleteUserRequest("password123");
		Cookie refreshTokenCookie = new Cookie("refreshToken", refreshToken);
		redisTemplate.opsForValue().set("refreshToken:" + refreshToken, "userId:" + user.getId());

		// when
		authService.deleteUser(user.getId(), request, refreshTokenCookie);

		// then
		assertThat(redisTemplate.opsForValue().get("refreshToken:" + refreshToken)).isEqualTo(null);
	}

	@Test
	@DisplayName("로그아웃 시 레디스에서 리프레시 토큰을 삭제한다.")
	void logout_ShouldDeleteRefreshToken() {
		// given
		Cookie refreshTokenCookie = new Cookie("refreshToken", refreshToken);
		redisTemplate.opsForValue().set("refreshToken:" + refreshToken, "userId:" + user.getId());

		// when
		authService.logout(refreshTokenCookie);

		// then
		assertThat(redisTemplate.opsForValue().get("refreshToken:" + refreshToken)).isEqualTo(null);
	}
}
