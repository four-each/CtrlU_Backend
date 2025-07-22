package org.example.ctrlu.domain.friendship.controller;

import org.example.ctrlu.domain.friendship.application.FriendshipService;
import org.example.ctrlu.domain.friendship.dto.request.FriendshipRequest;
import org.example.ctrlu.domain.friendship.dto.response.GetFriendshipListResponse;
import org.example.ctrlu.global.response.BaseResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

	@DeleteMapping("/{friendshipId}")
	public BaseResponse<Void> deleteFriendship(
		@AuthenticationPrincipal Long userId,
		@PathVariable Long friendshipId
	) {
		friendshipService.deleteFriendship(userId, friendshipId);
		return new BaseResponse<>(null);
	}

	@PatchMapping("/{friendshipId}")
	public BaseResponse<Void> acceptFriendship(
		@AuthenticationPrincipal Long userId,
		@PathVariable Long friendshipId
	) {
		friendshipService.acceptFriendship(userId, friendshipId);
		return new BaseResponse<>(null);
	}

	@PatchMapping("/{friendshipId}/reject")
	public BaseResponse<Void> rejectFriendship(
		@AuthenticationPrincipal Long userId,
		@PathVariable Long friendshipId
	) {
		friendshipService.rejectFriendship(userId, friendshipId);
		return new BaseResponse<>(null);
	}

	@DeleteMapping("/{friendshipId}/cancel")
	public BaseResponse<Void> cancelFriendship(
		@AuthenticationPrincipal Long userId,
		@PathVariable Long friendshipId
	) {
		friendshipService.cancelFriendship(userId, friendshipId);
		return new BaseResponse<>(null);
	}

	@GetMapping
	public BaseResponse<GetFriendshipListResponse> getFriends(
		@AuthenticationPrincipal Long userId
	) {
		GetFriendshipListResponse response = friendshipService.getFriends(userId);
		return new BaseResponse<>(response);
	}

	@GetMapping("/received")
	public BaseResponse<GetFriendshipListResponse> getReceivedRequests(
		@AuthenticationPrincipal Long userId
	) {
		GetFriendshipListResponse response = friendshipService.getReceivedRequests(userId);
		return new BaseResponse<>(response);
	}

	@GetMapping("/sent")
	public BaseResponse<GetFriendshipListResponse> getSentRequests(
		@AuthenticationPrincipal Long userId
	) {
		GetFriendshipListResponse response = friendshipService.getSentRequests(userId);
		return new BaseResponse<>(response);
	}
}
