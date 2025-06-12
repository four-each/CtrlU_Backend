package org.example.ctrlu.domain.friendship.exception;

import org.example.ctrlu.global.response.ErrorCode;
import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FriendshipErrorCode implements ErrorCode {

	NOT_FOUND_USER(HttpStatus.NOT_FOUND.value(),"F001","존재하지 않는 사용자입니다."),
	NOT_FOUND_FRIENDSHIP(HttpStatus.NOT_FOUND.value(),"F001","존재하지 않는 친구입니다.");

	private final int status;
	private final String code;
	private final String message;
}
