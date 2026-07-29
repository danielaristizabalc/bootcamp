package com.example.resilient_api.domain.model;

import java.time.LocalDate;

public record BootcampBasicInfo(
        Long id,
        String name,
        LocalDate releaseDate,
        Integer duration
) {
}