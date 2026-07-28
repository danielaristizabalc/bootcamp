package com.example.resilient_api.domain.usecase;

import com.example.resilient_api.domain.api.BootcampServicePort;
import com.example.resilient_api.domain.enums.TechnicalMessage;
import com.example.resilient_api.domain.exceptions.BusinessException;
import com.example.resilient_api.domain.model.Bootcamp;
import com.example.resilient_api.domain.spi.BootcampPersistencePort;
import com.example.resilient_api.domain.spi.CapabilityGateway;
import reactor.core.publisher.Mono;

import java.util.List;

public class BootcampUseCase implements BootcampServicePort {

    private final BootcampPersistencePort bootcampPersistencePort;
    private final CapabilityGateway capabilityGateway;
    
    private static final int MIN_CAPABILITIES = 1;
    private static final int MAX_CAPABILITIES = 4;

    public BootcampUseCase(BootcampPersistencePort bootcampPersistencePort, 
                           CapabilityGateway capabilityGateway) {
        this.bootcampPersistencePort = bootcampPersistencePort;
        this.capabilityGateway = capabilityGateway;
    }

    @Override
    public Mono<Bootcamp> registerBootcamp(Bootcamp bootcamp, String messageId) {
        return validateCapabilitiesCount(bootcamp)
                .flatMap(validBootcamp -> bootcampPersistencePort.existsByName(validBootcamp.name()))
                .filter(exists -> !exists)
                .switchIfEmpty(Mono.error(new BusinessException(TechnicalMessage.BOOTCAMP_ALREADY_EXISTS)))
                .flatMap(exists -> validateCapabilitiesExist(bootcamp.capabilityIds(), messageId))
                .flatMap(validatedIds -> bootcampPersistencePort.save(bootcamp));
    }

    private Mono<Bootcamp> validateCapabilitiesCount(Bootcamp bootcamp) {
        return Mono.just(bootcamp)
                .filter(b -> b.capabilityIds() != null 
                        && b.capabilityIds().size() >= MIN_CAPABILITIES 
                        && b.capabilityIds().size() <= MAX_CAPABILITIES)
                .switchIfEmpty(Mono.error(new BusinessException(TechnicalMessage.INVALID_CAPABILITIES_COUNT)));
    }

    private Mono<List<Long>> validateCapabilitiesExist(List<Long> capabilityIds, String messageId) {
        return capabilityGateway.validateCapabilities(capabilityIds, messageId);
    }
}
