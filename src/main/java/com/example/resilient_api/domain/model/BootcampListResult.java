package com.example.resilient_api.domain.model;

import java.util.List;

public record BootcampListResult(
        List<BootcampListItem> content,
        Integer page,
        Integer size,
        Long totalElements,
        Integer totalPages
) {
}