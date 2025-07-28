package org.example.ctrlu.global.s3;

import java.time.Duration;
import java.util.UUID;

import org.example.ctrlu.domain.auth.dto.request.GetPresignedUrlRequest;
import org.example.ctrlu.domain.auth.dto.response.PresignedUrlResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Service
@RequiredArgsConstructor
public class AwsS3Service {
	private final S3Client amazonS3;
	private final S3Presigner s3Presigner;

	@Value("${cloud.aws.s3.bucket}")
	private String bucketName;

	public PresignedUrlResponse generatePutPresignedUrl(GetPresignedUrlRequest request) {
		String fileName = request.imageType().getPath() + "/" + UUID.randomUUID() + request.fileExtension();

		PutObjectRequest objectRequest = PutObjectRequest.builder()
			.bucket(bucketName)
			.key(fileName)
			.build();

		PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
			.signatureDuration(Duration.ofMinutes(10))
			.putObjectRequest(objectRequest)
			.build();

		PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);
		return new PresignedUrlResponse(presignedRequest.url().toString(), fileName);
	}

	public String uploadImage(MultipartFile image) {
		if (image == null || image.isEmpty()) {
			return null;
		}
		String imageKey = "profiles/" + UUID.randomUUID() + image;

		PutObjectRequest objectRequest = PutObjectRequest.builder()
			.bucket(bucketName)
			.key(imageKey)
			.build();

		PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
			.signatureDuration(Duration.ofMinutes(5))
			.putObjectRequest(objectRequest)
			.build();

		PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);
		return "";
	}

	public void deleteImage(String fileName){
		// amazonS3.deleteObject(new DeleteObjectRequest(bucket, fileName));
	}

	private String getPublicUrl(String fileName){
		// return amazonS3.getUrl(bucket, fileName).toString();
		return "";
	}
}