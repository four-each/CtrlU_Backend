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
import org.springframework.util.AntPathMatcher;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JwtFilter extends AbstractAuthenticationProcessingFilter {
	private static final AntPathMatcher matcher = new AntPathMatcher();
	public static final String[] WHITE_LIST = {
		"/auth/signin",
		"/auth/signup",
		"/auth/reissue",
		"/auth/verify"
	};

	public JwtFilter(AuthenticationManager authenticationManager) {
		super(new AntPathRequestMatcher("/**"));
		setAuthenticationManager(authenticationManager);
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws
		IOException, ServletException {
		HttpServletRequest httpServletRequest = (HttpServletRequest) request;
		HttpServletResponse httpServletResponse = (HttpServletResponse) response;

		if (isWhiteListed(httpServletRequest.getRequestURI())) {
			// 화이트리스트는 바로 다음 필터로 넘김
			chain.doFilter(httpServletRequest, httpServletResponse);
			return;
		}

		// 그 외 요청은 인증 처리 시도
		super.doFilter(request, response, chain);
	}

	private boolean isWhiteListed(String uri) {
		return Arrays.stream(WHITE_LIST)
			.anyMatch(pattern -> matcher.match(pattern, uri));
	}

	@Override
	public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws
		AuthenticationException, IOException {
		String authHeader = request.getHeader("Authorization");
		if (authHeader == null || !authHeader.startsWith("Bearer ")) {
			setErrorResponse(response, "JWT 토큰이 없습니다.");
			return null;
		}

		String token = authHeader.split(" ")[1]; // "Bearer " 제거
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
		response.setContentType("application/json;charset=UTF-8");

		String body = new ObjectMapper().writeValueAsString(
			Map.of(
				"status", 401,
				"error", "Unauthorized",
				"message", message
			)
		);
		response.getWriter().write(body);
		response.getWriter().flush();
	}
}
