package org.example.ctrlu.domain.auth.dto.request;

import org.example.ctrlu.global.s3.ImagePath;

public record GetPresignedUrlRequest(
	ImagePath imageType,
	String fileExtension,
	String contentType
) {
}
