package org.example.ctrlu.domain.todo.dto.response;

import org.example.ctrlu.domain.todo.entity.Todo;
import org.example.ctrlu.global.s3.AwsS3Service;

import java.time.LocalDate;
import java.time.LocalTime;

public record GetTodoResponse (
    String title,
    String startImage,
    String endImage,
    String profileImage,
    LocalTime challengeTime,
    Integer durationTime,
    boolean isMine
){
    public static GetTodoResponse from(Todo todo, String profileImage, int durationTime, boolean isMine, AwsS3Service awsS3Service) {
        return new GetTodoResponse(todo.getTitle(),
                awsS3Service.generateGetPresignedUrl(todo.getStartImage()),
                awsS3Service.generateGetPresignedUrl(todo.getEndImage()),
                awsS3Service.generateGetPresignedUrl(profileImage),
                todo.getChallengeTime(),
                durationTime,
                isMine);
    }
}
