package org.example.ctrlu.domain.friendship.exception;

import org.example.ctrlu.global.exception.BaseException;
import org.example.ctrlu.global.response.ErrorCode;

public class FriendshipException extends BaseException {
	public FriendshipException(ErrorCode exceptionStatus) {
		super(exceptionStatus);
	}
}
