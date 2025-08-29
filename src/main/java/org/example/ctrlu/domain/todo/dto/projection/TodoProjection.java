package org.example.ctrlu.domain.todo.dto.projection;

import java.time.LocalDateTime;

public record TodoProjection(
        Long todoId,
        String nickname,
        LocalDateTime createdAt
) {}
