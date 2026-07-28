package com.example.resilient_api.domain.spi;

import com.example.resilient_api.domain.model.Bootcamp;
import reactor.core.publisher.Mono;

public interface BootcampPersistencePort {
    Mono<Bootcamp> save(Bootcamp bootcamp);
    Mono<Boolean> existsByName(String name);
}
