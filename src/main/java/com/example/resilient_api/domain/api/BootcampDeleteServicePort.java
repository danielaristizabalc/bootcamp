package com.example.resilient_api.domain.api;

import reactor.core.publisher.Mono;

public interface BootcampDeleteServicePort {
    Mono<Void> deleteBootcamp(Long bootcampId, String messageId);
}