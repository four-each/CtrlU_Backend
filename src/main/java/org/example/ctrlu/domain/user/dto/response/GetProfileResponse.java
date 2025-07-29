package org.example.ctrlu.domain.user.dto.response;

import org.example.ctrlu.domain.user.entity.User;
import org.example.ctrlu.global.s3.AwsS3Service;

public record GetProfileResponse(
	String nickname,
	String profileImage
) {
	public static GetProfileResponse from(User user, AwsS3Service awsS3Service) {
		return new GetProfileResponse(
			user.getNickname(),
			awsS3Service.generateGetPresignedUrl(user.getProfileImageKey())
		);
	}
}
