package com.example.resilient_api.domain.model;

import java.util.List;

public record BootcampValidationResult(
        List<BootcampBasicInfo> bootcamps
) {
}