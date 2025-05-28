package org.example.ctrlu.domain.auth.exception;

import org.example.ctrlu.global.response.BaseErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class AuthExceptionHandler {
	@ExceptionHandler({AuthException.class})
	public BaseErrorResponse handleAuthException(AuthException e) {
		log.error("[handleAuthException]", e);
		return new BaseErrorResponse(e.getExceptionStatus());
	}
}
