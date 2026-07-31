package com.example.resilient_api.infrastructure.entrypoints.handler;

import com.example.resilient_api.domain.enums.TechnicalMessage;
import com.example.resilient_api.domain.exceptions.BusinessException;
import com.example.resilient_api.domain.model.BootcampListCriteria;
import com.example.resilient_api.infrastructure.entrypoints.dto.BootcampBasicInfoDTO;
import com.example.resilient_api.infrastructure.entrypoints.dto.BootcampValidationResponseDTO;
import com.example.resilient_api.infrastructure.entrypoints.util.APIResponse;
import com.example.resilient_api.infrastructure.entrypoints.util.ErrorDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static com.example.resilient_api.infrastructure.entrypoints.util.Constants.X_MESSAGE_ID;

@ExtendWith(MockitoExtension.class)
class BootcampHandlerSupportTest {

    private static final String MESSAGE_ID = "message-id-123";

    @Mock
    private ServerRequest request;

    @Mock
    private ServerRequest.Headers headers;

    private BootcampHandlerSupport support;

    @BeforeEach
    void setUp() {
        support = new BootcampHandlerSupport();
    }

    @Test
    void debeObtenerMessageIdCuandoElHeaderExiste() {
        // Given
        when(request.headers()).thenReturn(headers);
        when(headers.firstHeader(X_MESSAGE_ID)).thenReturn(MESSAGE_ID);

        // When
        String messageId = support.getMessageId(request);

        // Then
        assertThat(messageId).isEqualTo(MESSAGE_ID);
    }

    @Test
    void debeRetornarBootcampListCriteriaCuandoLosParametrosSonValidos() {
        // Given
        when(request.queryParam("page")).thenReturn(Optional.of("2"));
        when(request.queryParam("size")).thenReturn(Optional.of("15"));
        when(request.queryParam("sortBy")).thenReturn(Optional.of("name"));
        when(request.queryParam("sortDirection")).thenReturn(Optional.of("desc"));

        // When
        BootcampListCriteria criteria = support.buildCriteria(request);

        // Then
        assertThat(criteria.page()).isEqualTo(2);
        assertThat(criteria.size()).isEqualTo(15);
        assertThat(criteria.sortBy()).isEqualTo("name");
        assertThat(criteria.sortDirection()).isEqualTo("desc");
    }

    @Test
    void debeLanzarBusinessExceptionCuandoElParametroPageEsInvalido() {
        // Given
        when(request.queryParam("page")).thenReturn(Optional.of("abc"));

        // When
        BusinessException exception = assertThrows(BusinessException.class, () -> support.buildCriteria(request));

        // Then
        assertThat(exception.getTechnicalMessage()).isEqualTo(TechnicalMessage.INVALID_PARAMETERS);
    }

    @Test
    void debeConstruirErrorDetailsCuandoSeRecibeUnTechnicalMessage() {
        // Given

        // When
        List<ErrorDTO> errors = support.buildErrorDetails(TechnicalMessage.INVALID_PARAMETERS);

        // Then
        assertThat(errors).hasSize(1);
        assertThat(errors.get(0).getCode()).isEqualTo(TechnicalMessage.INVALID_PARAMETERS.getCode());
        assertThat(errors.get(0).getMessage()).isEqualTo(TechnicalMessage.INVALID_PARAMETERS.getMessage());
        assertThat(errors.get(0).getParam()).isEqualTo(TechnicalMessage.INVALID_PARAMETERS.getParam());
    }

    @Test
    void debeConstruirApiResponseConDataCuandoSeInvocaElMetodoConData() {
        // Given
        BootcampValidationResponseDTO data = BootcampValidationResponseDTO.builder()
                .bootcamps(List.of(BootcampBasicInfoDTO.builder()
                        .id(1L)
                        .name("Bootcamp Java")
                .releaseDate(LocalDate.of(2026, Month.JULY, 31))
                        .duration(120)
                        .build()))
                .build();

        // When
        APIResponse response = support.buildApiResponse(TechnicalMessage.BOOTCAMP_VALIDATED, MESSAGE_ID, data);

        // Then
        assertThat(response.getCode()).isEqualTo(TechnicalMessage.BOOTCAMP_VALIDATED.getCode());
        assertThat(response.getMessage()).isEqualTo(TechnicalMessage.BOOTCAMP_VALIDATED.getMessage());
        assertThat(response.getIdentifier()).isEqualTo(MESSAGE_ID);
        assertThat(response.getDate()).isNotBlank();
        assertThat(response.getData()).isEqualTo(data);
    }

    @Test
    void debeConstruirApiResponseSinDataCuandoSeInvocaElMetodoSinData() {
        // Given

        // When
        APIResponse response = support.buildApiResponse(TechnicalMessage.BOOTCAMP_DELETED, MESSAGE_ID);

        // Then
        assertThat(response.getCode()).isEqualTo(TechnicalMessage.BOOTCAMP_DELETED.getCode());
        assertThat(response.getMessage()).isEqualTo(TechnicalMessage.BOOTCAMP_DELETED.getMessage());
        assertThat(response.getIdentifier()).isEqualTo(MESSAGE_ID);
        assertThat(response.getDate()).isNotBlank();
        assertThat(response.getData()).isNull();
        assertThat(response.getErrors()).isNull();
    }

    @Test
    void debeConstruirErrorResponseConEstadoBadRequestCuandoSeUsanErroresExplicitos() {
        // Given
        List<ErrorDTO> errors = support.buildErrorDetails(TechnicalMessage.INVALID_PARAMETERS);

        // When
        Mono<ServerResponse> responseMono = support.buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                MESSAGE_ID,
                TechnicalMessage.INVALID_PARAMETERS,
                errors);

        // Then
        StepVerifier.create(responseMono)
                .assertNext(response -> assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST))
                .verifyComplete();
    }

    @Test
    void debeConstruirErrorResponseConEstadoInternoCuandoSeUsaElAtajoDeError() {
        // Given

        // When
        Mono<ServerResponse> responseMono = support.buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                MESSAGE_ID,
                TechnicalMessage.INTERNAL_ERROR);

        // Then
        StepVerifier.create(responseMono)
                .assertNext(response -> assertThat(response.statusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR))
                .verifyComplete();
    }
}