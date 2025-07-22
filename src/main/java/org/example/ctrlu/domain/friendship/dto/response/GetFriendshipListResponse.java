package org.example.ctrlu.domain.friendship.dto.response;

import java.util.List;

public record GetFriendshipListResponse(
	List<FriendResponse> friends
) {
	public static GetFriendshipListResponse from(List<FriendResponse> friends) {
		return new GetFriendshipListResponse(friends);
	}
}
