package com.example.resilient_api.domain.model;

public record BootcampListCriteria(
        Integer page,
        Integer size,
        String sortBy,
        String sortDirection
) {
}