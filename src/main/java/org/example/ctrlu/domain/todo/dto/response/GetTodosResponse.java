package org.example.ctrlu.domain.todo.dto.response;

import org.example.ctrlu.domain.todo.dto.projection.TodoProjection;
import org.example.ctrlu.domain.todo.entity.Todo;
import org.example.ctrlu.domain.todo.util.DurationTimeCalculator;
import org.example.ctrlu.global.s3.AwsS3Service;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public record GetTodosResponse(
        List<TodoDetail> todos,
        int totalPageCount,
        int totalElementCount
) {
    public static GetTodosResponse from(Page<TodoProjection> todosPage, LocalDateTime now) {
        List<TodoDetail> todos = todosPage.getContent().stream()
                .filter(Objects::nonNull)
                .map(todo -> new TodoDetail(
                    todo.todoId(),
                    todo.todoName(),
                    todo.nickname(),
                    DurationTimeCalculator.calculateInProgress(todo.createdAt(), now)
                ))
                .toList();

        return new GetTodosResponse(
            todos,
            todosPage.getTotalPages(),
            (int) todosPage.getTotalElements()
        );
    }

    public static GetTodosResponse from(TodoProjection todo, LocalDateTime now) {
        if (todo == null) {
            return new GetTodosResponse(
                    Collections.emptyList(),
                    0,
                    0
            );
        }

        List<TodoDetail> todos = new ArrayList<>();
        TodoDetail todoDetail = new TodoDetail(todo.todoId(), todo.todoName(), todo.nickname(), DurationTimeCalculator.calculateInProgress(todo.createdAt(), now));
        todos.add(todoDetail);

        return new GetTodosResponse(
                todos,
                1,
                1
        );
    }

    public static record TodoDetail(
            Long id,
            String todoName,
            String userName,
            int durationTime
    ) {}
}
