package org.example.ctrlu.domain.todo.exception;

import org.example.ctrlu.global.response.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum TodoErrorCode implements ErrorCode {
	ALREADY_EXIST_IN_PROGRESS_TODO(HttpStatus.BAD_REQUEST.value(),"T001","이미 진행중인 할 일이 존재합니다."),
	NOT_FOUND_TODO(HttpStatus.BAD_REQUEST.value(),"T002","존재하지 않는 할 일입니다."),
	NOT_IN_PROGRESS_TODO(HttpStatus.BAD_REQUEST.value(), "T003", "진행 중인 할 일이 아닙니다."),
	NOT_YOUR_TODO(HttpStatus.BAD_REQUEST.value(), "T004", "본인의 할 일이 아닙니다."),
	FAIL_TO_GET_TODO(HttpStatus.BAD_REQUEST.value(), "T005", "조회할 수 없는 할 일입니다."),
	FAIL_TO_GET_FRIEND_TODOS(HttpStatus.BAD_REQUEST.value(), "T006", "친구는 진행 중인 할 일 목록만 조회 가능합니다." ),
	NOT_TARGET_TODO(HttpStatus.BAD_REQUEST.value(), "T007", "친구의 할 일이 아닙니다."),
	NO_RECENT_TODO(HttpStatus.BAD_REQUEST.value(), "T008", "24시간 내 등록된 할 일이 없습니다.");

	private final int status;
	private final String code;
	private final String message;
}