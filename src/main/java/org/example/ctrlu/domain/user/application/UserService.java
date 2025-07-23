package org.example.ctrlu.domain.user.application;

import static org.example.ctrlu.domain.user.exception.UserErrorCode.*;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.example.ctrlu.domain.user.dto.request.UpdatePasswordRequest;
import org.example.ctrlu.domain.user.dto.request.UpdateProfileRequest;
import org.example.ctrlu.domain.user.dto.response.CursorResult;
import org.example.ctrlu.domain.user.dto.response.SearchUsersResponse;
import org.example.ctrlu.domain.user.entity.User;
import org.example.ctrlu.domain.user.exception.UserException;
import org.example.ctrlu.domain.user.repository.UserRepository;
import org.example.ctrlu.global.s3.AwsS3Service;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {
	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final AwsS3Service awsS3Service;

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
	public void updateProfile(Long userId, UpdateProfileRequest request, MultipartFile userImage) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new UserException(NOT_FOUND_USER));

		awsS3Service.deleteImage(user.getImage());
		String imageUrl = awsS3Service.uploadImage(userImage);
		user.updateProfile(request.nickname(), imageUrl);
	}

	public CursorResult<SearchUsersResponse> searchUsersByEmail(String keyword, Long cursorId, int size) {
		if (!StringUtils.hasText(keyword) || keyword.trim().length() < 2) {
			return new CursorResult<>(Collections.emptyList(), false, null);
		}

		PageRequest pageable = PageRequest.of(0, size);
		String lowerCaseKeyword = keyword.trim().toLowerCase();

		Slice<User> usersSlice;
		if (cursorId == null) {
			usersSlice = userRepository.findByEmailStartsWithFirstPage(lowerCaseKeyword, pageable);
		} else {
			usersSlice = userRepository.findByEmailStartsWithNextPage(lowerCaseKeyword, cursorId, pageable);
		}

		return CursorResult.of(usersSlice, SearchUsersResponse::from, SearchUsersResponse::id);
	}
}
