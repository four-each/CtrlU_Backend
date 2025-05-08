package org.example.ctrlu.healthy;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import java.util.List;

import org.example.ctrlu.global.s3.AwsS3Service;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class HealthyController {
    private final AwsS3Service awsS3Service;

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
    @PostMapping("/api/file")
    public String uploadFile(MultipartFile file) {
        return awsS3Service.uploadImage(file);
    }

    @DeleteMapping("/api/file")
    public void deleteFile(@RequestParam String fileName) {
        awsS3Service.deleteImage(fileName);
    }
}
