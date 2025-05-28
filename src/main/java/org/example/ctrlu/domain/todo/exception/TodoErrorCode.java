package org.example.ctrlu.domain.todo.exception;

import org.example.ctrlu.global.response.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum TodoErrorCode implements ErrorCode {
	ALREADY_EXIST_PROCEEDING_TODO(HttpStatus.BAD_REQUEST.value(),"T001","이미 진행중인 할 일이 존재합니다."),
	NOT_FOUND_TODO(HttpStatus.BAD_REQUEST.value(),"T002","존재하지 않는 할 일입니다.")
	;

	private final int status;
	private final String code;
	private final String message;
}