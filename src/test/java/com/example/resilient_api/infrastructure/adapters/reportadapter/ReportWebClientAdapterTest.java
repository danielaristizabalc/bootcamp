package com.example.resilient_api.infrastructure.adapters.reportadapter;

import com.example.resilient_api.domain.enums.TechnicalMessage;
import com.example.resilient_api.domain.exceptions.TechnicalException;
import com.example.resilient_api.domain.model.Bootcamp;
import com.example.resilient_api.domain.model.Capability;
import com.example.resilient_api.domain.model.Technology;
import com.example.resilient_api.infrastructure.adapters.reportadapter.dto.BootcampReportRequestDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportWebClientAdapterTest {

    private static final String MESSAGE_ID = "message-id-123";

    private static final Bootcamp BOOTCAMP = new Bootcamp(
            10L,
            "Bootcamp Java",
            "Bootcamp de backend",
            LocalDate.of(2026, Month.JULY, 31),
            120,
            List.of(1L, 2L)
    );

    private static final Capability CAPABILITY_WITH_TECHNOLOGIES = new Capability(
            1L,
            "Java",
            List.of(
                    new Technology(11L, "Spring Boot"),
                    new Technology(12L, "JUnit 5")
            )
    );

    private static final Capability CAPABILITY_WITHOUT_TECHNOLOGIES = new Capability(
            2L,
            "Cloud",
            null
    );

    @Mock
    private WebClient reportWebClient;

    @Mock
    @SuppressWarnings("rawtypes")
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    @SuppressWarnings("rawtypes")
    private WebClient.RequestBodySpec requestBodySpec;

    @Mock
    @SuppressWarnings("rawtypes")
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    private ReportWebClientAdapter reportWebClientAdapter;

    @BeforeEach
    void setUp() {
        reportWebClientAdapter = new ReportWebClientAdapter(reportWebClient);
    }

    @Test
    void debeEnviarReporteDeBootcampConCapacidadesYTecnologiasCuandoLaPeticionEsValida() {
        // Given
        when(reportWebClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri("/reports/bootcamp")).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Void.class)).thenReturn(Mono.empty());

        ArgumentCaptor<BootcampReportRequestDTO> requestCaptor = ArgumentCaptor.forClass(BootcampReportRequestDTO.class);

        // When
        Mono<Void> responseMono = reportWebClientAdapter.sendBootcampCreatedReport(
                BOOTCAMP,
                List.of(CAPABILITY_WITH_TECHNOLOGIES, CAPABILITY_WITHOUT_TECHNOLOGIES),
                MESSAGE_ID
        );

        // Then
        StepVerifier.create(responseMono)
                .verifyComplete();

        org.mockito.Mockito.verify(requestBodySpec).bodyValue(requestCaptor.capture());

        BootcampReportRequestDTO requestDTO = requestCaptor.getValue();
        assertThat(requestDTO.id()).isEqualTo(BOOTCAMP.id());
        assertThat(requestDTO.name()).isEqualTo(BOOTCAMP.name());
        assertThat(requestDTO.description()).isEqualTo(BOOTCAMP.description());
        assertThat(requestDTO.releaseDate()).isEqualTo(BOOTCAMP.releaseDate());
        assertThat(requestDTO.duration()).isEqualTo(BOOTCAMP.duration());
        assertThat(requestDTO.capabilities()).hasSize(2);

        assertThat(requestDTO.capabilities().get(0).id()).isEqualTo(CAPABILITY_WITH_TECHNOLOGIES.id());
        assertThat(requestDTO.capabilities().get(0).nombre()).isEqualTo(CAPABILITY_WITH_TECHNOLOGIES.name());
        assertThat(requestDTO.capabilities().get(0).tecnologias())
                .extracting(BootcampReportRequestDTO.TechnologyReportDTO::id, BootcampReportRequestDTO.TechnologyReportDTO::name)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(11L, "Spring Boot"),
                        org.assertj.core.groups.Tuple.tuple(12L, "JUnit 5")
                );

        assertThat(requestDTO.capabilities().get(1).id()).isEqualTo(CAPABILITY_WITHOUT_TECHNOLOGIES.id());
        assertThat(requestDTO.capabilities().get(1).nombre()).isEqualTo(CAPABILITY_WITHOUT_TECHNOLOGIES.name());
        assertThat(requestDTO.capabilities().get(1).tecnologias()).isEmpty();
    }

    @Test
    void debeLanzarTechnicalExceptionCuandoElServicioRespondeErrorHttp() {
        // Given
        AtomicReference<org.springframework.web.reactive.function.client.ClientRequest> capturedRequest = new AtomicReference<>();
        ExchangeFunction exchangeFunction = request -> {
            capturedRequest.set(request);
            return Mono.just(ClientResponse.create(HttpStatus.INTERNAL_SERVER_ERROR).build());
        };

        WebClient realWebClient = WebClient.builder()
                .baseUrl("http://localhost:8090")
                .exchangeFunction(exchangeFunction)
                .build();

        reportWebClientAdapter = new ReportWebClientAdapter(realWebClient);

        // When
        Mono<Void> responseMono = reportWebClientAdapter.sendBootcampCreatedReport(
                BOOTCAMP,
                List.of(CAPABILITY_WITH_TECHNOLOGIES),
                MESSAGE_ID
        );

        // Then
        StepVerifier.create(responseMono)
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(TechnicalException.class);
                    TechnicalException technicalException = (TechnicalException) error;
                    assertThat(technicalException.getTechnicalMessage())
                            .isEqualTo(TechnicalMessage.REPORT_SERVICE_UNAVAILABLE);
                })
                .verify();

        assertThat(capturedRequest.get()).isNotNull();
        assertThat(capturedRequest.get().url().toString()).isEqualTo("http://localhost:8090/reports/bootcamp");
    }
}