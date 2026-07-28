package com.example.resilient_api.domain.model;

import java.util.List;

public record BootcampPageResult(
        List<BootcampSummary> content,
        Integer page,
        Integer size,
        Long totalElements,
        Integer totalPages
) {
}