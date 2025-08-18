package org.example.ctrlu.global.s3;

import java.time.Duration;
import java.util.UUID;

import org.example.ctrlu.domain.auth.dto.request.GetPresignedUrlRequest;
import org.example.ctrlu.domain.auth.dto.response.PresignedUrlResponse;
import org.example.ctrlu.global.exception.BaseException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Slf4j
@Service
@RequiredArgsConstructor
public class AwsS3Service {
	private final S3Client s3Client;
	private final S3Presigner s3Presigner;

	@Value("${cloud.aws.s3.bucket}")
	private String bucketName;

	public PresignedUrlResponse generatePutPresignedUrl(GetPresignedUrlRequest request) {
		try {
			String fileName = request.imageType().getPath() + "/" + UUID.randomUUID() + request.fileExtension();

			PutObjectRequest objectRequest = PutObjectRequest.builder()
				.bucket(bucketName)
				.key(fileName)
				.contentType("multipart/form-data")
				.build();

			PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
				.signatureDuration(Duration.ofMinutes(5))
				.putObjectRequest(objectRequest)
				.build();

			PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);
			return new PresignedUrlResponse(presignedRequest.url().toString(), fileName);
		} catch (S3Exception e) {
			throw new BaseException(S3ErrorCode.GENERATE_URL_FAILED);
		}
	}

	public String generateGetPresignedUrl(String fileName) {
		try {
			if (fileName == null || fileName.isBlank()) {
				return null;
			}

			GetObjectRequest getObjectRequest = GetObjectRequest.builder()
				.bucket(bucketName)
				.key(fileName)
				.build();

			GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
				.signatureDuration(Duration.ofMinutes(5))
				.getObjectRequest(getObjectRequest)
				.build();

			PresignedGetObjectRequest presignedGetObjectRequest = s3Presigner.presignGetObject(presignRequest);
			return presignedGetObjectRequest.url().toString();
		} catch (S3Exception e) {
			throw new BaseException(S3ErrorCode.GENERATE_URL_FAILED);
		}
	}

	public void deleteImage(String fileName){
		if (fileName == null || fileName.isBlank()) {
			return;
		}

		try {
			DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
				.bucket(bucketName)
				.key(fileName)
				.build();

			s3Client.deleteObject(deleteObjectRequest);
		} catch (S3Exception e) {
			throw new BaseException(S3ErrorCode.DELETE_IMAGE_FAILED);
		}
	}
}