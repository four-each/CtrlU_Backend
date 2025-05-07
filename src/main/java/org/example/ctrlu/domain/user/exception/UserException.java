package org.example.ctrlu.domain.user.exception;

import org.example.ctrlu.global.exception.BaseException;
import org.example.ctrlu.global.response.ErrorCode;

public class UserException extends BaseException {
	public UserException(ErrorCode exceptionStatus) {
		super(exceptionStatus);
	}
}
