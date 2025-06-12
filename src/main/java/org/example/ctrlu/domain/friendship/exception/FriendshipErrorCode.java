package org.example.ctrlu.domain.friendship.exception;

import org.example.ctrlu.global.response.ErrorCode;
import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FriendshipErrorCode implements ErrorCode {

	NOT_FOUND_USER(HttpStatus.NOT_FOUND.value(),"F001","존재하지 않는 사용자입니다."),
	NOT_FOUND_FRIENDSHIP(HttpStatus.NOT_FOUND.value(),"F002","존재하지 않는 친구입니다."),
	ALREADY_REQUESTED_FRIENDSHIP(HttpStatus.CONFLICT.value(),"F003","대기중인 친구 요청이 존재합니다."),
	FRIENDSHIP_EXISTS(HttpStatus.CONFLICT.value(),"F004","이미 친구입니다."),
	REJECTED_FRIENDSHIP(HttpStatus.CONFLICT.value(),"F005","이미 친구 요청을 거절한 친구입니다.");

	private final int status;
	private final String code;
	private final String message;
}
