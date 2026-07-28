package com.example.resilient_api.infrastructure.adapters.persistenceadapter;

import com.example.resilient_api.domain.enums.TechnicalMessage;
import com.example.resilient_api.domain.exceptions.BusinessException;
import com.example.resilient_api.domain.model.BootcampListCriteria;
import com.example.resilient_api.domain.model.BootcampPageResult;
import com.example.resilient_api.domain.model.BootcampSummary;
import com.example.resilient_api.domain.model.Bootcamp;
import com.example.resilient_api.domain.spi.BootcampPersistencePort;
import com.example.resilient_api.infrastructure.adapters.persistenceadapter.entity.BootcampCapabilityEntity;
import com.example.resilient_api.infrastructure.adapters.persistenceadapter.entity.BootcampEntity;
import com.example.resilient_api.infrastructure.adapters.persistenceadapter.mapper.BootcampEntityMapper;
import com.example.resilient_api.infrastructure.adapters.persistenceadapter.repository.BootcampCapabilityRepository;
import com.example.resilient_api.infrastructure.adapters.persistenceadapter.repository.BootcampRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class BootcampPersistenceAdapter implements BootcampPersistencePort {

    private static final String SORT_COLUMN_NAME = "b.name";
    private static final String SORT_COLUMN_CAPABILITY_COUNT = "capability_count";

    private final BootcampRepository bootcampRepository;
    private final BootcampCapabilityRepository bootcampCapabilityRepository;
    private final BootcampEntityMapper bootcampEntityMapper;
    private final DatabaseClient databaseClient;
    private final TransactionalOperator transactionalOperator;


    @Override
    public Mono<Bootcamp> save(Bootcamp bootcamp) {
        return validateCapabilitiesAreNotAssigned(bootcamp.capabilityIds())
            .then(bootcampRepository.save(bootcampEntityMapper.toEntity(bootcamp)))
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
    public Mono<BootcampPageResult> listBootcamps(BootcampListCriteria criteria) {
        int page = criteria.page() == null || criteria.page() < 0 ? 0 : criteria.page();
        int size = criteria.size() == null || criteria.size() <= 0 ? 10 : criteria.size();
        String sortBy = normalizeSortBy(criteria.sortBy());
        String sortDirection = normalizeSortDirection(criteria.sortDirection());
        int offset = page * size;

        Mono<List<BootcampRow>> rowsMono = databaseClient.sql(buildBootcampPageQuery(sortBy, sortDirection))
                .bind("limit", size)
                .bind("offset", offset)
                .map((row, metadata) -> new BootcampRow(
                        row.get("id", Long.class),
                        row.get("name", String.class),
                        row.get("description", String.class),
                        row.get("release_date", java.time.LocalDate.class),
                        row.get("duration", Integer.class),
                    row.get(SORT_COLUMN_CAPABILITY_COUNT, Long.class)
                ))
                .all()
                .collectList();

        Mono<Long> totalElementsMono = databaseClient.sql("SELECT COUNT(*) AS total_elements FROM bootcamps")
                .map((row, metadata) -> row.get("total_elements", Long.class))
                .one();

        return Mono.zip(rowsMono, totalElementsMono)
                .flatMap(tuple -> loadCapabilityIds(tuple.getT1())
                        .map(capabilityIdsByBootcamp -> tuple.getT1().stream()
                                .map(row -> new BootcampSummary(
                                        row.id(),
                                        row.name(),
                                        row.description(),
                                        row.releaseDate(),
                                        row.duration(),
                                        row.capabilityCount(),
                                        capabilityIdsByBootcamp.getOrDefault(row.id(), List.of())
                                ))
                                .toList())
                        .map(content -> new BootcampPageResult(
                                content,
                                page,
                                size,
                                tuple.getT2(),
                                totalPages(tuple.getT2(), size)
                        )));
    }

    private Mono<Map<Long, List<Long>>> loadCapabilityIds(List<BootcampRow> rows) {
        if (rows.isEmpty()) {
            return Mono.just(Map.<Long, List<Long>>of());
        }

        return Flux.fromIterable(rows)
                .flatMap(row -> bootcampCapabilityRepository.findByBootcampId(row.id())
                        .map(BootcampCapabilityEntity::getCapabilityId)
                        .collectList()
                        .map(ids -> Map.entry(row.id(), ids)))
                .collectMap(Map.Entry::getKey, Map.Entry::getValue);
    }

    private String buildBootcampPageQuery(String sortBy, String sortDirection) {
        return """
                SELECT b.id,
                       b.name,
                       b.description,
                       b.release_date,
                       b.duration,
                       COUNT(bc.capability_id) AS capability_count
                FROM bootcamps b
                LEFT JOIN bootcamp_capabilities bc ON bc.bootcamp_id = b.id
                GROUP BY b.id, b.name, b.description, b.release_date, b.duration
                ORDER BY %s %s, b.id ASC
                LIMIT :limit OFFSET :offset
                """.formatted(sortBy, sortDirection);
    }

    private String normalizeSortBy(String sortBy) {
        if (sortBy == null) {
            return SORT_COLUMN_NAME;
        }

        return switch (sortBy.toLowerCase()) {
            case "name" -> SORT_COLUMN_NAME;
            case "capabilities", "capabilitycount", "capabilitiescount" -> SORT_COLUMN_CAPABILITY_COUNT;
            default -> SORT_COLUMN_NAME;
        };
    }

    private String normalizeSortDirection(String sortDirection) {
        return "desc".equalsIgnoreCase(sortDirection) ? "DESC" : "ASC";
    }

    private Integer totalPages(Long totalElements, Integer size) {
        if (totalElements == null || totalElements == 0L) {
            return 0;
        }

        return (int) Math.ceil((double) totalElements / size);
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

    private record BootcampRow(
            Long id,
            String name,
            String description,
            java.time.LocalDate releaseDate,
            Integer duration,
            Long capabilityCount
    ) {
    }
}
