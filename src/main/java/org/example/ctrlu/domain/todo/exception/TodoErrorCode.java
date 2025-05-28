package org.example.ctrlu.domain.todo.exception;

import org.example.ctrlu.global.response.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum TodoErrorCode implements ErrorCode {
	ALREADY_EXIST_IN_PROGRESS_TODO(HttpStatus.BAD_REQUEST.value(),4001,"이미 진행중인 할 일이 존재합니다."),
	NOT_FOUND_TODO(HttpStatus.BAD_REQUEST.value(),4002,"존재하지 않는 할 일입니다."),
	NOT_IN_PROGRESS_TODO(HttpStatus.BAD_REQUEST.value(), 4003, "진행 중인 할 일이 아닙니다."),
	NOT_YOUR_TODO(HttpStatus.BAD_REQUEST.value(), 4004, "본인의 할 일이 아닙니다."),
	FAIL_TO_GET_TODO(HttpStatus.BAD_REQUEST.value(), 4005, "조회할 수 없는 할 일입니다.");

	private final int status;
	private final int code;
	private final String message;
}