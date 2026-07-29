package com.example.resilient_api.domain.api;

import com.example.resilient_api.domain.model.BootcampValidationResult;
import reactor.core.publisher.Mono;

import java.util.List;

public interface BootcampValidateServicePort {
    Mono<BootcampValidationResult> validateBootcamps(List<Long> bootcampIds, String messageId);
}