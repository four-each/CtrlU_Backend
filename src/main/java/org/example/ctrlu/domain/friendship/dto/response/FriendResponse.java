package org.example.ctrlu.domain.friendship.dto.response;

public record FriendResponse(
	Long id,
	String nickname,
	String email,
	String image
) {
}
