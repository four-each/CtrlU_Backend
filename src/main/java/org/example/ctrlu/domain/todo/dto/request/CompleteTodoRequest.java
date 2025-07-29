package org.example.ctrlu.domain.todo.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CompleteTodoRequest(
        int durationTime,
		@NotBlank
		String endImageKey
) {
}
