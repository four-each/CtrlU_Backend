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
			.set("refreshToken:" + refreshToken,
				"userId:" + userId,
				Duration.ofMillis(expirationTime));
	}

	public void deleteRefreshToken(String refreshToken) {
		if (redisTemplate.hasKey("refreshToken:" + refreshToken)) {
			redisTemplate.opsForValue().getAndDelete("refreshToken:" + refreshToken);
		} else {
			throw new AuthException(NOT_FOUND_REFRESHTOKEN_IN_REDIS);
		}
	}

	public Long getUserIdFromRefreshToken(String refreshToken) {
		if (!redisTemplate.hasKey("refreshToken:" + refreshToken)) {
			throw new AuthException(NOT_FOUND_REFRESHTOKEN_IN_REDIS);
		}

		String value = redisTemplate.opsForValue().get("refreshToken:" + refreshToken);
		return Long.parseLong(value.split(":")[1]);
	}
}
