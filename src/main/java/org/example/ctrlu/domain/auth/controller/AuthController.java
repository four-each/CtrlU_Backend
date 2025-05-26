package org.example.ctrlu.domain.auth.controller;

import org.example.ctrlu.domain.auth.application.AuthService;
import org.example.ctrlu.domain.auth.dto.request.SignupRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.view.RedirectView;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {
	private final AuthService authService;
	private static final String LOGIN_URL = "http://ctrlu.site/login";
	private static final String ERROR_URL = "http://ctrlu.site/error";

	@PostMapping("/signup")
	public void signup(
		@RequestPart("request") @Valid SignupRequest request,
		@RequestPart("userImage") MultipartFile userImage
	) {
		authService.signup(request, userImage);
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
}
