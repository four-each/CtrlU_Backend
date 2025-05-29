package org.example.ctrlu.domain.todo.util;

import org.example.ctrlu.domain.todo.entity.Todo;
import org.example.ctrlu.domain.todo.entity.TodoStatus;

import java.time.Duration;
import java.time.LocalDateTime;

public class DurationTimeCalculator {

    public static int calculate(Todo todo, LocalDateTime now) {
        TodoStatus status = todo.getStatus();

        return switch (status) {
            case IN_PROGRESS -> (int) Duration.between(todo.getCreatedAt(), now).toMillis();

            case GIVEN_UP -> {
                LocalDateTime modifiedAt = todo.getModifiedAt();
                yield (int) Duration.between(todo.getCreatedAt(), modifiedAt).toMillis();
            }

            case COMPLETED -> {
                yield todo.getDurationTime();
            }

            default -> throw new UnsupportedOperationException("지원하지 않는 상태: " + status);
        };
    }
}
