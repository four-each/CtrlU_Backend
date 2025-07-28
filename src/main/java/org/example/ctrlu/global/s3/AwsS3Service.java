package org.example.ctrlu.global.s3;

import java.time.Duration;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;


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

	// 파일명을 난수화하기 위해 UUID를 활용하여 난수를 돌린다.
	private String createFileName(String fileName){
		return UUID.randomUUID().toString().concat(getFileExtension(fileName));
	}

	//  "."의 존재 유무만 판단
	private String getFileExtension(String fileName){
		try{
			return fileName.substring(fileName.lastIndexOf("."));
		} catch (StringIndexOutOfBoundsException e){
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "잘못된 형식의 파일" + fileName + ") 입니다.");
		}
	}
}