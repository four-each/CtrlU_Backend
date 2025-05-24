package org.example.ctrlu.domain.todo.dto.response;

import org.example.ctrlu.domain.todo.entity.Todo;

public record GetTodoResponse (
    String title,
    String startImage,
    String endImage,
    Integer durationTime,
    boolean isMine
){
    public static GetTodoResponse from(Todo todo, int durationTime, boolean isMine) {
        return new GetTodoResponse(todo.getTitle(),
                todo.getStartImage(),
                todo.getEndImage(),
                durationTime,
                isMine);
    }
}
