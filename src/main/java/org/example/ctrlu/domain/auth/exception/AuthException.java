package org.example.ctrlu.domain.auth.exception;

import org.example.ctrlu.global.exception.BaseException;

public class AuthException extends BaseException {
	public AuthException(AuthErrorCode authErrorCode) {
		super(authErrorCode);
	}
}
