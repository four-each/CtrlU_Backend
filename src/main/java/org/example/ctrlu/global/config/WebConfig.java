package org.example.ctrlu.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
	@Override
	public void addCorsMappings(final CorsRegistry registry) {
		// ✅ 모든 요청을 허용하도록 수정한 코드
		registry.addMapping("/**")
				.allowedOrigins("*") // 모든 출처 허용
				.allowedMethods("*") // 모든 HTTP 메소드 허용
				.allowedHeaders("*") // 모든 헤더 허용
				.allowCredentials(true); // 와일드카드 사용 시에는 false로 설정해야 함

		/*
		registry.addMapping("/**")
			.allowedOrigins("https://www.ctrlu.site")
			.allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
			.allowedHeaders("Authorization", "Content-Type", "Accept")
			.exposedHeaders("Authorization", "Set-Cookie")
			.allowCredentials(true)
			.maxAge(3600);*/
	}
}
