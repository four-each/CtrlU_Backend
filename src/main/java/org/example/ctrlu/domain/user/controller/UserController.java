package org.example.ctrlu.domain.user.controller;

import org.example.ctrlu.domain.user.application.UserService;
import org.example.ctrlu.domain.user.dto.request.UpdatePasswordRequest;
import org.example.ctrlu.domain.user.dto.request.UpdateProfileRequest;
import org.example.ctrlu.domain.user.dto.response.CursorResult;
import org.example.ctrlu.domain.user.dto.response.GetProfileResponse;
import org.example.ctrlu.domain.user.dto.response.SearchUsersResponse;
import org.example.ctrlu.global.response.BaseResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
		@RequestBody @Valid UpdateProfileRequest request
	) {
		userService.updateProfile(userId, request);
		return new BaseResponse<>(null);
	}

	@GetMapping("/search")
	public BaseResponse<CursorResult<SearchUsersResponse>> searchUsersByEmail(
		@RequestParam("keyword") String keyword,
		@RequestParam(required = false) Long cursorId,
		@RequestParam(defaultValue = "10") int size
	) {
		return new BaseResponse<>(userService.searchUsersByEmail(keyword, cursorId, size));
	}

	@GetMapping("profile")
	public BaseResponse<GetProfileResponse> getProfile(
		@AuthenticationPrincipal Long userId
	) {
		return new BaseResponse<>(userService.getProfile(userId));
	}
}
