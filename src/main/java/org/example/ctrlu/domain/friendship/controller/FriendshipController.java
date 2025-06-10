package org.example.ctrlu.domain.friendship.controller;

import org.example.ctrlu.domain.friendship.application.FriendshipService;
import org.example.ctrlu.domain.friendship.dto.request.FriendshipRequest;
import org.example.ctrlu.global.response.BaseResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;

@RestController
@AllArgsConstructor
@RequestMapping("/friendships")
public class FriendshipController {
	private final FriendshipService friendshipService;

	@PostMapping
	public BaseResponse<Void> requestFriendship(
		@AuthenticationPrincipal Long userId,
		@Valid @RequestBody FriendshipRequest request
	) {
		friendshipService.requestFriendship(userId, request);
		return new BaseResponse<>(null);
	}
}
