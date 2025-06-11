package org.example.ctrlu.domain.user.dto.request;

import jakarta.validation.constraints.Pattern;

public record UpdatePasswordRequest(
	@Pattern(
		regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,12}$",
		message = "비밀번호는 영문자와 숫자를 포함해 8~12자여야 합니다."
	)
	String currentPassword,
	@Pattern(
		regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,12}$",
		message = "비밀번호는 영문자와 숫자를 포함해 8~12자여야 합니다."
	)
	String newPassword
) {
}
