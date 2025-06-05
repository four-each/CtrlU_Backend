package org.example.ctrlu.domain.todo.dto.response;

import org.example.ctrlu.domain.todo.entity.Todo;
import org.example.ctrlu.domain.todo.entity.TodoStatus;
import org.example.ctrlu.domain.todo.util.DurationTimeCalculator;

import java.time.LocalDateTime;
import java.time.LocalTime;

public record GetRecentUploadTodoResponse(
        String title,
        String startImage,
        String endImage,
        LocalTime challengeTime,
        int durationTime,
        TodoStatus status,
        Long nextId,
        Long prevId,
        int totalCount
) {
    public static GetRecentUploadTodoResponse from(LocalDateTime now, Todo todo, Long prevId, Long nextId, int totalCount) {
        return new GetRecentUploadTodoResponse(
                todo.getTitle(),
                todo.getStartImage(),
                todo.getEndImage(),
                todo.getChallengeTime(),
                DurationTimeCalculator.calculate(todo, now),
                todo.getStatus(),
                nextId,
                prevId,
                totalCount
        );
    }
}
