package org.example.ctrlu.domain.todo.exception;

import org.example.ctrlu.global.response.BaseErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class TodoExceptionHandler {
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	@ExceptionHandler({TodoException.class})
	public BaseErrorResponse handleTodoException(TodoException e) {
		log.error("[handleTodoException]", e);
		return new BaseErrorResponse(e.getExceptionStatus(), e.getMessage());
	}
}
