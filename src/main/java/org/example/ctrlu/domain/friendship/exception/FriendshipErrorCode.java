package org.example.ctrlu.domain.friendship.exception;

import org.example.ctrlu.global.response.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FriendshipErrorCode implements ErrorCode {

	; // FriendshipErrorCode

	private final int status;
	private final int code;
	private final String message;
}
