package org.example.ctrlu.domain.todo.dto.response;

import org.example.ctrlu.domain.todo.entity.Todo;
import org.example.ctrlu.global.s3.AwsS3Service;

public record GetTodoResponse (
    String title,
    String startImage,
    String endImage,
    Integer durationTime,
    boolean isMine
){
    public static GetTodoResponse from(Todo todo, int durationTime, boolean isMine, AwsS3Service awsS3Service) {
        return new GetTodoResponse(todo.getTitle(),
                awsS3Service.generateGetPresignedUrl(todo.getStartImage()),
                awsS3Service.generateGetPresignedUrl(todo.getEndImage()),
                durationTime,
                isMine);
    }
}
