package org.example.ctrlu.domain.todo.exception;

import org.example.ctrlu.global.response.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TodoErrorCode implements ErrorCode {

	; // TodoErrorCode

	private final int status;
	private final int code;
	private final String message;
}