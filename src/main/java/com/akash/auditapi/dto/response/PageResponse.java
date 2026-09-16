package com.akash.auditapi.dto.response;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * Provides a reusable response shape for typed paginated results.
 * Its factory converts a Spring Data page into immutable response rows.
 */
public record PageResponse<T>(int pageNo, int pageSize, int numberOfElements,
                              long totalElements, int totalPages, boolean hasPrevious,
                              boolean hasNext, List<T> rows) {
    public PageResponse {
        rows = List.copyOf(rows);
    }

    public static <S, T> PageResponse<T> from(Page<S> page, Function<S, T> mapper) {
        return new PageResponse<>(page.getNumber(), page.getSize(), page.getNumberOfElements(),
                page.getTotalElements(), page.getTotalPages(), page.hasPrevious(), page.hasNext(),
                page.getContent().stream().map(mapper).toList());
    }
}
