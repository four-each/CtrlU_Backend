package org.example.ctrlu.domain.user.controller;

import org.example.ctrlu.domain.user.application.UserService;
import org.example.ctrlu.domain.user.dto.request.UpdatePasswordRequest;
import org.example.ctrlu.domain.user.dto.request.UpdateProfileRequest;
import org.example.ctrlu.global.response.BaseResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {
	private final UserService userService;

	@PatchMapping("/password")
	public BaseResponse<Void> updatePassword(
		@AuthenticationPrincipal Long userId,
		@Valid @RequestBody UpdatePasswordRequest request
	) {
		userService.updatePassword(userId, request);
		return new BaseResponse<>(null);
	}

	@PatchMapping("/profile")
	public BaseResponse<Void> updateProfile(
		@AuthenticationPrincipal Long userId,
		@RequestPart("request") @Valid UpdateProfileRequest request,
		@RequestPart("userImage") MultipartFile userImage
	) {
		userService.updateProfile(userId, request, userImage);
		return new BaseResponse<>(null);
	}

}
