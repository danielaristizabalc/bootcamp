package com.example.resilient_api.domain.usecase;

import com.example.resilient_api.domain.api.BootcampValidateServicePort;
import com.example.resilient_api.domain.model.BootcampBasicInfo;
import com.example.resilient_api.domain.model.BootcampValidationResult;
import com.example.resilient_api.domain.spi.BootcampPersistencePort;
import reactor.core.publisher.Mono;

import java.util.List;

public class BootcampValidateUseCase implements BootcampValidateServicePort {

    private final BootcampPersistencePort bootcampPersistencePort;

    public BootcampValidateUseCase(BootcampPersistencePort bootcampPersistencePort) {
        this.bootcampPersistencePort = bootcampPersistencePort;
    }

    @Override
    public Mono<BootcampValidationResult> validateBootcamps(List<Long> bootcampIds, String messageId) {
        if (bootcampIds == null || bootcampIds.isEmpty()) {
            return Mono.just(new BootcampValidationResult(List.of()));
        }

        return bootcampPersistencePort.findBootcampsByIds(bootcampIds)
                .map(BootcampValidationResult::new);
    }
}