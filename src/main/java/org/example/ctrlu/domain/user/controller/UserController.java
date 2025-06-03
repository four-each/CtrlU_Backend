package org.example.ctrlu.domain.user.controller;

import org.example.ctrlu.domain.user.application.UserService;
import org.example.ctrlu.domain.user.dto.request.DeleteUserRequest;
import org.example.ctrlu.domain.user.dto.request.UpdatePasswordRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {
	private final UserService userService;

	@PatchMapping("/password")
	public void updatePassword(
		@AuthenticationPrincipal Long userId,
		@Valid @RequestBody UpdatePasswordRequest request
	) {
		userService.updatePassword(userId, request);
	}

	@DeleteMapping
	public void deleteUser(
		@AuthenticationPrincipal Long userId,
		@Valid @RequestBody DeleteUserRequest request
	) {
		userService.deleteUser(userId, request);
	}
}
