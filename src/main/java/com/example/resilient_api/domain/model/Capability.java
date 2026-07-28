package com.example.resilient_api.domain.model;

import java.util.List;

public record Capability(
        Long id,
        String name,
        List<Technology> technologies
) {
}