package com.example.resilient_api.infrastructure.adapters.reportadapter.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.util.List;

public record BootcampReportRequestDTO(
        Long id,
        String name,
        String description,
        LocalDate releaseDate,
        Integer duration,
        List<CapabilityReportDTO> capabilities
) {
    public record CapabilityReportDTO(
            Long id,
            @JsonProperty("nombre") String nombre,
            @JsonProperty("tecnologias") List<TechnologyReportDTO> tecnologias
    ) {
    }

    public record TechnologyReportDTO(
            Long id,
            String name
    ) {
    }
}
