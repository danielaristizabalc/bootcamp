package com.example.resilient_api.domain.usecase;

import com.example.resilient_api.domain.api.BootcampDeleteServicePort;
import com.example.resilient_api.domain.enums.TechnicalMessage;
import com.example.resilient_api.domain.exceptions.BusinessException;
import com.example.resilient_api.domain.spi.BootcampPersistencePort;
import com.example.resilient_api.domain.spi.CapabilityGateway;
import reactor.core.publisher.Mono;

import java.util.List;

public class BootcampDeleteUseCase implements BootcampDeleteServicePort {

    private final BootcampPersistencePort bootcampPersistencePort;
    private final CapabilityGateway capabilityGateway;

    public BootcampDeleteUseCase(BootcampPersistencePort bootcampPersistencePort,
                                 CapabilityGateway capabilityGateway) {
        this.bootcampPersistencePort = bootcampPersistencePort;
        this.capabilityGateway = capabilityGateway;
    }

    @Override
    public Mono<Void> deleteBootcamp(Long bootcampId, String messageId) {
        return bootcampPersistencePort.existsById(bootcampId)
                .filter(Boolean::booleanValue)
                .switchIfEmpty(Mono.error(new BusinessException(TechnicalMessage.BOOTCAMP_NOT_FOUND)))
                .then(bootcampPersistencePort.findExclusiveCapabilityIdsByBootcampId(bootcampId))
                .flatMap(exclusiveCapabilityIds -> deleteCapabilitiesIfNeeded(exclusiveCapabilityIds, messageId)
                        .then(bootcampPersistencePort.deleteBootcampById(bootcampId)))
                .then();
    }

    private Mono<Void> deleteCapabilitiesIfNeeded(List<Long> capabilityIds, String messageId) {
        if (capabilityIds == null || capabilityIds.isEmpty()) {
            return Mono.empty();
        }

        return capabilityGateway.deleteCapabilitiesByIds(capabilityIds, messageId);
    }
}