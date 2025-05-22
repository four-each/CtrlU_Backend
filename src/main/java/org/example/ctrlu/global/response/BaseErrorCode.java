package org.example.ctrlu.global.response;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BaseErrorCode implements ErrorCode {
	SUCCESS(HttpStatus.OK.value(), "B001", "요청에 성공하였습니다."),
	ARGUMENT_TYPE_MISMATCH(HttpStatus.BAD_REQUEST.value(), "B002", "잘못된 파라미터 타입입니다.");

	private final int status;
	private final String code;
	private final String message;
}