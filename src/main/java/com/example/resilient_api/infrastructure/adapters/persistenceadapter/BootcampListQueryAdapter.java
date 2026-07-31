package com.example.resilient_api.infrastructure.adapters.persistenceadapter;

import com.example.resilient_api.domain.model.BootcampListCriteria;
import com.example.resilient_api.domain.model.BootcampPageResult;
import com.example.resilient_api.domain.model.BootcampSummary;
import com.example.resilient_api.domain.spi.BootcampListQueryPort;
import com.example.resilient_api.infrastructure.adapters.persistenceadapter.entity.BootcampCapabilityEntity;
import com.example.resilient_api.infrastructure.adapters.persistenceadapter.repository.BootcampCapabilityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class BootcampListQueryAdapter implements BootcampListQueryPort {

    private final DatabaseClient databaseClient;
    private final BootcampCapabilityRepository bootcampCapabilityRepository;
    private final BootcampListQueryBuilder bootcampListQueryBuilder;

    public Mono<BootcampPageResult> listBootcamps(BootcampListCriteria criteria) {
        BootcampListQueryBuilder.BootcampListQuery query = bootcampListQueryBuilder.resolve(criteria);

        Mono<List<BootcampRow>> rowsMono = databaseClient.sql(bootcampListQueryBuilder.buildQuery(query))
                .bind("limit", query.size())
                .bind("offset", query.offset())
                .map((row, metadata) -> new BootcampRow(
                        row.get("id", Long.class),
                        row.get("name", String.class),
                        row.get("description", String.class),
                        row.get("release_date", java.time.LocalDate.class),
                        row.get("duration", Integer.class),
                        row.get(BootcampListQueryBuilder.SORT_COLUMN_CAPABILITY_COUNT, Long.class)
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
                                query.page(),
                                query.size(),
                                tuple.getT2(),
                                bootcampListQueryBuilder.totalPages(tuple.getT2(), query.size())
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