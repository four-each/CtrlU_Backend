package org.example.ctrlu.domain.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SigninRequest (
	@NotBlank
	@Email(message = "유효한 이메일 형식이어야 합니다.")
	String email,
	@Pattern(
		regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,12}$",
		message = "비밀번호는 영문자와 숫자를 포함해 8~12자여야 합니다."
	)
	String password
) {
}
