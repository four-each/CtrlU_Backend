package org.example.ctrlu.domain.auth.controller;

import org.example.ctrlu.domain.auth.application.AuthService;
import org.example.ctrlu.domain.auth.dto.request.SigninRequest;
import org.example.ctrlu.domain.auth.dto.request.SignupRequest;
import org.example.ctrlu.domain.auth.dto.response.SigninResponse;
import org.example.ctrlu.domain.auth.dto.response.TokenInfo;
import org.example.ctrlu.global.response.BaseResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
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
	private static final String LOGIN_URL = "http://ctrlu.site/login";
	private static final String ERROR_URL = "http://ctrlu.site/error";
	private static final String COOKIE_REFRESHTOKEN = "refreshToken=";
	private static final String COOKIE_NAME_REFRESHTOKEN = "refreshToken";
	private static final String COOKIE_FLAGS = "; Path=/; HttpOnly; Secure; ";
	private static final String COOKIE_MAXAGE = "Max-Age=";
	private static final Long REFRESHTOKEN_EXPIRATION_TIME = 60 * 60 * 24 * 7L; // 7일
	private static final String COOKIE_SAMESITE = "; SameSite=None";

	@PostMapping("/signup")
	public BaseResponse<Void> signup(
		@RequestPart("request") @Valid SignupRequest request,
		@RequestPart("userImage") MultipartFile userImage
	) {
		authService.signup(request, userImage);
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
}
