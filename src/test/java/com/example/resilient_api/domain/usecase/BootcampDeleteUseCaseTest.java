package com.example.resilient_api.domain.usecase;

import com.example.resilient_api.domain.enums.TechnicalMessage;
import com.example.resilient_api.domain.exceptions.BusinessException;
import com.example.resilient_api.domain.spi.BootcampPersistencePort;
import com.example.resilient_api.domain.spi.CapabilityGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BootcampDeleteUseCaseTest {

    private static final Long BOOTCAMP_ID = 7L;
    private static final String MESSAGE_ID = "message-id-123";

    @Mock
    private BootcampPersistencePort bootcampPersistencePort;

    @Mock
    private CapabilityGateway capabilityGateway;

    private BootcampDeleteUseCase bootcampDeleteUseCase;

    @BeforeEach
    void setUp() {
        bootcampDeleteUseCase = new BootcampDeleteUseCase(bootcampPersistencePort, capabilityGateway);
    }

    @Test
    void debeEliminarBootcampYCapacidadesCuandoExistenCapacidadesExclusivas() {
        // Given
        List<Long> exclusiveCapabilityIds = List.of(10L, 11L);
        when(bootcampPersistencePort.existsById(BOOTCAMP_ID)).thenReturn(Mono.just(true));
        when(bootcampPersistencePort.findExclusiveCapabilityIdsByBootcampId(BOOTCAMP_ID)).thenReturn(Mono.just(exclusiveCapabilityIds));
        when(capabilityGateway.deleteCapabilitiesByIds(exclusiveCapabilityIds, MESSAGE_ID)).thenReturn(Mono.empty());
        when(bootcampPersistencePort.deleteBootcampById(BOOTCAMP_ID)).thenReturn(Mono.empty());

        // When
        Mono<Void> responseMono = bootcampDeleteUseCase.deleteBootcamp(BOOTCAMP_ID, MESSAGE_ID);

        // Then
        StepVerifier.create(responseMono)
                .verifyComplete();
        verify(bootcampPersistencePort).existsById(BOOTCAMP_ID);
        verify(bootcampPersistencePort).findExclusiveCapabilityIdsByBootcampId(BOOTCAMP_ID);
        verify(capabilityGateway).deleteCapabilitiesByIds(exclusiveCapabilityIds, MESSAGE_ID);
        verify(bootcampPersistencePort).deleteBootcampById(BOOTCAMP_ID);
    }

    @Test
    void debeEliminarBootcampSinTocarCapacidadesCuandoNoHayCapacidadesExclusivas() {
        // Given
        when(bootcampPersistencePort.existsById(BOOTCAMP_ID)).thenReturn(Mono.just(true));
        when(bootcampPersistencePort.findExclusiveCapabilityIdsByBootcampId(BOOTCAMP_ID)).thenReturn(Mono.just(List.of()));
        when(bootcampPersistencePort.deleteBootcampById(BOOTCAMP_ID)).thenReturn(Mono.empty());

        // When
        Mono<Void> responseMono = bootcampDeleteUseCase.deleteBootcamp(BOOTCAMP_ID, MESSAGE_ID);

        // Then
        StepVerifier.create(responseMono)
                .verifyComplete();
        verify(bootcampPersistencePort).existsById(BOOTCAMP_ID);
        verify(bootcampPersistencePort).findExclusiveCapabilityIdsByBootcampId(BOOTCAMP_ID);
        verifyNoInteractions(capabilityGateway);
        verify(bootcampPersistencePort).deleteBootcampById(BOOTCAMP_ID);
    }

    @Test
    void debeLanzarBusinessExceptionCuandoElBootcampNoExiste() {
        // Given
        when(bootcampPersistencePort.existsById(BOOTCAMP_ID)).thenReturn(Mono.just(false));

        // When
        Mono<Void> responseMono = bootcampDeleteUseCase.deleteBootcamp(BOOTCAMP_ID, MESSAGE_ID);

        // Then
        StepVerifier.create(responseMono)
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(BusinessException.class);
                    BusinessException businessException = (BusinessException) error;
                    assertThat(businessException.getTechnicalMessage()).isEqualTo(TechnicalMessage.BOOTCAMP_NOT_FOUND);
                })
                .verify();
        verify(bootcampPersistencePort).existsById(BOOTCAMP_ID);
        verify(bootcampPersistencePort, never()).findExclusiveCapabilityIdsByBootcampId(any());
        verifyNoInteractions(capabilityGateway);
    }

    @Test
    void debePropagarElErrorCuandoFallaEliminacionDeCapacidades() {
        // Given
        List<Long> exclusiveCapabilityIds = List.of(10L, 11L);
        RuntimeException unexpectedException = new RuntimeException("unexpected");
        when(bootcampPersistencePort.existsById(BOOTCAMP_ID)).thenReturn(Mono.just(true));
        when(bootcampPersistencePort.findExclusiveCapabilityIdsByBootcampId(BOOTCAMP_ID)).thenReturn(Mono.just(exclusiveCapabilityIds));
        when(capabilityGateway.deleteCapabilitiesByIds(exclusiveCapabilityIds, MESSAGE_ID)).thenReturn(Mono.error(unexpectedException));

        // When
        Mono<Void> responseMono = bootcampDeleteUseCase.deleteBootcamp(BOOTCAMP_ID, MESSAGE_ID);

        // Then
        StepVerifier.create(responseMono)
                .expectErrorSatisfies(error -> assertThat(error).isSameAs(unexpectedException))
                .verify();
        verify(bootcampPersistencePort).existsById(BOOTCAMP_ID);
        verify(bootcampPersistencePort).findExclusiveCapabilityIdsByBootcampId(BOOTCAMP_ID);
        verify(capabilityGateway).deleteCapabilitiesByIds(exclusiveCapabilityIds, MESSAGE_ID);
        verify(bootcampPersistencePort, never()).deleteBootcampById(eq(BOOTCAMP_ID));
    }
}