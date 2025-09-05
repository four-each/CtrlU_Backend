package org.example.ctrlu.domain.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SigninRequest (
	@NotBlank
	@Email(message = "유효한 이메일 형식이어야 합니다.")
	String email,
	@Pattern(
		regexp = "^(?=.*[a-zA-Z])(?=.*\\p{IsHangul})(?=.*[!@#$%^&*])[A-Za-z\\p{IsHangul}0-9!@#$%^&*]{8,12}$",
		message = "비밀번호는 8~12자여야 하며, 영문, 한글, 특수문자를 각각 하나 이상 포함해야 합니다."
	)
	String password
) {
}
