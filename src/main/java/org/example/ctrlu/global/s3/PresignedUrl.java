package org.example.ctrlu.global.s3;

public record PresignedUrl(
	String presignedUrl,
	String imageKey
) {
}