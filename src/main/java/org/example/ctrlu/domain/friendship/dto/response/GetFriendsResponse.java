package org.example.ctrlu.domain.friendship.dto.response;

import java.util.List;

public record GetFriendsResponse(
	List<Friend> friends
) {
	public static GetFriendsResponse from(List<Friend> friends) {
		return new GetFriendsResponse(friends);
	}

	public record Friend(
		Long id,
		String nickname,
		String email,
		String image
	) {}
}
