package org.example.ctrlu.global.s3;

import org.example.ctrlu.global.response.ErrorCode;
import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum S3ErrorCode implements ErrorCode {
	GENERATE_URL_FAILED(HttpStatus.INTERNAL_SERVER_ERROR.value(), "S001", "Presigned URL 생성에 실패했습니다.");

	private final int status;
	private final String code;
	private final String message;
}