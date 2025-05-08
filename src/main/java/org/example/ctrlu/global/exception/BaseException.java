package org.example.ctrlu.global.exception;

import org.example.ctrlu.global.response.ErrorCode;

import lombok.Getter;

@Getter
public class BaseException extends RuntimeException {
	private final ErrorCode exceptionStatus;

	public BaseException(ErrorCode exceptionStatus) {
		super(exceptionStatus.getMessage());
		this.exceptionStatus = exceptionStatus;
	}
}