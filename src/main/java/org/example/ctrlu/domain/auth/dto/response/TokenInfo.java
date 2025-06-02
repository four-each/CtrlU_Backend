package org.example.ctrlu.domain.auth.dto.response;

public record TokenInfo(
	String accessToken,
	String refreshToken
) {
}
