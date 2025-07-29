package org.example.ctrlu.domain.auth.controller;

import org.example.ctrlu.domain.auth.application.AuthService;
import org.example.ctrlu.domain.auth.dto.request.DeleteUserRequest;
import org.example.ctrlu.domain.auth.dto.request.FindPasswordRequest;
import org.example.ctrlu.domain.auth.dto.request.GetPresignedUrlRequest;
import org.example.ctrlu.domain.auth.dto.request.ResetPasswordRequest;
import org.example.ctrlu.domain.auth.dto.request.SigninRequest;
import org.example.ctrlu.domain.auth.dto.request.SignupRequest;
import org.example.ctrlu.domain.auth.dto.response.PresignedUrlResponse;
import org.example.ctrlu.domain.auth.dto.response.SigninResponse;
import org.example.ctrlu.domain.auth.dto.response.TokenInfo;
import org.example.ctrlu.global.response.BaseResponse;
import org.example.ctrlu.global.s3.AwsS3Service;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.util.WebUtils;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {
	private final AuthService authService;
	private final AwsS3Service awsS3Service;
	private static final String LOGIN_URL = "http://ctrlu.site/login";
	private static final String ERROR_URL = "http://ctrlu.site/error";
	private static final String RESET_PASSWORD_URL = "http://ctrlu.site/reset-password";
	private static final String COOKIE_REFRESHTOKEN = "refreshToken=";
	private static final String COOKIE_NAME_REFRESHTOKEN = "refreshToken";
	private static final String COOKIE_FLAGS = "; Path=/; HttpOnly; Secure; ";
	private static final String COOKIE_MAXAGE = "Max-Age=";
	private static final Long REFRESHTOKEN_EXPIRATION_TIME = 60 * 60 * 24 * 7L; // 7일
	private static final String COOKIE_SAMESITE = "; SameSite=None";

	@PostMapping("/presigned-url")
	public ResponseEntity<PresignedUrlResponse> getPresignedUrl(@RequestBody @Valid GetPresignedUrlRequest request) {
		PresignedUrlResponse response = awsS3Service.generatePutPresignedUrl(request);
		return ResponseEntity.ok(response);
	}

	@PostMapping("/signup")
	public BaseResponse<Void> signup(
		@RequestBody @Valid SignupRequest request
	) {
		authService.signup(request);
		return new BaseResponse<>(null);
	}

	@GetMapping("/verify")
	public Object verifyEmail(@RequestParam("token") String token) {
		boolean isComplete = authService.verifyEmail(token);

		if (isComplete) {
			return new RedirectView(LOGIN_URL);
		} else {
			return new RedirectView(ERROR_URL);  // 링크 만료 페이지로 이동
		}
	}

	@PostMapping("/signin")
	public BaseResponse<SigninResponse> signin(@Valid @RequestBody SigninRequest request, HttpServletResponse response) {
		TokenInfo tokenInfo = authService.signin(request);
		response.setHeader(HttpHeaders.SET_COOKIE,
			COOKIE_REFRESHTOKEN
				+ tokenInfo.refreshToken()
				+ COOKIE_FLAGS
				+ COOKIE_MAXAGE
				+ REFRESHTOKEN_EXPIRATION_TIME
				+ COOKIE_SAMESITE);
		return new BaseResponse<>(new SigninResponse(tokenInfo.accessToken()));
	}

	@PostMapping("/reissue")
	public BaseResponse<SigninResponse> reissue(HttpServletRequest request, HttpServletResponse response) {
		Cookie cookie = WebUtils.getCookie(request, COOKIE_NAME_REFRESHTOKEN);
		TokenInfo tokenInfo = authService.reissue(cookie);
		response.setHeader(HttpHeaders.SET_COOKIE,
			COOKIE_REFRESHTOKEN
				+ tokenInfo.refreshToken()
				+ COOKIE_FLAGS
				+ COOKIE_MAXAGE
				+ REFRESHTOKEN_EXPIRATION_TIME
				+ COOKIE_SAMESITE);
		return new BaseResponse<>(new SigninResponse(tokenInfo.accessToken()));
	}

	@PostMapping("/logout")
	public BaseResponse<Void> logout(
		HttpServletRequest request,
		HttpServletResponse response
	) {
		Cookie refreshToken = WebUtils.getCookie(request, "refreshToken");
		authService.logout(refreshToken);
		clearCookie(response);
		return new BaseResponse<>(null);
	}

	@DeleteMapping("/withdraw")
	public BaseResponse<Void> deleteUser(
		@AuthenticationPrincipal Long userId,
		@Valid @RequestBody DeleteUserRequest deleteUserRequest,
		HttpServletRequest request,
		HttpServletResponse response
	) {
		Cookie refreshToken = WebUtils.getCookie(request, "refreshToken");
		authService.deleteUser(userId, deleteUserRequest, refreshToken);
		clearCookie(response);
		return new BaseResponse<>(null);
	}

	private static void clearCookie(HttpServletResponse response) {
		String cookie = "refreshToken=; Path=/; HttpOnly; Secure; Max-Age=0; SameSite=None";
		response.setHeader("Set-Cookie", cookie);
		response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
		response.setHeader("Pragma", "no-cache");
		response.setHeader("Expires", "0");
	}

	@PostMapping("/find-password")
	public BaseResponse<Void> findPassword(@Valid @RequestBody FindPasswordRequest request) {
		authService.findPassword(request);
		return new BaseResponse<>(null);
	}

	@GetMapping("/reset-password")
	public Object verifyResetToken(@RequestParam("token") String token) {
		boolean isComplete = authService.verifyResetToken(token);

		if (isComplete) {
			return new RedirectView(RESET_PASSWORD_URL);
		} else {
			return new RedirectView(ERROR_URL);  // 링크 만료 페이지로 이동
		}
	}

	@PostMapping("/reset-password")
	public BaseResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
		authService.resetPassword(request);
		return new BaseResponse<>(null);
	}
}
