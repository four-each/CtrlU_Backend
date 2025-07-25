package org.example.ctrlu.domain.auth.dto.request;

public record ResetPasswordRequest(
	String verifyToken,
	String password
) {
}
