package org.example.ctrlu.domain.auth.exception;

import org.example.ctrlu.global.response.ErrorCode;
import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorCode {
	ALREADY_EXIST_EMAIL(HttpStatus.CONFLICT.value(), "A001", "이미 존재하는 이메일입니다."),
	FAILED_SEND_EMAIL(HttpStatus.INTERNAL_SERVER_ERROR.value(), "A002", "인증 메일 전송에 실패하였습니다."),
	TRY_EMAIL_VERIFICATION(HttpStatus.UNAUTHORIZED.value(), "A003", "이메일 인증을 해주세요."),
	INVALID_PASSWORD(HttpStatus.UNAUTHORIZED.value(), "A004", "유효하지 않은 비밀번호입니다."),
	EXPIRED_REFRESHTOKEN(HttpStatus.UNAUTHORIZED.value(), "A005", "RefreshToken 유효 기간이 만료되었습니다."),
	NOT_FOUND_REFRESHTOKEN_IN_REDIS(HttpStatus.UNAUTHORIZED.value(), "A006", "Redis에 RefreshToken이 존재하지 않습니다."),
	NOT_FOUND_USER(HttpStatus.NOT_FOUND.value(),"A007","존재하지 않는 사용자입니다."),
	NOT_FOUND_REFRESHTOKEN_IN_COOKIE(HttpStatus.UNAUTHORIZED.value(),"A008","쿠키에 RefreshToken이 존재하지 않습니다."),
	INVALID_REFRESHTOKEN(HttpStatus.UNAUTHORIZED.value(),"A009","유효하지 않은 RefreshToken입니다.");

	private final int status;
	private final String code;
	private final String message;
}
