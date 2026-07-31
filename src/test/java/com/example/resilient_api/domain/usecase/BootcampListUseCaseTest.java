package com.example.resilient_api.domain.usecase;

import com.example.resilient_api.domain.model.BootcampListCriteria;
import com.example.resilient_api.domain.model.BootcampListItem;
import com.example.resilient_api.domain.model.BootcampListResult;
import com.example.resilient_api.domain.model.BootcampPageResult;
import com.example.resilient_api.domain.model.BootcampSummary;
import com.example.resilient_api.domain.model.Capability;
import com.example.resilient_api.domain.model.Technology;
import com.example.resilient_api.domain.spi.BootcampPersistencePort;
import com.example.resilient_api.domain.spi.CapabilityGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BootcampListUseCaseTest {

    private static final String MESSAGE_ID = "message-id-123";
        private static final String BOOTCAMP_NAME = "Bootcamp Java";
        private static final String BOOTCAMP_DESCRIPTION = "Backend con Spring";
    private static final BootcampListCriteria DEFAULT_CRITERIA = new BootcampListCriteria(0, 10, "name", "asc");

    @Mock
    private BootcampPersistencePort bootcampPersistencePort;

    @Mock
    private CapabilityGateway capabilityGateway;

    private BootcampListUseCase bootcampListUseCase;

    @BeforeEach
    void setUp() {
        bootcampListUseCase = new BootcampListUseCase(bootcampPersistencePort, capabilityGateway);
    }

    @Test
    void debeListarBootcampsEnriquecidosCuandoExistenCapacidades() {
        // Given
        BootcampSummary bootcampOne = new BootcampSummary(
                1L,
                BOOTCAMP_NAME,
                BOOTCAMP_DESCRIPTION,
                LocalDate.of(2026, Month.JULY, 1),
                120,
                2L,
                List.of(1L, 2L)
        );
        BootcampSummary bootcampTwo = new BootcampSummary(
                2L,
                "Bootcamp Cloud",
                "Arquitectura y despliegue",
                LocalDate.of(2026, Month.JULY, 15),
                90,
                2L,
                List.of(2L, 3L)
        );
        List<Capability> capabilities = List.of(
                new Capability(1L, "Java", List.of(new Technology(11L, "Spring Boot"))),
                new Capability(2L, "SQL", List.of(new Technology(12L, "PostgreSQL"))),
                new Capability(3L, "Cloud", List.of(new Technology(13L, "AWS")))
        );
        BootcampPageResult pageResult = new BootcampPageResult(List.of(bootcampOne, bootcampTwo), 0, 10, 2L, 1);

        when(bootcampPersistencePort.listBootcamps(DEFAULT_CRITERIA)).thenReturn(Mono.just(pageResult));
        when(capabilityGateway.findCapabilitiesByIds(List.of(1L, 2L, 3L), MESSAGE_ID)).thenReturn(Mono.just(capabilities));

        // When
        Mono<BootcampListResult> responseMono = bootcampListUseCase.listBootcamps(DEFAULT_CRITERIA, MESSAGE_ID);

        // Then
        StepVerifier.create(responseMono)
                .assertNext(result -> {
                    assertThat(result.page()).isEqualTo(0);
                    assertThat(result.size()).isEqualTo(10);
                    assertThat(result.totalElements()).isEqualTo(2L);
                    assertThat(result.totalPages()).isEqualTo(1);
                    assertThat(result.content()).hasSize(2);

                    BootcampListItem firstItem = result.content().get(0);
                    assertThat(firstItem.id()).isEqualTo(1L);
                    assertThat(firstItem.name()).isEqualTo(BOOTCAMP_NAME);
                    assertThat(firstItem.description()).isEqualTo(BOOTCAMP_DESCRIPTION);
                    assertThat(firstItem.releaseDate()).isEqualTo(LocalDate.of(2026, Month.JULY, 1));
                    assertThat(firstItem.duration()).isEqualTo(120);
                    assertThat(firstItem.capabilities())
                            .extracting(Capability::id, Capability::name)
                            .containsExactly(
                                    org.assertj.core.groups.Tuple.tuple(1L, "Java"),
                                    org.assertj.core.groups.Tuple.tuple(2L, "SQL")
                            );

                    BootcampListItem secondItem = result.content().get(1);
                    assertThat(secondItem.id()).isEqualTo(2L);
                    assertThat(secondItem.name()).isEqualTo("Bootcamp Cloud");
                    assertThat(secondItem.description()).isEqualTo("Arquitectura y despliegue");
                    assertThat(secondItem.releaseDate()).isEqualTo(LocalDate.of(2026, Month.JULY, 15));
                    assertThat(secondItem.duration()).isEqualTo(90);
                    assertThat(secondItem.capabilities())
                            .extracting(Capability::id, Capability::name)
                            .containsExactly(
                                    org.assertj.core.groups.Tuple.tuple(2L, "SQL"),
                                    org.assertj.core.groups.Tuple.tuple(3L, "Cloud")
                            );
                })
                .verifyComplete();

        verify(bootcampPersistencePort).listBootcamps(DEFAULT_CRITERIA);
        verify(capabilityGateway).findCapabilitiesByIds(List.of(1L, 2L, 3L), MESSAGE_ID);
    }

    @Test
    void debeListarBootcampsSinCapacidadesCuandoNoHayIdsEnLosBootcamps() {
        // Given
        BootcampSummary bootcamp = new BootcampSummary(
                1L,
                BOOTCAMP_NAME,
                BOOTCAMP_DESCRIPTION,
                LocalDate.of(2026, Month.JULY, 1),
                120,
                0L,
                List.of()
        );
        BootcampPageResult pageResult = new BootcampPageResult(List.of(bootcamp), 0, 10, 1L, 1);

        when(bootcampPersistencePort.listBootcamps(DEFAULT_CRITERIA)).thenReturn(Mono.just(pageResult));

        // When
        Mono<BootcampListResult> responseMono = bootcampListUseCase.listBootcamps(DEFAULT_CRITERIA, MESSAGE_ID);

        // Then
        StepVerifier.create(responseMono)
                .assertNext(result -> {
                    assertThat(result.page()).isEqualTo(0);
                    assertThat(result.size()).isEqualTo(10);
                    assertThat(result.totalElements()).isEqualTo(1L);
                    assertThat(result.totalPages()).isEqualTo(1);
                    assertThat(result.content()).hasSize(1);
                    assertThat(result.content().get(0).capabilities()).isEmpty();
                })
                .verifyComplete();

        verify(bootcampPersistencePort).listBootcamps(DEFAULT_CRITERIA);
        verifyNoInteractions(capabilityGateway);
    }

    @Test
    void debePropagarElErrorCuandoFallaLaConsultaDeBootcamps() {
        // Given
        RuntimeException unexpectedException = new RuntimeException("unexpected");
        when(bootcampPersistencePort.listBootcamps(DEFAULT_CRITERIA)).thenReturn(Mono.error(unexpectedException));

        // When
        Mono<BootcampListResult> responseMono = bootcampListUseCase.listBootcamps(DEFAULT_CRITERIA, MESSAGE_ID);

        // Then
        StepVerifier.create(responseMono)
                .expectErrorSatisfies(error -> assertThat(error).isSameAs(unexpectedException))
                .verify();

        verify(bootcampPersistencePort).listBootcamps(DEFAULT_CRITERIA);
        verifyNoInteractions(capabilityGateway);
    }

    @Test
    void debePropagarElErrorCuandoFallaLaConsultaDeCapacidades() {
        // Given
        BootcampSummary bootcamp = new BootcampSummary(
                1L,
                BOOTCAMP_NAME,
                BOOTCAMP_DESCRIPTION,
                LocalDate.of(2026, Month.JULY, 1),
                120,
                1L,
                List.of(1L)
        );
        BootcampPageResult pageResult = new BootcampPageResult(List.of(bootcamp), 0, 10, 1L, 1);
        RuntimeException unexpectedException = new RuntimeException("unexpected");

        when(bootcampPersistencePort.listBootcamps(DEFAULT_CRITERIA)).thenReturn(Mono.just(pageResult));
        when(capabilityGateway.findCapabilitiesByIds(List.of(1L), MESSAGE_ID)).thenReturn(Mono.error(unexpectedException));

        // When
        Mono<BootcampListResult> responseMono = bootcampListUseCase.listBootcamps(DEFAULT_CRITERIA, MESSAGE_ID);

        // Then
        StepVerifier.create(responseMono)
                .expectErrorSatisfies(error -> assertThat(error).isSameAs(unexpectedException))
                .verify();

        verify(bootcampPersistencePort).listBootcamps(DEFAULT_CRITERIA);
        verify(capabilityGateway).findCapabilitiesByIds(List.of(1L), MESSAGE_ID);
        verify(capabilityGateway, never()).deleteCapabilitiesByIds(List.of(1L), MESSAGE_ID);
    }
}
