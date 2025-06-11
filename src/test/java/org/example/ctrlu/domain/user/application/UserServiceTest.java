package org.example.ctrlu.domain.user.application;

import static org.assertj.core.api.Assertions.*;
import static org.example.ctrlu.domain.user.exception.UserErrorCode.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.example.ctrlu.domain.user.dto.request.UpdatePasswordRequest;
import org.example.ctrlu.domain.user.dto.request.UpdateProfileRequest;
import org.example.ctrlu.domain.user.entity.User;
import org.example.ctrlu.domain.user.exception.UserException;
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
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
	@InjectMocks
	private UserService userService;
	@Mock
	private UserRepository userRepository;
	@Mock
	private AwsS3Service awsS3Service;
	@Mock
	private MultipartFile file;
	@Mock
	private PasswordEncoder passwordEncoder;

	private Long userId;
	private User user;

	@BeforeEach
	void setUp() {
		userId = 1L;
		user =  User.builder()
			.email("test@test.com")
			.password("encodedOldPassword")
			.nickname("nickname")
			.image("oldImageUrl")
			.verifyToken("verifytoken")
			.build();
		user.changeUserStatusToActive();
	}

	@Test
	@DisplayName("비밀번호 변경에 성공한다.")
	void updatePassword_ShouldSucceed() {
		// given
		UpdatePasswordRequest request = new UpdatePasswordRequest("oldPassword", "newPassword");

		when(userRepository.findById(userId)).thenReturn(Optional.of(user));
		when(passwordEncoder.matches("oldPassword", "encodedOldPassword")).thenReturn(true);
		when(passwordEncoder.encode("newPassword")).thenReturn("encodedNewPassword");

		// when
		userService.updatePassword(userId, request);

		// then
		verify(passwordEncoder, times(1)).matches("oldPassword", "encodedOldPassword");
		verify(passwordEncoder, times(1)).encode("newPassword");
		assertThat("encodedNewPassword").isEqualTo(user.getPassword());
	}

	@Test
	@DisplayName("비밀번호 변경 시 현재 비밀번호가 틀리면 예외가 발생한다.")
	void updatePassword_ShouldThrowException_WhenPasswordIsInvalid() {
		// given
		UpdatePasswordRequest request = new UpdatePasswordRequest("oldPassword", "newPassword");

		when(userRepository.findById(userId)).thenReturn(Optional.of(user));
		when(passwordEncoder.matches("oldPassword", "encodedOldPassword")).thenReturn(false);

		// when & then
		UserException userException = assertThrows(UserException.class,
			() -> userService.updatePassword(userId, request));
		assertThat(INVALID_PASSWORD).isEqualTo(userException.getExceptionStatus());
	}

	@Test
	@DisplayName("프로필 변경에 성공한다.")
	void updateProfile_ShouldSucceed() {
		// given
		UpdateProfileRequest request = new UpdateProfileRequest("newNickname");

		when(userRepository.findById(userId)).thenReturn(Optional.of(user));
		when(awsS3Service.uploadImage(file)).thenReturn("newImageUrl");

		// when
		userService.updateProfile(userId, request, file);

		// then
		assertThat("newNickname").isEqualTo(user.getNickname());
		assertThat("newImageUrl").isEqualTo(user.getImage());
	}

}