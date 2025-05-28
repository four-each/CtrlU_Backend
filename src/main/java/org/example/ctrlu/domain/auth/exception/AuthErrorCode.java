package org.example.ctrlu.domain.auth.exception;

import org.example.ctrlu.global.response.ErrorCode;
import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorCode {
	ALREADY_EXIST_EMAIL(HttpStatus.BAD_REQUEST.value(), "A001", "이미 존재하는 이메일입니다."),
	FAILED_SEND_EMAIL(HttpStatus.INTERNAL_SERVER_ERROR.value(), "A002", "인증 메일 전송에 실패하였습니다."),
	TRY_EMAIL_VERIFICATION(HttpStatus.BAD_REQUEST.value(), "A003", "이메일 인증을 해주세요.");

	private final int status;
	private final String code;
	private final String message;
}
