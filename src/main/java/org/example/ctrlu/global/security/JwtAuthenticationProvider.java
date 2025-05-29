package org.example.ctrlu.global.security;

import java.util.List;

import org.example.ctrlu.domain.auth.util.JWTUtil;
import org.example.ctrlu.domain.user.entity.User;
import org.example.ctrlu.domain.user.entity.UserStatus;
import org.example.ctrlu.domain.user.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationProvider implements AuthenticationProvider {
	private static final String ROLE_USER = "ROLE_USER";
	private final JWTUtil jwtUtil;
	private final UserRepository userRepository;

	@Override
	public Authentication authenticate(Authentication authentication) throws AuthenticationException {
		String token = (String) authentication.getCredentials();
		log.info(token);
		if (jwtUtil.isExpired(token)) {
			throw new BadCredentialsException("토큰이 만료되었습니다.");
		}

		Long userIdFromToken = jwtUtil.getUserIdFromToken(token);
		if (userIdFromToken == null) {
			throw new BadCredentialsException("유효하지 않은 토큰입니다.");
		}

		User user = userRepository.findByIdAndStatus(userIdFromToken, UserStatus.ACTIVE)
			.orElseThrow(() -> new BadCredentialsException("존재하지 않거나 비활성화된 사용자의 토큰입니다."));

		List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(ROLE_USER));
		return new JwtAuthenticationToken(user.getId(), authorities);
	}

	@Override
	public boolean supports(Class<?> authentication) {
		return JwtAuthenticationToken.class.isAssignableFrom(authentication);
	}
}
