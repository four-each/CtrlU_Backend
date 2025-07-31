package org.example.ctrlu.domain.auth.application;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.util.Optional;

import org.example.ctrlu.domain.auth.dto.request.FindPasswordRequest;
import org.example.ctrlu.domain.auth.dto.request.ResetPasswordRequest;
import org.example.ctrlu.domain.auth.exception.AuthErrorCode;
import org.example.ctrlu.domain.auth.exception.AuthException;
import org.example.ctrlu.domain.auth.util.JWTUtil;
import org.example.ctrlu.domain.user.entity.User;
import org.example.ctrlu.domain.user.entity.UserStatus;
import org.example.ctrlu.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
public class FindPasswordServiceTest {
	@InjectMocks
	private AuthService authService;
	@Mock private UserRepository userRepository;
	@Mock private JWTUtil jwtUtil;
	@Mock private MailService mailService;
	@Mock private PasswordEncoder passwordEncoder;

	private User activeUser;
	private User inactiveUser;
	private String validVerifyToken;
	private String expiredVerifyToken;

	@BeforeEach
	void setUp() {
		activeUser = User.builder()
			.email("test@example.com")
			.password("oldpassword")
			.verifyToken("oldVerifyToken")
			.build();
		activeUser.changeUserStatusToActive();

		inactiveUser = User.builder()
			.email("inactive@example.com")
			.password("password")
			.build();
		inactiveUser.changeUserStatusToActive();

		validVerifyToken = "validTestToken";
		expiredVerifyToken = "expiredTestToken";
	}

	@Test
	@DisplayName("비밀번호 찾기 - 유효한 이메일로 요청 시 성공적으로 처리한다.")
	void findPassword_success_withValidEmail() {
		// Given
		FindPasswordRequest request = new FindPasswordRequest("test@example.com");
		given(userRepository.findByEmailAndStatus(request.email(), UserStatus.ACTIVE))
			.willReturn(Optional.of(activeUser));
		given(jwtUtil.createVerifyToken(anyLong())).willReturn(validVerifyToken);

		// When
		authService.findPassword(request);

		// Then
		assertThat(activeUser.getVerifyToken()).isEqualTo(validVerifyToken);
		verify(mailService, times(1)).sendFindPasswordEmail(activeUser);
		verify(userRepository, times(1)).findByEmailAndStatus(request.email(), UserStatus.ACTIVE);
	}

	@Test
	@DisplayName("비밀번호 찾기 - 존재하지 않는 이메일로 요청 시 AuthException을 발생시킨다.")
	void findPassword_throwsAuthException_whenUserNotFound() {
		// Given
		FindPasswordRequest request = new FindPasswordRequest("nonexistent@example.com");
		when(userRepository.findByEmailAndStatus(request.email(), UserStatus.ACTIVE))
			.thenReturn(Optional.empty());

		// When & Then
		AuthException authException = assertThrows(AuthException.class,
			() -> authService.findPassword(request));
		assertThat(AuthErrorCode.NOT_FOUND_USER).isEqualTo(authException.getExceptionStatus());
		verify(jwtUtil, never()).createVerifyToken(anyLong());
		verify(mailService, never()).sendFindPasswordEmail(any(User.class));
	}

	@Test
	@DisplayName("비밀번호 찾기 - 비활성 상태의 이메일로 요청 시 AuthException을 발생시킨다.")
	void findPassword_throwsAuthException_whenUserInactive() {
		// Given
		FindPasswordRequest request = new FindPasswordRequest("inactive@example.com");
		when(userRepository.findByEmailAndStatus(request.email(), UserStatus.ACTIVE))
			.thenReturn(Optional.empty());

		// When & Then
		AuthException authException = assertThrows(AuthException.class,
			() -> authService.findPassword(request));
		assertThat(AuthErrorCode.NOT_FOUND_USER).isEqualTo(authException.getExceptionStatus());
		verify(jwtUtil, never()).createVerifyToken(anyLong());
		verify(mailService, never()).sendFindPasswordEmail(any(User.class));
	}

	@Test
	@DisplayName("비밀번호 재설정 토큰 검증 - 유효한 토큰일 경우 true를 반환한다.")
	void verifyResetToken_returnsTrue_withValidToken() {
		// Given
		when(jwtUtil.isExpired(validVerifyToken)).thenReturn(false);
		when(userRepository.findByVerifyTokenAndStatus(validVerifyToken, UserStatus.ACTIVE))
			.thenReturn(Optional.of(activeUser));

		// When
		boolean result = authService.verifyResetToken(validVerifyToken);

		// Then
		assertThat(result).isTrue();
		verify(jwtUtil, times(1)).isExpired(validVerifyToken);
		verify(userRepository, times(1)).findByVerifyTokenAndStatus(validVerifyToken, UserStatus.ACTIVE);
	}

	@Test
	@DisplayName("비밀번호 재설정 토큰 검증 - 만료된 토큰일 경우 false를 반환한다.")
	void verifyResetToken_returnsFalse_withExpiredToken() {
		// Given
		when(jwtUtil.isExpired(expiredVerifyToken)).thenReturn(true);

		// When
		boolean result = authService.verifyResetToken(expiredVerifyToken);

		// Then
		assertThat(result).isFalse();
		verify(jwtUtil, times(1)).isExpired(expiredVerifyToken);
		verify(userRepository, never()).findByVerifyTokenAndStatus(anyString(), any(UserStatus.class));
	}

	@Test
	@DisplayName("비밀번호 재설정 토큰 검증 - 토큰은 유효하지만 해당하는 사용자가 없는 경우 AuthException을 발생시킨다.")
	void verifyResetToken_throwsAuthException_whenUserNotFound() {
		// Given
		when(jwtUtil.isExpired(validVerifyToken)).thenReturn(false);
		when(userRepository.findByVerifyTokenAndStatus(validVerifyToken, UserStatus.ACTIVE))
			.thenReturn(Optional.empty());

		// When & Then
		AuthException authException = assertThrows(AuthException.class,
			() -> authService.verifyResetToken(validVerifyToken));
		assertThat(AuthErrorCode.NOT_FOUND_USER).isEqualTo(authException.getExceptionStatus());

		verify(jwtUtil, times(1)).isExpired(validVerifyToken);
		verify(userRepository, times(1)).findByVerifyTokenAndStatus(validVerifyToken, UserStatus.ACTIVE);
	}

	@Test
	@DisplayName("비밀번호 재설정 - 유효한 토큰과 새 비밀번호로 성공적으로 재설정한다.")
	void resetPassword_success_withValidTokenAndNewPassword() {
		// Given
		ResetPasswordRequest request = new ResetPasswordRequest(validVerifyToken, "newpassword123!");
		when(jwtUtil.isExpired(validVerifyToken)).thenReturn(false);
		when(userRepository.findByVerifyTokenAndStatus(validVerifyToken, UserStatus.ACTIVE))
			.thenReturn(Optional.of(activeUser));
		when(passwordEncoder.encode(request.password())).thenReturn("encodedNewPassword");

		// When
		authService.resetPassword(request);

		// Then
		assertThat(activeUser.getPassword()).isEqualTo("encodedNewPassword");
		assertThat(activeUser.getVerifyToken()).isEmpty();
		verify(jwtUtil, times(1)).isExpired(validVerifyToken);
		verify(userRepository, times(1)).findByVerifyTokenAndStatus(validVerifyToken, UserStatus.ACTIVE);
		verify(passwordEncoder, times(1)).encode(request.password());
	}

	@Test
	@DisplayName("비밀번호 재설정 - 만료된 토큰으로 요청 시 AuthException을 발생시킨다.")
	void resetPassword_throwsAuthException_whenTokenExpired() {
		// Given
		ResetPasswordRequest request = new ResetPasswordRequest(expiredVerifyToken, "newpassword123!");
		when(jwtUtil.isExpired(expiredVerifyToken)).thenReturn(true);

		// When & Then
		AuthException authException = assertThrows(AuthException.class,
			() -> authService.resetPassword(request));
		assertThat(AuthErrorCode.EXPIRED_VERIFYTOKEN).isEqualTo(authException.getExceptionStatus());
		verify(jwtUtil, times(1)).isExpired(expiredVerifyToken);
		verify(userRepository, never()).findByVerifyTokenAndStatus(anyString(), any(UserStatus.class));
		verify(passwordEncoder, never()).encode(anyString());
	}

	@Test
	@DisplayName("비밀번호 재설정 - 토큰은 유효하지만 해당하는 사용자가 없는 경우 AuthException을 발생시킨다.")
	void resetPassword_throwsAuthException_whenUserNotFound() {
		// Given
		ResetPasswordRequest request = new ResetPasswordRequest(validVerifyToken, "newpassword123!");
		when(jwtUtil.isExpired(validVerifyToken)).thenReturn(false);
		when(userRepository.findByVerifyTokenAndStatus(validVerifyToken, UserStatus.ACTIVE))
			.thenReturn(Optional.empty());

		// When & Then
		AuthException authException = assertThrows(AuthException.class,
			() -> authService.resetPassword(request));
		assertThat(AuthErrorCode.NOT_FOUND_USER).isEqualTo(authException.getExceptionStatus());
		verify(jwtUtil, times(1)).isExpired(validVerifyToken);
		verify(userRepository, times(1)).findByVerifyTokenAndStatus(validVerifyToken, UserStatus.ACTIVE);
		verify(passwordEncoder, never()).encode(anyString());
	}
}
