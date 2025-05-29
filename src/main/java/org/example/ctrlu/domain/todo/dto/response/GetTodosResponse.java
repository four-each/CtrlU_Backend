package org.example.ctrlu.domain.todo.dto.response;

import org.example.ctrlu.domain.todo.entity.Todo;
import org.example.ctrlu.domain.todo.util.DurationTimeCalculator;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.util.List;

public record GetTodosResponse(
        List<TodoDetail> todos,
        int totalPageCount,
        int totalElementCount
) {
    public static GetTodosResponse from(Page<Todo> todosPage, LocalDateTime now) {
        List<TodoDetail> todos = todosPage.getContent().stream()
                .map(todo -> new TodoDetail(
                        todo.getId(),
                        todo.getStartImage(),
                        todo.getEndImage(),
                        DurationTimeCalculator.calculate(todo, now)
                ))
                .toList();

        return new GetTodosResponse(
                todos,
                todosPage.getTotalPages(),
                (int) todosPage.getTotalElements()
        );
    }

    public static record TodoDetail(
            Long id,
            String startImage,
            String endImage,
            int durationTime
    ) {}
}
