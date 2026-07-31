package com.example.resilient_api.domain.usecase;

import com.example.resilient_api.domain.enums.TechnicalMessage;
import com.example.resilient_api.domain.exceptions.BusinessException;
import com.example.resilient_api.domain.exceptions.TechnicalException;
import com.example.resilient_api.domain.model.Bootcamp;
import com.example.resilient_api.domain.model.Capability;
import com.example.resilient_api.domain.model.Technology;
import com.example.resilient_api.domain.spi.BootcampPersistencePort;
import com.example.resilient_api.domain.spi.CapabilityGateway;
import com.example.resilient_api.domain.spi.ReportGateway;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BootcampUseCaseTest {

    private static final String MESSAGE_ID = "message-id-123";
    private static final Bootcamp VALID_BOOTCAMP = new Bootcamp(
            1L,
            "Bootcamp Java",
            "Bootcamp de backend",
            LocalDate.of(2026, Month.JULY, 31),
            120,
            List.of(1L, 2L)
    );

    @Mock
    private BootcampPersistencePort bootcampPersistencePort;

    @Mock
    private CapabilityGateway capabilityGateway;

    @Mock
    private ReportGateway reportGateway;

    private BootcampUseCase bootcampUseCase;

    @BeforeEach
    void setUp() {
        bootcampUseCase = new BootcampUseCase(bootcampPersistencePort, capabilityGateway, reportGateway);
    }

    @Test
    void debeRegistrarBootcampCuandoLaPeticionEsValida() {
        // Given
        List<Capability> capabilities = List.of(
                new Capability(1L, "Java", List.of(new Technology(1L, "Spring Boot"))),
                new Capability(2L, "SQL", List.of(new Technology(2L, "PostgreSQL")))
        );

        when(bootcampPersistencePort.existsByName(VALID_BOOTCAMP.name())).thenReturn(Mono.just(false));
        when(capabilityGateway.validateCapabilities(VALID_BOOTCAMP.capabilityIds(), MESSAGE_ID)).thenReturn(Mono.just(VALID_BOOTCAMP.capabilityIds()));
        when(bootcampPersistencePort.save(VALID_BOOTCAMP)).thenReturn(Mono.just(VALID_BOOTCAMP));
        when(capabilityGateway.findCapabilitiesByIds(VALID_BOOTCAMP.capabilityIds(), MESSAGE_ID)).thenReturn(Mono.just(capabilities));
        when(reportGateway.sendBootcampCreatedReport(VALID_BOOTCAMP, capabilities, MESSAGE_ID)).thenReturn(Mono.empty());

        // When
        Mono<Bootcamp> responseMono = bootcampUseCase.registerBootcamp(VALID_BOOTCAMP, MESSAGE_ID);

        // Then
        StepVerifier.create(responseMono)
                .expectNext(VALID_BOOTCAMP)
                .verifyComplete();
        verify(bootcampPersistencePort).existsByName(VALID_BOOTCAMP.name());
        verify(capabilityGateway).validateCapabilities(VALID_BOOTCAMP.capabilityIds(), MESSAGE_ID);
        verify(bootcampPersistencePort).save(VALID_BOOTCAMP);
        verify(capabilityGateway, timeout(1000)).findCapabilitiesByIds(VALID_BOOTCAMP.capabilityIds(), MESSAGE_ID);
        verify(reportGateway, timeout(1000)).sendBootcampCreatedReport(VALID_BOOTCAMP, capabilities, MESSAGE_ID);
    }

    @Test
    void debeLanzarBusinessExceptionCuandoLaCantidadDeCapacidadesEsInvalida() {
        // Given
        Bootcamp invalidBootcamp = new Bootcamp(
                1L,
                "Bootcamp Java",
                "Bootcamp de backend",
                LocalDate.of(2026, Month.JULY, 31),
                120,
                List.of()
        );

        // When
        Mono<Bootcamp> responseMono = bootcampUseCase.registerBootcamp(invalidBootcamp, MESSAGE_ID);

        // Then
        StepVerifier.create(responseMono)
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(BusinessException.class);
                    BusinessException businessException = (BusinessException) error;
                    assertThat(businessException.getTechnicalMessage()).isEqualTo(TechnicalMessage.INVALID_CAPABILITIES_COUNT);
                })
                .verify();
        verifyNoInteractions(bootcampPersistencePort, capabilityGateway, reportGateway);
    }

    @Test
    void debeLanzarBusinessExceptionCuandoElBootcampYaExiste() {
        // Given
        when(bootcampPersistencePort.existsByName(VALID_BOOTCAMP.name())).thenReturn(Mono.just(true));

        // When
        Mono<Bootcamp> responseMono = bootcampUseCase.registerBootcamp(VALID_BOOTCAMP, MESSAGE_ID);

        // Then
        StepVerifier.create(responseMono)
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(BusinessException.class);
                    BusinessException businessException = (BusinessException) error;
                    assertThat(businessException.getTechnicalMessage()).isEqualTo(TechnicalMessage.BOOTCAMP_ALREADY_EXISTS);
                })
                .verify();
        verify(bootcampPersistencePort).existsByName(VALID_BOOTCAMP.name());
        verifyNoInteractions(capabilityGateway, reportGateway);
    }

    @Test
    void debePropagarElErrorCuandoLaValidacionDeCapacidadesFalla() {
        // Given
        TechnicalException technicalException = new TechnicalException(TechnicalMessage.CAPABILITY_SERVICE_UNAVAILABLE);

        when(bootcampPersistencePort.existsByName(VALID_BOOTCAMP.name())).thenReturn(Mono.just(false));
        when(capabilityGateway.validateCapabilities(VALID_BOOTCAMP.capabilityIds(), MESSAGE_ID)).thenReturn(Mono.error(technicalException));

        // When
        Mono<Bootcamp> responseMono = bootcampUseCase.registerBootcamp(VALID_BOOTCAMP, MESSAGE_ID);

        // Then
        StepVerifier.create(responseMono)
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(TechnicalException.class);
                    TechnicalException technicalError = (TechnicalException) error;
                    assertThat(technicalError.getTechnicalMessage()).isEqualTo(TechnicalMessage.CAPABILITY_SERVICE_UNAVAILABLE);
                })
                .verify();
        verify(bootcampPersistencePort).existsByName(VALID_BOOTCAMP.name());
        verify(capabilityGateway).validateCapabilities(VALID_BOOTCAMP.capabilityIds(), MESSAGE_ID);
        verifyNoInteractions(reportGateway);
    }
}