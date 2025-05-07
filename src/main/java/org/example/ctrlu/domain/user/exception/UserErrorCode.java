package org.example.ctrlu.domain.user.exception;

import org.example.ctrlu.global.response.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements ErrorCode {

	; // UserErrorCode

	private final int status;
	private final int code;
	private final String message;
}
