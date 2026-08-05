package com.example.resilient_api.infrastructure.adapters.persistenceadapter.repository;

import com.example.resilient_api.infrastructure.adapters.persistenceadapter.entity.BootcampCapabilityEntity;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Repository
public interface BootcampCapabilityRepository extends R2dbcRepository<BootcampCapabilityEntity, Long> {
    Flux<BootcampCapabilityEntity> findByBootcampId(Long bootcampId);

    @Query("""
            SELECT capability_id
            FROM bootcamp_capabilities
            WHERE bootcamp_id = :bootcampId
            GROUP BY capability_id
            HAVING COUNT(*) = 1
            """)
    Flux<Long> findExclusiveCapabilityIdsByBootcampId(@Param("bootcampId") Long bootcampId);

    @Query("""
            SELECT DISTINCT capability_id
            FROM bootcamp_capabilities
            WHERE capability_id IN (:capabilityIds)
            """)
    Flux<Long> findAssignedCapabilityIds(@Param("capabilityIds") List<Long> capabilityIds);

    Mono<Void> deleteByBootcampId(Long bootcampId);

    @Query("""
        SELECT EXISTS (
            SELECT 1 
            FROM bootcamp_capabilities bc1
            INNER JOIN bootcamp_capabilities bc2 ON bc1.capability_id = bc2.capability_id
            WHERE bc1.bootcamp_id = :bootcampId 
              AND bc2.bootcamp_id <> :bootcampId
        )
    """)
    Mono<Boolean> hasSharedCapabilities(Long bootcampId);
}
