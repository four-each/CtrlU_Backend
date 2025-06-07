package org.example.ctrlu.global.security;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class JwtFilter extends AbstractAuthenticationProcessingFilter {
	public static final String CONTENT_TYPE = "application/json;charset=UTF-8";
	private static final String AUTHORIZATION = "Authorization";
	private static final String AUTHORIZATION_PREFIX = "Bearer ";
	private static final String SPLIT_REGEX = " ";
	public static final String[] WHITE_LIST = {"/auth/signup", "/auth/signin", "/auth/reissue", "/auth/verify"};

	public JwtFilter(AuthenticationManager authenticationManager) {
		super(new AntPathRequestMatcher("/**"));
		setAuthenticationManager(authenticationManager);
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws
		IOException, ServletException {
		HttpServletRequest httpServletRequest = (HttpServletRequest) request;
		HttpServletResponse httpServletResponse = (HttpServletResponse) response;

		if (isWhiteListed(httpServletRequest)) {
			System.out.println(httpServletRequest.getRequestURI());
			chain.doFilter(httpServletRequest, httpServletResponse);  // 인증 x
			return;
		}

		// 그 외 요청은 인증 처리
		super.doFilter(request, response, chain);
	}

	private boolean isWhiteListed(HttpServletRequest request) {
		String requestUri = request.getRequestURI();
		return Arrays.asList(WHITE_LIST).contains(requestUri);
	}

	@Override
	public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws
		AuthenticationException, IOException {
		String authHeader = request.getHeader(AUTHORIZATION);
		if (authHeader == null || !authHeader.startsWith(AUTHORIZATION_PREFIX)) {
			setErrorResponse(response, "JWT 토큰이 없거나 Bearer 형식에 맞지 않습니다.");
			return null;
		}

		String token = authHeader.split(SPLIT_REGEX)[1]; // "Bearer " 제거
		JwtAuthenticationToken authenticationToken = new JwtAuthenticationToken(token);
		return this.getAuthenticationManager().authenticate(authenticationToken);
	}

	@Override
	protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response,
		FilterChain chain, Authentication authResult)
		throws IOException, ServletException {
		SecurityContextHolder.getContext().setAuthentication(authResult);
		chain.doFilter(request, response);
	}

	@Override
	protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response,
		AuthenticationException failed) throws IOException {
		setErrorResponse(response, failed.getMessage());
	}

	private void setErrorResponse(HttpServletResponse response, String message) throws IOException {
		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		response.setContentType(CONTENT_TYPE);

		String body = new ObjectMapper().writeValueAsString(
			Map.of(
				"status", 401,
				"code", "UNAUTHORIZED",
				"message", message
			)
		);
		response.getWriter().write(body);
		response.getWriter().flush();
	}
}
