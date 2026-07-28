package com.example.resilient_api.domain.api;

import com.example.resilient_api.domain.model.Bootcamp;
import reactor.core.publisher.Mono;

public interface BootcampServicePort {
    Mono<Bootcamp> registerBootcamp(Bootcamp bootcamp, String messageId);
}
