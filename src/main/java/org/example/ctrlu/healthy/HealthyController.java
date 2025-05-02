package org.example.ctrlu.healthy;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class HealthyController {

    private final HealthyRepository healthyRepository;

    @GetMapping("/api/healthy-check")
    public List<Healthy> healthyCheck() {
        List<Healthy> healthyList = healthyRepository.findAll();
        return healthyList;
    }
}
