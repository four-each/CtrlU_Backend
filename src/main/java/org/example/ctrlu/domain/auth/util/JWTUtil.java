package org.example.ctrlu.domain.auth.util;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;

@Component
public class JWTUtil {
	private SecretKey secretKey;

	public JWTUtil(@Value("${jwt.secret}") String secret) {
		secretKey = new SecretKeySpec(
						secret.getBytes(StandardCharsets.UTF_8),
						Jwts.SIG.HS256.key().build().getAlgorithm()
					);
	}

	public String createVerifyToken(Long expirationTime) {
		return Jwts.builder()
			.issuedAt(new Date(System.currentTimeMillis()))
			.expiration(new Date(System.currentTimeMillis() + expirationTime))
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
		} catch (JwtException e) {
			return true;
		}
	}

	public String createAccessToken(Long userId, Long expirationTime) {
		return Jwts.builder()
			.subject(String.valueOf(userId))
			.issuedAt(new Date(System.currentTimeMillis()))
			.expiration(new Date(System.currentTimeMillis() + expirationTime))
			.signWith(secretKey)
			.compact();
	}

	public String createRefreshToken(Long expirationTime) {
		return Jwts.builder()
			.issuedAt(new Date(System.currentTimeMillis()))
			.expiration(new Date(System.currentTimeMillis() + expirationTime))
			.signWith(secretKey)
			.compact();
	}

	public Long getUserIdFromToken(String token) {
		String userId = Jwts.parser()
			.verifyWith(secretKey)
			.build()
			.parseSignedClaims(token)
			.getPayload()
			.getSubject();

		return Long.parseLong(userId);
	}

}
