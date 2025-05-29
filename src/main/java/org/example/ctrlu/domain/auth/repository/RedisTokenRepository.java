package org.example.ctrlu.domain.auth.repository;

import java.time.Duration;

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
}
