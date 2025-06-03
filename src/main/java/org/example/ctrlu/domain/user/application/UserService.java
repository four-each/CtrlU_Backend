package org.example.ctrlu.domain.user.application;

import static org.example.ctrlu.domain.user.exception.UserErrorCode.*;

import org.example.ctrlu.domain.user.dto.request.DeleteUserRequest;
import org.example.ctrlu.domain.user.dto.request.UpdatePasswordRequest;
import org.example.ctrlu.domain.user.entity.User;
import org.example.ctrlu.domain.user.entity.UserStatus;
import org.example.ctrlu.domain.user.exception.UserException;
import org.example.ctrlu.domain.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {
	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	public void updatePassword(Long userId, UpdatePasswordRequest request) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new UserException(NOT_FOUND_USER));

		if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
			throw new UserException(INVALID_PASSWORD);
		}

		String encodedPassword = passwordEncoder.encode(request.newPassword());
		user.updatePassword(encodedPassword);
	}

	public void deleteUser(Long userId, DeleteUserRequest request) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new UserException(NOT_FOUND_USER));

		if (!passwordEncoder.matches(request.password(), user.getPassword())) {
			throw new UserException(INVALID_PASSWORD);
		}

		user.updateStatus(UserStatus.INACTIVE);
	}
}
