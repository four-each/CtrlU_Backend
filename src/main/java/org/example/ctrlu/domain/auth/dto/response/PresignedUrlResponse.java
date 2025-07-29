package org.example.ctrlu.domain.auth.dto.response;

public record PresignedUrlResponse(
	String presignedUrl,
	String imageKey
) {
}