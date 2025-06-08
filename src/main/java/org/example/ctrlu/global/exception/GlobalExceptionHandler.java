package org.example.ctrlu.global.exception;

import static org.example.ctrlu.global.response.BaseErrorCode.*;

import org.example.ctrlu.global.response.BaseErrorResponse;
import org.example.ctrlu.global.response.ErrorCode;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public BaseErrorResponse handleValidationException(MethodArgumentNotValidException e) {
		log.error("[handleValidationException]", e);
		String errorMessage = e.getBindingResult().getFieldErrors().stream()
			.findFirst()
			.map(DefaultMessageSourceResolvable::getDefaultMessage)
			.orElse("잘못된 요청입니다.");

		return new BaseErrorResponse(new ErrorCode() {
			@Override
			public int getStatus() {
				return HttpStatus.BAD_REQUEST.value();
			}

			@Override
			public String getCode() {
				return "B003";
			}

			@Override
			public String getMessage() {
				return errorMessage;
			}
		});
	}

	@ResponseStatus(HttpStatus.BAD_REQUEST)
	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public BaseErrorResponse handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
		log.error("[handleMethodArgumentTypeMismatchException]", e);
		return new BaseErrorResponse(ARGUMENT_TYPE_MISMATCH);
	}

	@ResponseStatus(HttpStatus.BAD_REQUEST)
	@ExceptionHandler(MissingServletRequestPartException.class)
	public BaseErrorResponse handleMissingServletRequestPartException(MissingServletRequestPartException e) {
		log.error("[handleMissingServletRequestPartException]", e);
		return new BaseErrorResponse(IMAGE_NOT_PRESENT);
	}

}
