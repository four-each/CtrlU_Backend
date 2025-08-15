package org.example.ctrlu.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
	@Override
	public void addCorsMappings(CorsRegistry registry) {
		registry.addMapping("/**") // 모든 경로에 대해
			.allowedOrigins("https://ctrlu.site") // 허용할 출처
			.allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS") // 허용할 메서드
			.allowedHeaders("Authorization", "Content-Type", "Accept")
			.exposedHeaders("Authorization", "Set-Cookie")
			.allowCredentials(true)
			.maxAge(3600);
	}
}