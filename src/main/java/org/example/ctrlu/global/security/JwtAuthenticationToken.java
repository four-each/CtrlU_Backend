package org.example.ctrlu.global.security;

import java.util.Collection;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

public class JwtAuthenticationToken extends AbstractAuthenticationToken {
	private final String token;
	private Object principal;  // 인증된 사용자의 주체(예: User ID)

	/**
	 * 인증 전 (필터에서 토큰을 추출하여 인증 매니저에게 전달할 때 사용)
	 * @param token JWT 토큰 문자열
	 */
	public JwtAuthenticationToken(String token) {
		super(null);
		this.token = token;
		setAuthenticated(false);
	}

	/**
	 * (AuthenticationProvider에서 인증이 성공한 후 인증 객체를 반환할 때 사용됨.
	 * 반환된 인증 객체는 Spring Security 컨텍스트에 저장됨.)
	 * @param principal 인증된 사용자의 주체 (예: User ID)
	 * @param authorities 사용자에게 부여된 권한 목록
	 */
	public JwtAuthenticationToken(Object principal, Collection<? extends GrantedAuthority> authorities) {
		super(authorities);
		this.principal = principal;
		this.token = null;
		setAuthenticated(true);
	}

	@Override
	public Object getCredentials() {
		return token;
	}

	@Override
	public Object getPrincipal() {
		return principal;  // 인증 전에는 null, 인증 후에는 인증된 사용자의 주체
	}
}
