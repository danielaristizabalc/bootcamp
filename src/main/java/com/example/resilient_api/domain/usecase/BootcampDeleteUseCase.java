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
                .then(Mono.defer(() -> bootcampPersistencePort.hasSharedCapabilities(bootcampId)))
                .flatMap(isValid -> isValid
                        ? bootcampPersistencePort.deleteBootcampById(bootcampId)
                        : bootcampPersistencePort.findExclusiveCapabilityIdsByBootcampId(bootcampId)
                        .flatMap(exclusiveCapabilityIds -> deleteCapabilitiesIfNeeded(exclusiveCapabilityIds, messageId, bootcampId)
                                .then(Mono.defer(() -> bootcampPersistencePort.deleteBootcampById(bootcampId))))
                        .then()
                );

    }

    private Mono<Void> deleteCapabilitiesIfNeeded(List<Long> capabilityIds, String messageId, Long bootcampId) {
        if (capabilityIds == null || capabilityIds.isEmpty()) {
            return Mono.empty();
        }

        return capabilityGateway.deleteCapabilitiesByIds(capabilityIds, messageId)
                .onErrorResume(BusinessException.class, ex -> bootcampPersistencePort.deleteBootcampById(bootcampId)
                        .doOnNext(o -> System.out.println("Error de borrado de capacidades" + ex.getMessage()))
                );
    }
}