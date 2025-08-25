package org.example.ctrlu.domain.user.dto.response;

import org.example.ctrlu.domain.user.entity.User;
import org.example.ctrlu.global.s3.AwsS3Service;

public record SearchUsersResponse(
	Long id,
	String nickname,
	String email,
	String image
) {
	public static SearchUsersResponse from(User user, AwsS3Service awsS3Service) {
		return new SearchUsersResponse(
			user.getId(),
			user.getNickname(),
			user.getEmail(),
			awsS3Service.generateGetPresignedUrl(user.getProfileImageKey())
		);
	}
}
