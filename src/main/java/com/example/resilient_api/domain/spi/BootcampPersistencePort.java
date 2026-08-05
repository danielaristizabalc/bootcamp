package com.example.resilient_api.domain.spi;

import com.example.resilient_api.domain.model.Bootcamp;
import com.example.resilient_api.domain.model.BootcampBasicInfo;
import reactor.core.publisher.Mono;

import java.util.List;

public interface BootcampPersistencePort {
    Mono<Bootcamp> save(Bootcamp bootcamp);
    Mono<Boolean> existsByName(String name);
    Mono<Boolean> existsById(Long bootcampId);
    Mono<java.util.List<Long>> findExclusiveCapabilityIdsByBootcampId(Long bootcampId);
    Mono<Void> deleteBootcampById(Long bootcampId);
    Mono<List<BootcampBasicInfo>> findBootcampsByIds(List<Long> bootcampIds);
    Mono<Boolean> hasSharedCapabilities(Long bootcampId);
}
