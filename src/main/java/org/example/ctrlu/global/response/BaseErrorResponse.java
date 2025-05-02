package org.example.ctrlu.global.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@JsonPropertyOrder({"code", "status", "message", "timestamp"})
public class BaseErrorResponse implements ErrorCode {
	private final int code;
	private final int status;
	private final String message;

	public BaseErrorResponse(ErrorCode status){
		this.code = status.getCode();
		this.status = status.getStatus();
		this.message = status.getMessage();
	}

	@Override
	public int getCode() {
		return code;
	}

	@Override
	public int getStatus() {
		return status;
	}

	@Override
	public String getMessage() {
		return message;
	}
}