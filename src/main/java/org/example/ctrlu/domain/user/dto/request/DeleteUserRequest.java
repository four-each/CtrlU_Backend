package org.example.ctrlu.domain.user.dto.request;

import jakarta.validation.constraints.NotBlank;

public record DeleteUserRequest(
	@NotBlank
	String password
) {
}
