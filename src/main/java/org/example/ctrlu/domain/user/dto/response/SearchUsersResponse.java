package org.example.ctrlu.domain.user.dto.response;

import org.example.ctrlu.domain.user.entity.User;

public record SearchUsersResponse(
	Long id,
	String nickname,
	String email,
	String image
) {
	public static SearchUsersResponse from(User user) {
		return new SearchUsersResponse(user.getId(), user.getNickname(), user.getEmail(), user.getProfileImageKey());
	}
}
