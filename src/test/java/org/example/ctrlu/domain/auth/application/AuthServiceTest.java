package org.example.ctrlu.domain.auth.application;

import static org.example.ctrlu.domain.auth.exception.AuthErrorCode.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.example.ctrlu.domain.auth.dto.request.DeleteUserRequest;
import org.example.ctrlu.domain.auth.dto.request.SigninRequest;
import org.example.ctrlu.domain.auth.dto.request.SignupRequest;
import org.example.ctrlu.domain.auth.dto.response.TokenInfo;
import org.example.ctrlu.domain.auth.exception.AuthException;
import org.example.ctrlu.domain.auth.repository.RedisTokenRepository;
import org.example.ctrlu.domain.auth.util.JWTUtil;
import org.example.ctrlu.domain.user.entity.User;
import org.example.ctrlu.domain.user.entity.UserStatus;
import org.example.ctrlu.domain.user.repository.UserRepository;
import org.example.ctrlu.global.s3.AwsS3Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.Cookie;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
	public static final String VERIFY_TOKEN = "verify_token";
	public static final String EXPIRED_TOKEN = "expired_token";
	public static final String ENCODED_PASSWORD = "encoded_password";
	public static final String IMAGE_URL = "http://s3.com/image.png";
	public static final String REFRESH_TOKEN = "valid_refreshtoken";
	private static final Long EXPIRATION_TIME = 300000L;
	private static final Long ACCESSTOKEN_EXPIRATION_TIME = (60 * 1000L) * 30; // 30분
	private static final Long REFRESHTOKEN_EXPIRATION_TIME = (60 * 1000L) * 60 * 24 * 7; // 7일

	@InjectMocks
	private AuthService authService;
	@Mock
	private MailService mailService;
	@Mock
	private AwsS3Service awsS3Service;
	@Mock
	private UserRepository userRepository;
	@Mock
	private PasswordEncoder passwordEncoder;
	@Mock
	private JWTUtil jwtUtil;
	@Mock
	private MultipartFile file;
	@Mock
	private RedisTokenRepository redisTokenRepository;

	private SignupRequest signupRequest;
	private SigninRequest signinRequest;

	@BeforeEach
	void setUp() {
		signupRequest = new SignupRequest("email@gmail.com", "password", "nickname");
		signinRequest = new SigninRequest("test@email.com", "password123");
	}

	@Test
	@DisplayName("회원가입 시 사용자가 존재하지 않으면 새로운 사용자를 생성한다.")
	void signup_ShouldCreateNewUser_WhenEmailNotExists() {
		// given
		when(userRepository.findByEmail(signupRequest.email())).thenReturn(Optional.empty());
		when(awsS3Service.uploadImage(file)).thenReturn(IMAGE_URL);
		when(passwordEncoder.encode(signupRequest.password())).thenReturn(ENCODED_PASSWORD);
		when(jwtUtil.createVerifyToken(EXPIRATION_TIME)).thenReturn(VERIFY_TOKEN);

		// when
		authService.signup(signupRequest, file);

		// then
		verify(userRepository).save(any(User.class));
		verify(mailService).sendEmail(any(User.class));
	}

	@Test
	@DisplayName("회원가입 시 사용자가 존재하고 인증된 상태라면 예외가 발생한다.")
	void signup_ShouldThrowException_WhenEmailAlreadyActive() {
		// given
		User user = User.builder().build();
		ReflectionTestUtils.setField(user, "status", UserStatus.ACTIVE);
		when(userRepository.findByEmail(signupRequest.email())).thenReturn(Optional.of(user));

		// when / then
		AuthException authException =
			assertThrows(AuthException.class, () -> authService.signup(signupRequest, file));
		assertEquals(ALREADY_EXIST_EMAIL, authException.getExceptionStatus());
	}

	@Test
	@DisplayName("회원가입 시 사용자가 존재하지만 인증 전 상태이고 이전 토큰 만료되지 않았다면 예외가 발생한다.")
	void signup_ShouldThrowException_WhenUserNonCertifiedAndTokenNotExpired() {
		// given
		User user = User.builder().build();
		ReflectionTestUtils.setField(user, "status", UserStatus.NONCERTIFIED);
		ReflectionTestUtils.setField(user, "verifyToken", VERIFY_TOKEN);

		when(jwtUtil.isExpired(VERIFY_TOKEN)).thenReturn(false);
		when(userRepository.findByEmail(signupRequest.email())).thenReturn(Optional.of(user));

		// when / then
		AuthException authException =
			assertThrows(AuthException.class, () -> authService.signup(signupRequest, file));
		assertEquals(TRY_EMAIL_VERIFICATION, authException.getExceptionStatus());
	}

	@Test
	@DisplayName("회원가입 시 사용자가 존재하지만 인증 전 상태이고 이전 토큰 만료되었다면 사용자를 갱신하고 인증 메일을 보낸다.")
	void signup_ShouldRestoreUser_WhenTokenExpired() {
		// given
		User user = User.builder().build();
		ReflectionTestUtils.setField(user, "status", UserStatus.NONCERTIFIED);
		ReflectionTestUtils.setField(user, "verifyToken", EXPIRED_TOKEN);

		when(userRepository.findByEmail(signupRequest.email())).thenReturn(Optional.of(user));
		when(jwtUtil.isExpired(EXPIRED_TOKEN)).thenReturn(true);
		when(awsS3Service.uploadImage(file)).thenReturn(IMAGE_URL);
		when(passwordEncoder.encode(signupRequest.password())).thenReturn(ENCODED_PASSWORD);
		when(jwtUtil.createVerifyToken(EXPIRATION_TIME)).thenReturn(VERIFY_TOKEN);

		// when
		authService.signup(signupRequest, file);

		// then
		assertEquals(ENCODED_PASSWORD, user.getPassword());
		assertEquals(signupRequest.nickname(), user.getNickname());
		assertEquals(IMAGE_URL, user.getImage());
		assertEquals(VERIFY_TOKEN, user.getVerifyToken());
		verify(mailService).sendEmail(user);
	}

	@Test
	@DisplayName("회원가입 시 사용자가 존재하지만 탈퇴 상태라면 사용자를 갱신하고 인증 메일을 보낸다.")
	void signup_ShouldRestoreUser_WhenUserInActive() {
		// given
		User user = User.builder().build();
		ReflectionTestUtils.setField(user, "status", UserStatus.INACTIVE);
		ReflectionTestUtils.setField(user, "verifyToken", EXPIRED_TOKEN);

		when(userRepository.findByEmail(signupRequest.email())).thenReturn(Optional.of(user));
		when(awsS3Service.uploadImage(file)).thenReturn(IMAGE_URL);
		when(passwordEncoder.encode(signupRequest.password())).thenReturn(ENCODED_PASSWORD);
		when(jwtUtil.createVerifyToken(EXPIRATION_TIME)).thenReturn(VERIFY_TOKEN);

		// when
		authService.signup(signupRequest, file);

		// then
		assertEquals(ENCODED_PASSWORD, user.getPassword());
		assertEquals(signupRequest.nickname(), user.getNickname());
		assertEquals(IMAGE_URL, user.getImage());
		assertEquals(VERIFY_TOKEN, user.getVerifyToken());
		verify(mailService).sendEmail(user);
	}

	@Test
	@DisplayName("이메일 인증 시 토큰이 유효하면 true를 반환한다.")
	void verifyEmail_ShouldReturnTrue_WhenTokenValid() {
		// given
		User user = User.builder().build();
		when(jwtUtil.isExpired(VERIFY_TOKEN)).thenReturn(false);
		when(userRepository.findByVerifyToken(VERIFY_TOKEN)).thenReturn(Optional.of(user));

		// when
		boolean result = authService.verifyEmail(VERIFY_TOKEN);

		// then
		assertTrue(result);
		assertEquals(UserStatus.ACTIVE, user.getStatus());
	}

	@Test
	@DisplayName("이메일 인증 시 토큰이 만료됐으면 false를 반환한다.")
	void verifyEmail_ShouldReturnFalse_WhenTokenExpired() {
		// given
		when(jwtUtil.isExpired(EXPIRED_TOKEN)).thenReturn(true);

		// when
		boolean result = authService.verifyEmail(EXPIRED_TOKEN);

		// then
		assertFalse(result);
		verify(userRepository, never()).findByVerifyToken(any());
	}

	@Test
	@DisplayName("이메일 인증 시 사용자가 존재하지 않으면 예외를 발생시킨다.")
	void verifyEmail_ShouldThrowException_WhenNotFoundUser() {
		// given
		when(jwtUtil.isExpired(VERIFY_TOKEN)).thenReturn(false);
		when(userRepository.findByVerifyToken(VERIFY_TOKEN)).thenReturn(Optional.empty());

		// when / then
		AuthException authException =
			assertThrows(AuthException.class, () -> authService.verifyEmail(VERIFY_TOKEN));
		assertEquals(NOT_FOUND_USER, authException.getExceptionStatus());
	}

	@Test
	@DisplayName("로그인 시 비밀번호가 틀리면 예외가 발생한다.")
	void signin_invalid_password_throwsException() {
		// given
		User user = User.builder()
			.password(ENCODED_PASSWORD)
			.build();

		when(userRepository.findByEmailAndStatus(signinRequest.email(), UserStatus.ACTIVE)).thenReturn(Optional.of(user));
		when(passwordEncoder.matches(signinRequest.password(), user.getPassword())).thenReturn(false);

		// when & then
		AuthException authException
			= assertThrows(AuthException.class, () -> authService.signin(signinRequest));
		assertEquals(INVALID_PASSWORD, authException.getExceptionStatus());
	}

	@Test
	@DisplayName("리프레시 토큰 재발급 시 새로운 토큰을 재발급한다.")
	void reissue_ShouldReissueTokens_WhenValidRefreshToken() {
		// given
		String newAccessToken = "newAccessToken";
		String newRefreshToken = "newRefreshToken";
		Cookie refreshTokenCookie = new Cookie("refreshToken", REFRESH_TOKEN);
		Long userId = 1L;
		User user = User.builder().build();
		ReflectionTestUtils.setField(user, "id", userId);

		when(jwtUtil.isExpired(REFRESH_TOKEN)).thenReturn(false);
		when(redisTokenRepository.getUserIdFromRefreshToken(REFRESH_TOKEN)).thenReturn(userId);
		when(userRepository.findByIdAndStatus(userId, UserStatus.ACTIVE)).thenReturn(Optional.of(user));
		when(jwtUtil.createAccessToken(userId, ACCESSTOKEN_EXPIRATION_TIME)).thenReturn(newAccessToken);
		when(jwtUtil.createRefreshToken(REFRESHTOKEN_EXPIRATION_TIME)).thenReturn(newRefreshToken);

		// when
		TokenInfo tokenInfo = authService.reissue(refreshTokenCookie);

		// then
		assertNotNull(tokenInfo);
		assertEquals(newAccessToken, tokenInfo.accessToken());
		assertEquals(newRefreshToken, tokenInfo.refreshToken());
	}

	@Test
	@DisplayName("리프레시 토큰 재발급 시 쿠키가 null인 경우 예외가 발생한다.")
	void reissue_ShouldThrowException_whenNotPresentCookie() {
		// given
		Cookie refreshTokenCookie = null;

		// when & then
		AuthException authException =
			assertThrows(AuthException.class, () -> authService.reissue(refreshTokenCookie));
		assertEquals(NOT_FOUND_REFRESHTOKEN_IN_COOKIE, authException.getExceptionStatus());
	}

	@Test
	@DisplayName("리프레시 토큰 재발급 시 쿠키에 리프레시 토큰이 존재하지 않을 경우 예외가 발생한다.")
	void reissue_ShouldThrowException_whenNotPresentRefreshtoken() {
		// given
		Cookie refreshTokenCookie = new Cookie("refreshToken", "");

		// when & then
		AuthException authException =
			assertThrows(AuthException.class, () -> authService.reissue(refreshTokenCookie));
		assertEquals(NOT_FOUND_REFRESHTOKEN_IN_COOKIE, authException.getExceptionStatus());
	}

	@Test
	@DisplayName("리프레시 토큰 재발급 시 기존 리프레시 토큰이 만료되었을 경우 예외가 발생한다.")
	void reissue_ShouldThrowException_whenExpiredRefreshtoken() {
		// given
		Cookie cookie = new Cookie("refreshToken", EXPIRED_TOKEN);

		when(jwtUtil.isExpired(EXPIRED_TOKEN)).thenReturn(true);

		// when & then
		AuthException authException =
			assertThrows(AuthException.class, () -> authService.reissue(cookie));
		assertEquals(EXPIRED_REFRESHTOKEN, authException.getExceptionStatus());
	}

	@Test
	@DisplayName("로그아웃 시 쿠키가 null인 경우 예외가 발생한다.")
	void logout_ShouldThrowException_WhenNotPresentCookie() {
		// given
		Cookie refreshTokenCookie = null;

		// when & then
		AuthException authException =
			assertThrows(AuthException.class, () -> authService.logout(refreshTokenCookie));
		assertEquals(NOT_FOUND_REFRESHTOKEN_IN_COOKIE, authException.getExceptionStatus());
	}

	@Test
	@DisplayName("로그아웃 시 쿠키에 리프레시 토큰이 존재하지 않을 경우 예외가 발생한다.")
	void logout_ShouldThrowException_WhenNotPresentRefreshtoken() {
		// given
		Cookie refreshTokenCookie = new Cookie("refreshToken", "");

		// when & then
		AuthException authException =
			assertThrows(AuthException.class, () -> authService.logout(refreshTokenCookie));
		assertEquals(NOT_FOUND_REFRESHTOKEN_IN_COOKIE, authException.getExceptionStatus());
	}

	@Test
	@DisplayName("회원탈퇴 시 비밀번호가 틀리면 예외가 발생한다.")
	void deleteUser_ShouldThrowException_WhenInvalidPassword() {
		// given
		Long userId = 1L;
		User user = User.builder()
			.password(ENCODED_PASSWORD)
			.build();
		ReflectionTestUtils.setField(user, "id", userId);
		DeleteUserRequest request = new DeleteUserRequest("incorrectPassword");
		Cookie refreshTokenCookie = new Cookie("refreshToken", REFRESH_TOKEN);

		when(userRepository.findByIdAndStatus(userId, UserStatus.ACTIVE)).thenReturn(Optional.of(user));
		when(passwordEncoder.matches(request.password(), user.getPassword())).thenReturn(false);

		// when & then
		AuthException authException
			= assertThrows(AuthException.class, () -> authService.deleteUser(userId, request, refreshTokenCookie));
		assertEquals(INVALID_PASSWORD, authException.getExceptionStatus());
	}

	@Test
	@DisplayName("회원탈퇴 시 쿠키가 null인 경우 예외가 발생한다.")
	void deleteUser_ShouldThrowException_WhenNotPresentCookie() {
		// given
		Long userId = 1L;
		User user = User.builder()
			.password(ENCODED_PASSWORD)
			.build();
		ReflectionTestUtils.setField(user, "id", userId);
		DeleteUserRequest request = new DeleteUserRequest("password123");
		Cookie refreshTokenCookie = null;

		when(userRepository.findByIdAndStatus(userId, UserStatus.ACTIVE)).thenReturn(Optional.of(user));
		when(passwordEncoder.matches(request.password(), user.getPassword())).thenReturn(true);

		// when & then
		AuthException authException =
			assertThrows(AuthException.class, () -> authService.deleteUser(userId, request, refreshTokenCookie));
		assertEquals(NOT_FOUND_REFRESHTOKEN_IN_COOKIE, authException.getExceptionStatus());
	}

	@Test
	@DisplayName("회원탈퇴 시 쿠키에 리프레시 토큰이 존재하지 않을 경우 예외가 발생한다.")
	void deleteUser_ShouldThrowException_WhenNotPresentRefreshtoken() {
		// given
		Long userId = 1L;
		User user = User.builder()
			.password(ENCODED_PASSWORD)
			.build();
		ReflectionTestUtils.setField(user, "id", userId);
		DeleteUserRequest request = new DeleteUserRequest("password123");
		Cookie refreshTokenCookie = new Cookie("refreshToken", "");

		when(userRepository.findByIdAndStatus(userId, UserStatus.ACTIVE)).thenReturn(Optional.of(user));
		when(passwordEncoder.matches(request.password(), user.getPassword())).thenReturn(true);

		// when & then
		AuthException authException =
			assertThrows(AuthException.class, () -> authService.deleteUser(userId, request, refreshTokenCookie));
		assertEquals(NOT_FOUND_REFRESHTOKEN_IN_COOKIE, authException.getExceptionStatus());
	}

}