package org.example.ctrlu.healthy;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class HealthyController {

    private final HealthyRepository healthyRepository;
    private final RedisTemplate<String, String> redisTemplate;

    @GetMapping("/api/healthy-check")
    public List<Healthy> healthyCheck() {
        List<Healthy> healthyList = healthyRepository.findAll();
        return healthyList;
    }

    @GetMapping("/api/redis/healthy-check")
    public String redisHealthyCheck() {
        String key = "test:health";
        String testValue = "redis-ok";

        redisTemplate.opsForValue().set(key, testValue);
        Object result = redisTemplate.opsForValue().get(key);

        return "Redis Connected, Value: " + result;
    }
}
