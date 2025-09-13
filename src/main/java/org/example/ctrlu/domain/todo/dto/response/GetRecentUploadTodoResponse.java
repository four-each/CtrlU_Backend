package org.example.ctrlu.domain.todo.dto.response;

import org.example.ctrlu.domain.todo.entity.Todo;
import org.example.ctrlu.domain.todo.entity.TodoStatus;
import org.example.ctrlu.domain.todo.util.DurationTimeCalculator;
import org.example.ctrlu.global.s3.AwsS3Service;

import java.time.LocalDateTime;
import java.time.LocalTime;

public record GetRecentUploadTodoResponse(
        String title,
        String profileImage,
        String startImage,
        String endImage,
        String userName,
        LocalTime challengeTime,
        int durationTime,
        TodoStatus status,
        Long nextId,
        Long prevId,
        int totalCount
) {
    public static GetRecentUploadTodoResponse from(LocalDateTime now,String profileImage, String userName, Todo todo, Long prevId, Long nextId, int totalCount, AwsS3Service awsS3Service) {
        return new GetRecentUploadTodoResponse(
            todo.getTitle(),
            awsS3Service.generateGetPresignedUrl(profileImage),
            awsS3Service.generateGetPresignedUrl(todo.getStartImage()),
            awsS3Service.generateGetPresignedUrl(todo.getEndImage()),
            userName,
            todo.getChallengeTime(),
            DurationTimeCalculator.calculate(todo, now),
            todo.getStatus(),
            nextId,
            prevId,
            totalCount
        );
    }
}
