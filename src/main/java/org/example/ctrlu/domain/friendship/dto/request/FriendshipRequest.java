package org.example.ctrlu.domain.friendship.dto.request;

import jakarta.validation.constraints.NotNull;

public record FriendshipRequest(
	@NotNull
	Long targetId
) {
}
