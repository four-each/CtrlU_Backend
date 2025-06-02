package org.example.ctrlu.domain.auth.repository;

import static org.example.ctrlu.domain.auth.exception.AuthErrorCode.*;

import java.time.Duration;

import org.example.ctrlu.domain.auth.exception.AuthException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class RedisTokenRepository {
	private final RedisTemplate<String, String> redisTemplate;

	public void saveRefreshToken(String refreshToken, Long userId, Long expirationTime) {
		redisTemplate.opsForValue()
			.set("refreshToken: " + refreshToken,
				"userId: " + userId,
				Duration.ofMillis(expirationTime));
	}

	public void deleteRefreshToken(String refreshToken) {
		redisTemplate.opsForValue().getAndDelete(refreshToken);
	}

	public Long getValue(String refreshToken) {
		String value = redisTemplate.opsForValue().get("refreshToken: " + refreshToken);
		if (value == null) {
			throw new AuthException(NOT_FOUND_REFRESHTOKEN);
		}
		return Long.parseLong(value.split(" ")[1]);
	}
}
