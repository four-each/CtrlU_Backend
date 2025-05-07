package org.example.ctrlu.domain.todo.exception;

import org.example.ctrlu.global.exception.BaseException;
import org.example.ctrlu.global.response.ErrorCode;

public class TodoException extends BaseException {
	public TodoException(ErrorCode exceptionStatus) {
		super(exceptionStatus);
	}
}
