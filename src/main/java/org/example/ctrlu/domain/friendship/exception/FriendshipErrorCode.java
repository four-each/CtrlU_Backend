package org.example.ctrlu.domain.friendship.exception;

import org.example.ctrlu.global.response.ErrorCode;
import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FriendshipErrorCode implements ErrorCode {

	NOT_FOUND_USER(HttpStatus.NOT_FOUND.value(),"F001","존재하지 않는 사용자입니다."),
	NOT_FOUND_TARGET(HttpStatus.NOT_FOUND.value(),"F002","존재하지 않는 친구 요청 대상입니다."),
	NOT_FOUND_FRIENDSHIP(HttpStatus.NOT_FOUND.value(),"F003","존재하지 않는 친구 요청입니다."),
	ALREADY_REQUESTED_FRIENDSHIP(HttpStatus.CONFLICT.value(),"F004","대기중인 친구 요청이 존재합니다."),
	ALREADY_ACCEPTED_FRIENDSHIP(HttpStatus.CONFLICT.value(),"F005","이미 친구입니다."),
	REJECTED_FRIENDSHIP(HttpStatus.CONFLICT.value(),"F006","요청을 거절한 친구입니다."),
	FRIEND_LIMIT_EXCEEDED(HttpStatus.CONFLICT.value(),"F007","친구 수가 최대 한도에 도달했습니다."),
	CANNOT_FRIEND_SELF(HttpStatus.BAD_REQUEST.value(), "F008", "자기 자신에게 친구 요청을 할 수 없습니다."),
	ALREADY_EXISTS_FRIENDSHIP(HttpStatus.CONFLICT.value(), "F009", "이미 친구이거나 거절/대기중인 요청이 존재합니다."),
	FRIENDSHIP_ACCEPT_CONFLICT(HttpStatus.CONFLICT.value(), "F010", "충돌로 인해 친구 요청 수락을 실패했습니다."),
	FRIENDSHIP_REJECT_CONFLICT(HttpStatus.CONFLICT.value(), "F011", "충돌로 인해 친구 요청 거절을 실패했습니다."),
	FRIENDSHIP_CANCEL_CONFLICT(HttpStatus.CONFLICT.value(), "F012", "충돌로 인해 친구 요청 취소를 실패했습니다."),
	FRIENDSHIP_REQUEST_CONFLICT(HttpStatus.CONFLICT.value(), "F013", "충돌로 인해 친구 요청에 실패했습니다."),
	FRIENDSHIP_REQUEST_LOCK_ACQUISITION_FAILED(HttpStatus.CONFLICT.value(), "F014", "친구 요청 락 획득에 실패했습니다.");

	private final int status;
	private final String code;
	private final String message;
}
