package org.example.ctrlu.domain.user.exception;

import org.example.ctrlu.global.response.BaseErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class UserExceptionHandler {
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	@ExceptionHandler({UserException.class})
	public BaseErrorResponse handleUserException(UserException e) {
		log.error("[handleUserException]", e);
		return new BaseErrorResponse(e.getExceptionStatus());
	}
}
