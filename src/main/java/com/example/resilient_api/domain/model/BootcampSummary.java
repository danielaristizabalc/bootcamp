package com.example.resilient_api.domain.model;

import java.time.LocalDate;
import java.util.List;

public record BootcampSummary(
        Long id,
        String name,
        String description,
        LocalDate releaseDate,
        Integer duration,
        Long capabilityCount,
        List<Long> capabilityIds
) {
}