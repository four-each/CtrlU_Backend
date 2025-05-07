package org.example.ctrlu.global.s3;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AwsS3Service {
	private final AmazonS3 amazonS3;

	@Value("${cloud.aws.s3.bucket}")
	private String bucket;

	public String uploadImage(MultipartFile image) {
		if (image == null || image.isEmpty()) {
			return null;
		}

		// 메타데이터 설정
		String fileName = createFileName(image.getOriginalFilename());
		ObjectMetadata objectMetadata = new ObjectMetadata();
		objectMetadata.setContentLength(image.getSize());
		objectMetadata.setContentType(image.getContentType());

		try(InputStream inputStream = image.getInputStream()){
			// S3에 파일 업로드 요청 생성
			PutObjectRequest putObjectRequest =
				new PutObjectRequest(bucket, fileName, inputStream, objectMetadata);

			//  S3에 파일 업로드
			amazonS3.putObject(putObjectRequest);
		} catch (IOException e){
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "파일 업로드에 실패했습니다.");
		}

		return getPublicUrl(fileName);
	}

	public void deleteImage(String fileName){
		amazonS3.deleteObject(new DeleteObjectRequest(bucket, fileName));
	}

	private String getPublicUrl(String fileName){
		return amazonS3.getUrl(bucket, fileName).toString();
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