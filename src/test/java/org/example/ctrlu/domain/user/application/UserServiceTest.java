package org.example.ctrlu.domain.user.application;

import static org.assertj.core.api.Assertions.*;
import static org.example.ctrlu.domain.user.exception.UserErrorCode.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.example.ctrlu.domain.user.dto.request.UpdatePasswordRequest;
import org.example.ctrlu.domain.user.dto.request.UpdateProfileRequest;
import org.example.ctrlu.domain.user.dto.response.CursorResult;
import org.example.ctrlu.domain.user.dto.response.GetProfileResponse;
import org.example.ctrlu.domain.user.dto.response.SearchUsersResponse;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
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
			.profileImageKey("oldImageUrl")
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
		UpdateProfileRequest request = new UpdateProfileRequest("newNickname", "profile/123.jpg");

		when(userRepository.findById(userId)).thenReturn(Optional.of(user));

		// when
		userService.updateProfile(userId, request);

		// then
		assertThat("newNickname").isEqualTo(user.getNickname());
		assertThat("profile/123.jpg").isEqualTo(user.getProfileImageKey());
	}

	@Test
	@DisplayName("사용자 검색에 성공한다.")
	void searchUsersByEmail_success_withValidKeyword() {
		// Given
		String keyword = "TestUser@Example.com ";
		String expectedSearchKeyword = "testuser@example.com";
		Long cursorId = 1L;
		int size = 10;
		PageRequest pageable = PageRequest.of(0, size);
		List<User> userList = List.of(user);
		Slice<User> userSlice = new SliceImpl<>(userList, pageable, true);

		when(userRepository.findByEmailWithCursor(expectedSearchKeyword, cursorId, pageable)).thenReturn(userSlice);
		when(awsS3Service.generateGetPresignedUrl(user.getProfileImageKey())).thenReturn("profile/123.jpg");

		// When
		CursorResult<SearchUsersResponse> response = userService.searchUsersByEmail(keyword, cursorId, size);

		// Then
		assertThat(response.values().get(0).id()).isEqualTo(user.getId());
		assertThat(response.values().get(0).email()).isEqualTo(user.getEmail());
		assertThat(response.values().get(0).nickname()).isEqualTo(user.getNickname());
		assertThat(response.values().get(0).image()).isEqualTo(awsS3Service.generateGetPresignedUrl(user.getProfileImageKey()));
	}

	@Test
	@DisplayName("사용자 검색 결과가 없는 경우 어떤 사용자도 반환하지 않는다.")
	void searchUsersByEmail_throwsUserException_whenUserNotFound() {
		// Given
		String keyword = "nonexistent@example.com";
		String expectedSearchKeyword = "nonexistent@example.com";
		Long cursorId = 1L;
		int size = 10;
		PageRequest pageable = PageRequest.of(0, size);
		Slice<User> emptySlice = new SliceImpl<>(Collections.emptyList(), pageable, true);

		when(userRepository.findByEmailWithCursor(expectedSearchKeyword, cursorId, pageable))
			.thenReturn(emptySlice);

		// When
		CursorResult<SearchUsersResponse> response = userService.searchUsersByEmail(keyword, cursorId, size);

		// Then
		assertThat(response.values().size()).isEqualTo(0);
	}

	@Test
	@DisplayName("프로필 조회 시 닉네임과 프로필 이미지 조회에 성공한다.")
	void getProfile_ShouldSucceed() {
		// Given
		Long userId = 1L;
		when(userRepository.findById(userId)).thenReturn(Optional.of(user));
		when(awsS3Service.generateGetPresignedUrl(user.getProfileImageKey())).thenReturn("profile/123.jpg");

		// When
		GetProfileResponse response = userService.getProfile(userId);

		// Then
		assertThat(response.nickname()).isEqualTo(user.getNickname());
		assertThat(response.profileImage()).isEqualTo(awsS3Service.generateGetPresignedUrl(user.getProfileImageKey()));
	}

	@Test
	@DisplayName("존재하지 않는 사용자 ID로 프로필 조회 시 예외가 발생한다.")
	void getProfile_throwsUserException_withNotFoundUserId() {
		// Given
		Long nonExistentUserId = 99L;
		when(userRepository.findById(nonExistentUserId)).thenReturn(Optional.empty());

		// When & Then
		UserException userException = assertThrows(UserException.class,
			() -> userService.getProfile(nonExistentUserId));
		assertThat(NOT_FOUND_USER).isEqualTo(userException.getExceptionStatus());
	}
}