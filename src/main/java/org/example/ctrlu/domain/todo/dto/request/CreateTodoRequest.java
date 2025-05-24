package org.example.ctrlu.domain.todo.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

public record CreateTodoRequest (
        @NotBlank
        String title,
        @NotNull
        LocalTime challengeTime
) {
}
