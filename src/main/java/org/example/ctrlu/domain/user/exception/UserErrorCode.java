package org.example.ctrlu.domain.user.exception;

import org.example.ctrlu.global.response.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements ErrorCode {
	NOT_FOUND_USER(HttpStatus.BAD_REQUEST.value(),3001,"존재하지 않는 사용자입니다.");;

	private final int status;
	private final int code;
	private final String message;
}
