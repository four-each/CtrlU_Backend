package org.example.ctrlu.domain.friendship.exception;

import org.example.ctrlu.global.response.BaseErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class FriendshipExceptionHandler {
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	@ExceptionHandler({FriendshipException.class})
	public BaseErrorResponse handleFriendshipException(FriendshipException e) {
		log.error("[handleFriendshipException]", e);
		return new BaseErrorResponse(e.getExceptionStatus());
	}
}
