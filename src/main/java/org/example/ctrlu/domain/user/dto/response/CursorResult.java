package org.example.ctrlu.domain.user.dto.response;

import java.util.List;
import java.util.function.Function;

import org.springframework.data.domain.Slice;

public record CursorResult<T>(
	List<T> values,
	Boolean hasNext,
	Long nextCursorId
) {
	public static <E, T> CursorResult<T> of(Slice<E> slice, Function<E, T> converter, Function<T, Long> idExtractor) {
		List<T> values = slice.getContent().stream()
			.map(converter)
			.toList();

		if (values.isEmpty()) {
			return new CursorResult<>(values, slice.hasNext(), null);
		}

		Long nextCursorId = idExtractor.apply(values.get(values.size() - 1));
		return new CursorResult<>(values, slice.hasNext(), nextCursorId);
	}
}
