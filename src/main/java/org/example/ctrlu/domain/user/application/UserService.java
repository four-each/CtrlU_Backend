package org.example.ctrlu.domain.user.application;

import static org.example.ctrlu.domain.user.exception.UserErrorCode.*;

import java.util.Collections;

import org.example.ctrlu.domain.user.dto.request.UpdatePasswordRequest;
import org.example.ctrlu.domain.user.dto.request.UpdateProfileRequest;
import org.example.ctrlu.domain.user.dto.response.CursorResult;
import org.example.ctrlu.domain.user.dto.response.GetProfileResponse;
import org.example.ctrlu.domain.user.dto.response.SearchUsersResponse;
import org.example.ctrlu.domain.user.entity.User;
import org.example.ctrlu.domain.user.exception.UserException;
import org.example.ctrlu.domain.user.repository.UserRepository;
import org.example.ctrlu.global.s3.AwsS3Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {
	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final AwsS3Service awsS3Service;

	@Value("${cloud.aws.s3.default-profile-image}")
	private String defaultImageKey;

	@Transactional
	public void updatePassword(Long userId, UpdatePasswordRequest request) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new UserException(NOT_FOUND_USER));

		if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
			throw new UserException(INVALID_PASSWORD);
		}

		user.updatePassword(passwordEncoder.encode(request.newPassword()));
	}

	@Transactional
	public void updateProfile(Long userId, UpdateProfileRequest request) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new UserException(NOT_FOUND_USER));

		String profileImageKey = request.profileImageKey();
		if (profileImageKey.equals(user.getProfileImageKey())) {
			user.updateProfile(request.nickname(), profileImageKey);
			return;
		}

		if (profileImageKey.isBlank()) {
			profileImageKey = defaultImageKey;
		}

		if (!user.getProfileImageKey().equals(defaultImageKey)) {
			awsS3Service.deleteImage(user.getProfileImageKey());
		}

		user.updateProfile(request.nickname(), profileImageKey);
	}

	@Transactional(readOnly = true)
	public CursorResult<SearchUsersResponse> searchUsersByEmail(String keyword, Long cursorId, int size) {
		if (!StringUtils.hasText(keyword) || keyword.trim().length() < 2) {
			return new CursorResult<>(Collections.emptyList(), false, null);
		}

		PageRequest pageable = PageRequest.of(0, size);
		String lowerCaseKeyword = keyword.trim().toLowerCase();
		Slice<User> usersSlice = userRepository.findByEmailWithCursor(lowerCaseKeyword, cursorId, pageable);;

		return CursorResult.of(
			usersSlice,
			user -> SearchUsersResponse.from(user, awsS3Service),
			SearchUsersResponse::id
		);
	}

	@Transactional(readOnly = true)
	public GetProfileResponse getProfile(Long userId) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new UserException(NOT_FOUND_USER));

		return GetProfileResponse.from(user, awsS3Service);
	}
}
