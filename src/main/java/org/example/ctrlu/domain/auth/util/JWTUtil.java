package org.example.ctrlu.domain.auth.util;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;

@Component
public class JWTUtil {
	private SecretKey secretKey;
	private static final Long VERIFYTOKEN_EXPIRATION = 300000L; // 5분

	public JWTUtil(@Value("${jwt.secret}") String secret) {
		secretKey = new SecretKeySpec(
						secret.getBytes(StandardCharsets.UTF_8),
						Jwts.SIG.HS256.key().build().getAlgorithm()
					);
	}

	public String createVerifyToken() {
		return Jwts.builder()
			.issuedAt(new Date(System.currentTimeMillis()))
			.expiration(new Date(System.currentTimeMillis() + VERIFYTOKEN_EXPIRATION))
			.signWith(secretKey)
			.compact();
	}

	public Boolean isExpired(String token) {
		try {
			Date expiration = Jwts.parser()
				.verifyWith(secretKey)
				.build()
				.parseSignedClaims(token)
				.getPayload()
				.getExpiration();

			return expiration.before(new Date());
		} catch (ExpiredJwtException e) {
			// 토큰이 만료된 경우 true 반환
			return true;
		}
	}
}
