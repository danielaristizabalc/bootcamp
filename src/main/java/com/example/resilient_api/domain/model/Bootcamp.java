package com.example.resilient_api.domain.model;

import java.time.LocalDate;
import java.util.List;

public record Bootcamp(
    Long id,
    String name,
    String description,
    LocalDate releaseDate,
    Integer duration,
    List<Long> capabilityIds
) {
}
