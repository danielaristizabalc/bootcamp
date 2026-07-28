package com.example.resilient_api.domain.api;

import com.example.resilient_api.domain.model.BootcampListCriteria;
import com.example.resilient_api.domain.model.BootcampListResult;
import reactor.core.publisher.Mono;

public interface BootcampListServicePort {
    Mono<BootcampListResult> listBootcamps(BootcampListCriteria criteria, String messageId);
}