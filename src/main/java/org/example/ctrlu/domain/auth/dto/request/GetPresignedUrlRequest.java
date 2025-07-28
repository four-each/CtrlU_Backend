package org.example.ctrlu.domain.auth.dto.request;

import org.example.ctrlu.global.s3.ImageType;

public record GetPresignedUrlRequest(
	ImageType imageType,
	String fileExtension
) {
}
