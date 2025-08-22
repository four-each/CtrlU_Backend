package org.example.ctrlu.domain.friendship.dto.response;

import org.example.ctrlu.global.s3.AwsS3Service;

public record FriendResponse(
	Long id,
	String nickname,
	String email,
	String image
) {
	public static FriendResponse from(FriendResponse friendResponse, AwsS3Service awsS3Service) {
		return new FriendResponse(
			friendResponse.id(),
			friendResponse.nickname(),
			friendResponse.email(),
			awsS3Service.generateGetPresignedUrl(friendResponse.image())
		);
	}
}
