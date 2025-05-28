package org.example.ctrlu.domain.auth.exception;

import org.example.ctrlu.global.exception.BaseException;
import org.example.ctrlu.global.response.ErrorCode;

public class AuthException extends BaseException {
	public AuthException(ErrorCode exceptionStatus) {
		super(exceptionStatus);
	}
}
