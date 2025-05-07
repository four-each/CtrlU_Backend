package org.example.ctrlu.global.response;

import org.springframework.http.HttpStatus;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum BaseErrorCode implements ErrorCode {
	/**
	 * 1000: 요청 성공 (OK)
	 */
	SUCCESS(1000, HttpStatus.OK.value(), "요청에 성공하였습니다."),

	/**
	 * 2000: 요청 실패
	 */
	ARGUMENT_TYPE_MISMATCH(2002, HttpStatus.BAD_REQUEST.value(), "잘못된 파라미터 타입입니다.");

	private final int code;
	private final int status;
	private final String message;

	@Override
	public int getCode() {
		return code;
	}

	@Override
	public int getStatus() {
		return status;
	}

	@Override
	public String getMessage() {
		return message;
	}
}