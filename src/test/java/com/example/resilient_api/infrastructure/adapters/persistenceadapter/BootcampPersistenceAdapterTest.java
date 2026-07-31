package com.example.resilient_api.infrastructure.adapters.persistenceadapter;

import com.example.resilient_api.domain.enums.TechnicalMessage;
import com.example.resilient_api.domain.exceptions.BusinessException;
import com.example.resilient_api.domain.model.Bootcamp;
import com.example.resilient_api.domain.model.BootcampBasicInfo;
import com.example.resilient_api.infrastructure.adapters.persistenceadapter.entity.BootcampCapabilityEntity;
import com.example.resilient_api.infrastructure.adapters.persistenceadapter.entity.BootcampEntity;
import com.example.resilient_api.infrastructure.adapters.persistenceadapter.mapper.BootcampEntityMapper;
import com.example.resilient_api.infrastructure.adapters.persistenceadapter.repository.BootcampCapabilityRepository;
import com.example.resilient_api.infrastructure.adapters.persistenceadapter.repository.BootcampRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BootcampPersistenceAdapterTest {

    private static final Bootcamp VALID_BOOTCAMP = new Bootcamp(
            1L,
            "Bootcamp Java",
            "Bootcamp de backend",
            LocalDate.of(2026, Month.JULY, 31),
            120,
            List.of(10L, 11L)
    );

    private static final Bootcamp BOOTCAMP_WITHOUT_CAPABILITIES = new Bootcamp(
            2L,
            "Bootcamp Cloud",
            "Bootcamp de cloud",
            LocalDate.of(2026, Month.AUGUST, 1),
            90,
            List.of()
    );

    @Mock
    private BootcampRepository bootcampRepository;

    @Mock
    private BootcampCapabilityRepository bootcampCapabilityRepository;

    @Mock
    private BootcampEntityMapper bootcampEntityMapper;

    @Mock
    private TransactionalOperator transactionalOperator;

    private BootcampPersistenceAdapter bootcampPersistenceAdapter;

    @BeforeEach
    void setUp() {
        bootcampPersistenceAdapter = new BootcampPersistenceAdapter(
                bootcampRepository,
                bootcampCapabilityRepository,
                bootcampEntityMapper,
                transactionalOperator
        );
    }

    @Test
    void debeGuardarBootcampYCapacidadesCuandoLaPeticionEsValida() {
        // Given
        BootcampEntity bootcampEntity = buildBootcampEntity(VALID_BOOTCAMP);
        BootcampEntity savedBootcampEntity = buildSavedBootcampEntity(VALID_BOOTCAMP);

                stubTransactionalMono();

        when(bootcampEntityMapper.toEntity(VALID_BOOTCAMP)).thenReturn(bootcampEntity);
        when(bootcampCapabilityRepository.findAssignedCapabilityIds(VALID_BOOTCAMP.capabilityIds())).thenReturn(Flux.empty());
        when(bootcampRepository.save(bootcampEntity)).thenReturn(Mono.just(savedBootcampEntity));
        when(bootcampCapabilityRepository.saveAll(anyList())).thenReturn(Flux.empty());

        // When
        Mono<Bootcamp> responseMono = bootcampPersistenceAdapter.save(VALID_BOOTCAMP);

        // Then
        StepVerifier.create(responseMono)
                .expectNextMatches(savedBootcamp -> {
                    assertThat(savedBootcamp.id()).isEqualTo(VALID_BOOTCAMP.id());
                    assertThat(savedBootcamp.name()).isEqualTo(VALID_BOOTCAMP.name());
                    assertThat(savedBootcamp.description()).isEqualTo(VALID_BOOTCAMP.description());
                    assertThat(savedBootcamp.releaseDate()).isEqualTo(VALID_BOOTCAMP.releaseDate());
                    assertThat(savedBootcamp.duration()).isEqualTo(VALID_BOOTCAMP.duration());
                    assertThat(savedBootcamp.capabilityIds()).containsExactly(10L, 11L);
                    return true;
                })
                .verifyComplete();

        verify(bootcampEntityMapper).toEntity(VALID_BOOTCAMP);
        verify(bootcampCapabilityRepository).findAssignedCapabilityIds(VALID_BOOTCAMP.capabilityIds());
        verify(bootcampRepository).save(bootcampEntity);

        ArgumentCaptor<List<BootcampCapabilityEntity>> capabilityEntitiesCaptor = ArgumentCaptor.forClass(List.class);
        verify(bootcampCapabilityRepository).saveAll(capabilityEntitiesCaptor.capture());
        assertThat(capabilityEntitiesCaptor.getValue()).hasSize(2);
        assertThat(capabilityEntitiesCaptor.getValue())
                .extracting(BootcampCapabilityEntity::getBootcampId, BootcampCapabilityEntity::getCapabilityId)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(VALID_BOOTCAMP.id(), 10L),
                        org.assertj.core.groups.Tuple.tuple(VALID_BOOTCAMP.id(), 11L)
                );
    }

    @Test
    void debeGuardarBootcampSinCapacidadesCuandoLaListaVieneVacia() {
        // Given
        BootcampEntity bootcampEntity = buildBootcampEntity(BOOTCAMP_WITHOUT_CAPABILITIES);
        BootcampEntity savedBootcampEntity = buildSavedBootcampEntity(BOOTCAMP_WITHOUT_CAPABILITIES);

                stubTransactionalMono();

        when(bootcampEntityMapper.toEntity(BOOTCAMP_WITHOUT_CAPABILITIES)).thenReturn(bootcampEntity);
        when(bootcampRepository.save(bootcampEntity)).thenReturn(Mono.just(savedBootcampEntity));

        // When
        Mono<Bootcamp> responseMono = bootcampPersistenceAdapter.save(BOOTCAMP_WITHOUT_CAPABILITIES);

        // Then
        StepVerifier.create(responseMono)
                .assertNext(savedBootcamp -> {
                    assertThat(savedBootcamp.id()).isEqualTo(BOOTCAMP_WITHOUT_CAPABILITIES.id());
                    assertThat(savedBootcamp.capabilityIds()).isEmpty();
                })
                .verifyComplete();

        verify(bootcampEntityMapper).toEntity(BOOTCAMP_WITHOUT_CAPABILITIES);
        verify(bootcampRepository).save(bootcampEntity);
        verifyNoInteractions(bootcampCapabilityRepository);
    }

    @Test
    void debeLanzarBusinessExceptionCuandoExistenCapacidadesAsignadas() {
        // Given
        stubTransactionalMono();

        when(bootcampCapabilityRepository.findAssignedCapabilityIds(VALID_BOOTCAMP.capabilityIds()))
                .thenReturn(Flux.just(10L));

        // When
        Mono<Bootcamp> responseMono = bootcampPersistenceAdapter.save(VALID_BOOTCAMP);

        // Then
        StepVerifier.create(responseMono)
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(BusinessException.class);
                    BusinessException businessException = (BusinessException) error;
                    assertThat(businessException.getTechnicalMessage()).isEqualTo(TechnicalMessage.CAPABILITIES_ALREADY_ASSIGNED);
                })
                .verify();

        verify(bootcampCapabilityRepository).findAssignedCapabilityIds(VALID_BOOTCAMP.capabilityIds());
        verifyNoInteractions(bootcampEntityMapper);
        verify(bootcampRepository, never()).save(any());
        verify(bootcampCapabilityRepository, never()).saveAll(anyList());
    }

    @Test
    void debeRetornarTrueCuandoElBootcampExistePorNombre() {
        // Given
        when(bootcampRepository.findByName(VALID_BOOTCAMP.name())).thenReturn(Mono.just(buildSavedBootcampEntity(VALID_BOOTCAMP)));

        // When
        Mono<Boolean> responseMono = bootcampPersistenceAdapter.existsByName(VALID_BOOTCAMP.name());

        // Then
        StepVerifier.create(responseMono)
                .expectNext(true)
                .verifyComplete();

        verify(bootcampRepository).findByName(VALID_BOOTCAMP.name());
    }

    @Test
    void debeRetornarFalseCuandoElBootcampNoExistePorNombre() {
        // Given
        when(bootcampRepository.findByName(VALID_BOOTCAMP.name())).thenReturn(Mono.empty());

        // When
        Mono<Boolean> responseMono = bootcampPersistenceAdapter.existsByName(VALID_BOOTCAMP.name());

        // Then
        StepVerifier.create(responseMono)
                .expectNext(false)
                .verifyComplete();

        verify(bootcampRepository).findByName(VALID_BOOTCAMP.name());
    }

    @Test
    void debeRetornarTrueCuandoElBootcampExistePorId() {
        // Given
        when(bootcampRepository.findById(VALID_BOOTCAMP.id())).thenReturn(Mono.just(buildSavedBootcampEntity(VALID_BOOTCAMP)));

        // When
        Mono<Boolean> responseMono = bootcampPersistenceAdapter.existsById(VALID_BOOTCAMP.id());

        // Then
        StepVerifier.create(responseMono)
                .expectNext(true)
                .verifyComplete();

        verify(bootcampRepository).findById(VALID_BOOTCAMP.id());
    }

    @Test
    void debeRetornarFalseCuandoElBootcampNoExistePorId() {
        // Given
        when(bootcampRepository.findById(VALID_BOOTCAMP.id())).thenReturn(Mono.empty());

        // When
        Mono<Boolean> responseMono = bootcampPersistenceAdapter.existsById(VALID_BOOTCAMP.id());

        // Then
        StepVerifier.create(responseMono)
                .expectNext(false)
                .verifyComplete();

        verify(bootcampRepository).findById(VALID_BOOTCAMP.id());
    }

    @Test
    void debeRetornarIdsExclusivosDeCapacidadesCuandoExistenRelacionadas() {
        // Given
        when(bootcampCapabilityRepository.findExclusiveCapabilityIdsByBootcampId(VALID_BOOTCAMP.id()))
                .thenReturn(Flux.just(10L, 11L));

        // When
        Mono<List<Long>> responseMono = bootcampPersistenceAdapter.findExclusiveCapabilityIdsByBootcampId(VALID_BOOTCAMP.id());

        // Then
        StepVerifier.create(responseMono)
                .assertNext(ids -> assertThat(ids).containsExactly(10L, 11L))
                .verifyComplete();

        verify(bootcampCapabilityRepository).findExclusiveCapabilityIdsByBootcampId(VALID_BOOTCAMP.id());
    }

    @Test
    void debeEliminarBootcampYCapacidadesCuandoSeSolicitaElBorrado() {
        // Given
                stubTransactionalMono();

        when(bootcampCapabilityRepository.deleteByBootcampId(VALID_BOOTCAMP.id())).thenReturn(Mono.empty());
        when(bootcampRepository.deleteById(VALID_BOOTCAMP.id())).thenReturn(Mono.empty());

        // When
        Mono<Void> responseMono = bootcampPersistenceAdapter.deleteBootcampById(VALID_BOOTCAMP.id());

        // Then
        StepVerifier.create(responseMono)
                .verifyComplete();

        verify(bootcampCapabilityRepository).deleteByBootcampId(VALID_BOOTCAMP.id());
        verify(bootcampRepository).deleteById(VALID_BOOTCAMP.id());
    }

    @Test
    void debeMapearBootcampsADominioCuandoBuscaPorIds() {
        // Given
        BootcampEntity firstEntity = buildSavedBootcampEntity(VALID_BOOTCAMP);
        BootcampEntity secondEntity = buildSavedBootcampEntity(BOOTCAMP_WITHOUT_CAPABILITIES);

        when(bootcampRepository.findAllById(List.of(VALID_BOOTCAMP.id(), BOOTCAMP_WITHOUT_CAPABILITIES.id())))
                .thenReturn(Flux.just(firstEntity, secondEntity));

        // When
        Mono<List<BootcampBasicInfo>> responseMono = bootcampPersistenceAdapter.findBootcampsByIds(
                List.of(VALID_BOOTCAMP.id(), BOOTCAMP_WITHOUT_CAPABILITIES.id())
        );

        // Then
        StepVerifier.create(responseMono)
                .assertNext(result -> {
                    assertThat(result).hasSize(2);
                    assertThat(result.get(0).id()).isEqualTo(VALID_BOOTCAMP.id());
                    assertThat(result.get(0).name()).isEqualTo(VALID_BOOTCAMP.name());
                    assertThat(result.get(0).releaseDate()).isEqualTo(VALID_BOOTCAMP.releaseDate());
                    assertThat(result.get(0).duration()).isEqualTo(VALID_BOOTCAMP.duration());
                    assertThat(result.get(1).id()).isEqualTo(BOOTCAMP_WITHOUT_CAPABILITIES.id());
                    assertThat(result.get(1).name()).isEqualTo(BOOTCAMP_WITHOUT_CAPABILITIES.name());
                })
                .verifyComplete();

        verify(bootcampRepository).findAllById(List.of(VALID_BOOTCAMP.id(), BOOTCAMP_WITHOUT_CAPABILITIES.id()));
    }

    private BootcampEntity buildBootcampEntity(Bootcamp bootcamp) {
        BootcampEntity entity = new BootcampEntity();
        entity.setId(bootcamp.id());
        entity.setName(bootcamp.name());
        entity.setDescription(bootcamp.description());
        entity.setReleaseDate(bootcamp.releaseDate());
        entity.setDuration(bootcamp.duration());
        return entity;
    }

    private BootcampEntity buildSavedBootcampEntity(Bootcamp bootcamp) {
        BootcampEntity entity = buildBootcampEntity(bootcamp);
        entity.setId(bootcamp.id());
        return entity;
    }

        private void stubTransactionalMono() {
                when(transactionalOperator.transactional(org.mockito.ArgumentMatchers.<Mono<?>>any()))
                                .thenAnswer(invocation -> invocation.getArgument(0));
        }
}