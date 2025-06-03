package org.example.ctrlu.domain.user.exception;

import org.example.ctrlu.global.response.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements ErrorCode {
	NOT_FOUND_USER(HttpStatus.BAD_REQUEST.value(),"U001","존재하지 않는 사용자입니다."),
	INVALID_PASSWORD(HttpStatus.BAD_REQUEST.value(), "U002", "유효하지 않은 비밀번호입니다.");

	private final int status;
	private final String code;
	private final String message;
}
