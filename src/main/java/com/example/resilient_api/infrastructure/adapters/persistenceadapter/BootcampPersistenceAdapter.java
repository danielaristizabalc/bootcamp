package com.example.resilient_api.infrastructure.adapters.persistenceadapter;

import com.example.resilient_api.domain.enums.TechnicalMessage;
import com.example.resilient_api.domain.exceptions.BusinessException;
import com.example.resilient_api.domain.model.Bootcamp;
import com.example.resilient_api.domain.model.BootcampBasicInfo;
import com.example.resilient_api.domain.spi.BootcampPersistencePort;
import com.example.resilient_api.infrastructure.adapters.persistenceadapter.entity.BootcampCapabilityEntity;
import com.example.resilient_api.infrastructure.adapters.persistenceadapter.entity.BootcampEntity;
import com.example.resilient_api.infrastructure.adapters.persistenceadapter.mapper.BootcampEntityMapper;
import com.example.resilient_api.infrastructure.adapters.persistenceadapter.repository.BootcampCapabilityRepository;
import com.example.resilient_api.infrastructure.adapters.persistenceadapter.repository.BootcampRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@RequiredArgsConstructor
public class BootcampPersistenceAdapter implements BootcampPersistencePort {

    private final BootcampRepository bootcampRepository;
    private final BootcampCapabilityRepository bootcampCapabilityRepository;
    private final BootcampEntityMapper bootcampEntityMapper;
    private final TransactionalOperator transactionalOperator;


    @Override
    public Mono<Bootcamp> save(Bootcamp bootcamp) {
        return validateCapabilitiesAreNotAssigned(bootcamp.capabilityIds())
                .then(Mono.defer(() -> bootcampRepository.save(bootcampEntityMapper.toEntity(bootcamp))))
                .flatMap(savedBootcamp -> {
                    if (bootcamp.capabilityIds() != null && !bootcamp.capabilityIds().isEmpty()) {
                        return saveBootcampCapabilities(savedBootcamp.getId(), bootcamp.capabilityIds())
                                .thenReturn(buildBootcampWithCapabilities(savedBootcamp, bootcamp.capabilityIds()));
                    }
                    return Mono.just(buildBootcampWithCapabilities(savedBootcamp, bootcamp.capabilityIds()));
                }).as(transactionalOperator::transactional);
    }

    @Override
    public Mono<Boolean> existsByName(String name) {
        return bootcampRepository.findByName(name)
                .map(bootcamp -> true)
                .defaultIfEmpty(false);
    }

    @Override
    public Mono<Boolean> existsById(Long bootcampId) {
        return bootcampRepository.findById(bootcampId)
                .map(bootcamp -> true)
                .defaultIfEmpty(false);
    }

    @Override
    public Mono<List<Long>> findExclusiveCapabilityIdsByBootcampId(Long bootcampId) {
        return bootcampCapabilityRepository.findExclusiveCapabilityIdsByBootcampId(bootcampId)
                .collectList();
    }

    @Override
    public Mono<Void> deleteBootcampById(Long bootcampId) {
        return bootcampCapabilityRepository.deleteByBootcampId(bootcampId)
                .then(bootcampRepository.deleteById(bootcampId))
                .as(transactionalOperator::transactional);
    }

    @Override
    public Mono<List<BootcampBasicInfo>> findBootcampsByIds(List<Long> bootcampIds) {
        return bootcampRepository.findAllById(bootcampIds)
                .map(bootcamp -> new BootcampBasicInfo(
                        bootcamp.getId(), bootcamp.getName(), bootcamp.getReleaseDate(), bootcamp.getDuration()))
                .collectList();
    }

    private Mono<Void> validateCapabilitiesAreNotAssigned(List<Long> capabilityIds) {
        if (capabilityIds == null || capabilityIds.isEmpty()) {
            return Mono.empty();
        }

        return bootcampCapabilityRepository.findAssignedCapabilityIds(capabilityIds)
                .collectList()
                .flatMap(conflictingCapabilities -> conflictingCapabilities.isEmpty()
                        ? Mono.empty()
                        : Mono.error(new BusinessException(TechnicalMessage.CAPABILITIES_ALREADY_ASSIGNED)));
    }

    private Mono<Void> saveBootcampCapabilities(Long bootcampId, List<Long> capabilityIds) {
        List<BootcampCapabilityEntity> entities = capabilityIds.stream()
                .map(capabilityId -> {
                    BootcampCapabilityEntity entity = new BootcampCapabilityEntity();
                    entity.setBootcampId(bootcampId);
                    entity.setCapabilityId(capabilityId);
                    entity.setCreatedAt(LocalDateTime.now(ZoneId.systemDefault()));
                    return entity;
                })
                .toList();

        return bootcampCapabilityRepository.saveAll(entities).then();
    }

    private Bootcamp buildBootcampWithCapabilities(BootcampEntity entity, List<Long> capabilityIds) {
        return new Bootcamp(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getReleaseDate(),
                entity.getDuration(),
                capabilityIds
        );
    }

}
