package org.example.ctrlu.domain.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SignupRequest (
	@NotBlank
	@Email(message = "유효한 이메일 형식이어야 합니다.")
	String email,
	@Pattern(
		regexp = "^(?=.*[a-zA-Z])(?=.*\\d)(?=.*[!@#$%^&*])[A-Za-z\\d!@#$%^&*]{8,12}$",
		message = "비밀번호는 8~12자여야 하며, 영문, 숫자, 특수문자를 각각 하나 이상 포함해야 합니다."
	)
	String password,
	@NotBlank
	String nickname,
	String profileImageKey
) {
}
