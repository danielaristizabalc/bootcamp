package com.example.resilient_api.domain.usecase;

import com.example.resilient_api.domain.model.BootcampBasicInfo;
import com.example.resilient_api.domain.model.BootcampValidationResult;
import com.example.resilient_api.domain.spi.BootcampPersistencePort;
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
class BootcampValidateUseCaseTest {

    private static final String MESSAGE_ID = "message-id-123";
    private static final List<Long> BOOTCAMP_IDS = List.of(1L, 2L);

    @Mock
    private BootcampPersistencePort bootcampPersistencePort;

    private BootcampValidateUseCase bootcampValidateUseCase;

    @BeforeEach
    void setUp() {
        bootcampValidateUseCase = new BootcampValidateUseCase(bootcampPersistencePort);
    }

    @Test
    void debeRetornarBootcampsValidadosCuandoLaListaDeIdsEsValida() {
        // Given
        List<BootcampBasicInfo> bootcamps = List.of(
                new BootcampBasicInfo(1L, "Bootcamp Java", LocalDate.of(2026, Month.JULY, 31), 120),
                new BootcampBasicInfo(2L, "Bootcamp Cloud", LocalDate.of(2026, Month.AUGUST, 1), 90)
        );
        BootcampValidationResult expectedResult = new BootcampValidationResult(bootcamps);

        when(bootcampPersistencePort.findBootcampsByIds(BOOTCAMP_IDS)).thenReturn(Mono.just(bootcamps));

        // When
        Mono<BootcampValidationResult> responseMono = bootcampValidateUseCase.validateBootcamps(BOOTCAMP_IDS, MESSAGE_ID);

        // Then
        StepVerifier.create(responseMono)
                .assertNext(result -> {
                    assertThat(result.bootcamps()).hasSize(2);
                    assertThat(result.bootcamps()).isEqualTo(expectedResult.bootcamps());
                    assertThat(result.bootcamps().get(0).id()).isEqualTo(1L);
                    assertThat(result.bootcamps().get(0).name()).isEqualTo("Bootcamp Java");
                    assertThat(result.bootcamps().get(0).releaseDate()).isEqualTo(LocalDate.of(2026, Month.JULY, 31));
                    assertThat(result.bootcamps().get(0).duration()).isEqualTo(120);
                    assertThat(result.bootcamps().get(1).id()).isEqualTo(2L);
                    assertThat(result.bootcamps().get(1).name()).isEqualTo("Bootcamp Cloud");
                    assertThat(result.bootcamps().get(1).releaseDate()).isEqualTo(LocalDate.of(2026, Month.AUGUST, 1));
                    assertThat(result.bootcamps().get(1).duration()).isEqualTo(90);
                })
                .verifyComplete();
        verify(bootcampPersistencePort).findBootcampsByIds(BOOTCAMP_IDS);
    }

    @Test
    void debeRetornarListaVaciaCuandoLaListaDeIdsEsNula() {
        // Given

        // When
        Mono<BootcampValidationResult> responseMono = bootcampValidateUseCase.validateBootcamps(null, MESSAGE_ID);

        // Then
        StepVerifier.create(responseMono)
                .assertNext(result -> assertThat(result.bootcamps()).isEmpty())
                .verifyComplete();
        verifyNoInteractions(bootcampPersistencePort);
    }

    @Test
    void debeRetornarListaVaciaCuandoLaListaDeIdsEstaVacia() {
        // Given

        // When
        Mono<BootcampValidationResult> responseMono = bootcampValidateUseCase.validateBootcamps(List.of(), MESSAGE_ID);

        // Then
        StepVerifier.create(responseMono)
                .assertNext(result -> assertThat(result.bootcamps()).isEmpty())
                .verifyComplete();
        verifyNoInteractions(bootcampPersistencePort);
    }

    @Test
    void debePropagarElErrorCuandoFallaLaConsultaDeBootcamps() {
        // Given
        RuntimeException unexpectedException = new RuntimeException("unexpected");
        when(bootcampPersistencePort.findBootcampsByIds(BOOTCAMP_IDS)).thenReturn(Mono.error(unexpectedException));

        // When
        Mono<BootcampValidationResult> responseMono = bootcampValidateUseCase.validateBootcamps(BOOTCAMP_IDS, MESSAGE_ID);

        // Then
        StepVerifier.create(responseMono)
                .expectErrorSatisfies(error -> assertThat(error).isSameAs(unexpectedException))
                .verify();
        verify(bootcampPersistencePort).findBootcampsByIds(BOOTCAMP_IDS);
        verify(bootcampPersistencePort, never()).save(null);
    }
}
