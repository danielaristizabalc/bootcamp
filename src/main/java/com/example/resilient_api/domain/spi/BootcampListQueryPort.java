package com.example.resilient_api.domain.spi;

import com.example.resilient_api.domain.model.BootcampListCriteria;
import com.example.resilient_api.domain.model.BootcampPageResult;
import reactor.core.publisher.Mono;

public interface BootcampListQueryPort {
    Mono<BootcampPageResult> listBootcamps(BootcampListCriteria criteria);
}