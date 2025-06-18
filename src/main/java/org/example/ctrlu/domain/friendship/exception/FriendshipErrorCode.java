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
	ALREADY_ACCEPTED_FRIENDSHIP(HttpStatus.CONFLICT.value(),"F004","이미 친구입니다."),
	REJECTED_FRIENDSHIP(HttpStatus.CONFLICT.value(),"F005","요청을 거절한 친구입니다."),
	FRIEND_LIMIT_EXCEEDED(HttpStatus.CONFLICT.value(),"F006","친구 수가 최대 한도에 도달했습니다."),
	CANNOT_FRIEND_SELF(HttpStatus.BAD_REQUEST.value(), "F007", "자기 자신에게 친구 요청을 할 수 없습니다."),
	ALREADY_EXISTS_FRIENDSHIP(HttpStatus.CONFLICT.value(), "F008", "이미 친구이거나 거절/대기중인 요청이 존재합니다.");

	private final int status;
	private final String code;
	private final String message;
}
