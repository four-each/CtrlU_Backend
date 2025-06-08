package org.example.ctrlu.domain.user.application;

import static org.example.ctrlu.domain.user.exception.UserErrorCode.*;

import org.example.ctrlu.domain.user.dto.request.UpdatePasswordRequest;
import org.example.ctrlu.domain.user.dto.request.UpdateProfileRequest;
import org.example.ctrlu.domain.user.entity.User;
import org.example.ctrlu.domain.user.exception.UserException;
import org.example.ctrlu.domain.user.repository.UserRepository;
import org.example.ctrlu.global.s3.AwsS3Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
}
