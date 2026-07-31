package com.example.resilient_api.infrastructure.entrypoints.handler;

import com.example.resilient_api.domain.api.BootcampValidateServicePort;
import com.example.resilient_api.domain.enums.TechnicalMessage;
import com.example.resilient_api.domain.exceptions.BusinessException;
import com.example.resilient_api.domain.exceptions.TechnicalException;
import com.example.resilient_api.domain.model.BootcampBasicInfo;
import com.example.resilient_api.domain.model.BootcampValidationResult;
import com.example.resilient_api.infrastructure.entrypoints.dto.BootcampBasicInfoDTO;
import com.example.resilient_api.infrastructure.entrypoints.dto.BootcampIdsRequestDTO;
import com.example.resilient_api.infrastructure.entrypoints.dto.BootcampValidationResponseDTO;
import com.example.resilient_api.infrastructure.entrypoints.mapper.BootcampMapper;
import com.example.resilient_api.infrastructure.entrypoints.util.APIResponse;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BootcampValidateHandlerTest {

    private static final String MESSAGE_ID = "message-id-123";

    @Mock
    private BootcampValidateServicePort bootcampValidateServicePort;

    @Mock
    private BootcampMapper bootcampMapper;

    @Mock
    private BootcampHandlerSupport support;

    @Mock
    private ServerRequest request;

    private BootcampValidateHandler bootcampValidateHandler;

    @BeforeEach
    void setUp() {
        bootcampValidateHandler = new BootcampValidateHandler(bootcampValidateServicePort, bootcampMapper, support);
    }

    @Test
    void debeValidarBootcampsCuandoLaPeticionEsValida() {
        // Given
        List<Long> ids = List.of(1L, 2L);
        BootcampIdsRequestDTO body = BootcampIdsRequestDTO.builder().idBootcamps(ids).build();
        BootcampValidationResult validationResult = new BootcampValidationResult(List.of(
                new BootcampBasicInfo(1L, "Bootcamp Java", LocalDate.of(2026, Month.JULY, 31), 120)
        ));
        BootcampValidationResponseDTO responseDto = BootcampValidationResponseDTO.builder()
                .bootcamps(List.of(BootcampBasicInfoDTO.builder()
                        .id(1L)
                        .name("Bootcamp Java")
                        .releaseDate(LocalDate.of(2026, Month.JULY, 31))
                        .duration(120)
                        .build()))
                .build();
        APIResponse apiResponse = mock(APIResponse.class);

        when(support.getMessageId(request)).thenReturn(MESSAGE_ID);
        when(request.bodyToMono(BootcampIdsRequestDTO.class)).thenReturn(Mono.just(body));
        when(bootcampValidateServicePort.validateBootcamps(ids, MESSAGE_ID)).thenReturn(Mono.just(validationResult));
        when(bootcampMapper.bootcampValidationResultToDto(validationResult)).thenReturn(responseDto);
        when(support.buildApiResponse(TechnicalMessage.BOOTCAMP_VALIDATED, MESSAGE_ID, responseDto)).thenReturn(apiResponse);

        // When
        Mono<ServerResponse> responseMono = bootcampValidateHandler.handle(request);

        // Then
        StepVerifier.create(responseMono)
                .assertNext(response -> assertThat(response.statusCode()).isEqualTo(HttpStatus.OK))
                .verifyComplete();
        verify(support).getMessageId(request);
        verify(request).bodyToMono(BootcampIdsRequestDTO.class);
        verify(bootcampValidateServicePort).validateBootcamps(ids, MESSAGE_ID);
        verify(bootcampMapper).bootcampValidationResultToDto(validationResult);
        verify(support).buildApiResponse(TechnicalMessage.BOOTCAMP_VALIDATED, MESSAGE_ID, responseDto);
    }

    @Test
    void debeRetornarBadRequestCuandoElBodyVieneVacio() {
        // Given
        ServerResponse expectedResponse = mock(ServerResponse.class);

        when(support.getMessageId(request)).thenReturn(MESSAGE_ID);
        when(request.bodyToMono(BootcampIdsRequestDTO.class)).thenReturn(Mono.empty());
        when(support.buildErrorResponse(HttpStatus.BAD_REQUEST, MESSAGE_ID, TechnicalMessage.INVALID_REQUEST))
                .thenReturn(Mono.just(expectedResponse));

        // When
        Mono<ServerResponse> responseMono = bootcampValidateHandler.handle(request);

        // Then
        StepVerifier.create(responseMono)
                .expectNext(expectedResponse)
                .verifyComplete();
        verify(support).buildErrorResponse(HttpStatus.BAD_REQUEST, MESSAGE_ID, TechnicalMessage.INVALID_REQUEST);
    }

    @Test
    void debeRetornarBadRequestCuandoLaListaDeIdsVieneVacia() {
        // Given
        ServerResponse expectedResponse = mock(ServerResponse.class);
        BootcampIdsRequestDTO body = BootcampIdsRequestDTO.builder().idBootcamps(List.of()).build();

        when(support.getMessageId(request)).thenReturn(MESSAGE_ID);
        when(request.bodyToMono(BootcampIdsRequestDTO.class)).thenReturn(Mono.just(body));
        when(support.buildErrorResponse(HttpStatus.BAD_REQUEST, MESSAGE_ID, TechnicalMessage.INVALID_REQUEST))
                .thenReturn(Mono.just(expectedResponse));

        // When
        Mono<ServerResponse> responseMono = bootcampValidateHandler.handle(request);

        // Then
        StepVerifier.create(responseMono)
                .expectNext(expectedResponse)
                .verifyComplete();
        verify(support).buildErrorResponse(HttpStatus.BAD_REQUEST, MESSAGE_ID, TechnicalMessage.INVALID_REQUEST);
    }

    @Test
    void debeRetornarInternalServerErrorCuandoElServicioLanzaTechnicalException() {
        // Given
        ServerResponse expectedResponse = mock(ServerResponse.class);
        List<Long> ids = List.of(1L, 2L);
        BootcampIdsRequestDTO body = BootcampIdsRequestDTO.builder().idBootcamps(ids).build();
        TechnicalException technicalException = new TechnicalException(TechnicalMessage.CAPABILITY_SERVICE_UNAVAILABLE);

        when(support.getMessageId(request)).thenReturn(MESSAGE_ID);
        when(request.bodyToMono(BootcampIdsRequestDTO.class)).thenReturn(Mono.just(body));
        when(bootcampValidateServicePort.validateBootcamps(ids, MESSAGE_ID)).thenReturn(Mono.error(technicalException));
        when(support.buildErrorResponse(any(), any(), any())).thenReturn(Mono.just(expectedResponse));

        // When
        Mono<ServerResponse> responseMono = bootcampValidateHandler.handle(request);

        // Then
        StepVerifier.create(responseMono)
                .expectNext(expectedResponse)
                .verifyComplete();
        verify(support).buildErrorResponse(any(), any(), any());
    }

    @Test
    void debeRetornarInternalServerErrorCuandoOcurreUnErrorInesperado() {
        // Given
        ServerResponse expectedResponse = mock(ServerResponse.class);
        List<Long> ids = List.of(1L, 2L);
        BootcampIdsRequestDTO body = BootcampIdsRequestDTO.builder().idBootcamps(ids).build();
        RuntimeException unexpectedException = new RuntimeException("unexpected");

        when(support.getMessageId(request)).thenReturn(MESSAGE_ID);
        when(request.bodyToMono(BootcampIdsRequestDTO.class)).thenReturn(Mono.just(body));
        when(bootcampValidateServicePort.validateBootcamps(ids, MESSAGE_ID)).thenReturn(Mono.error(unexpectedException));
        when(support.buildErrorResponse(any(), any(), any())).thenReturn(Mono.just(expectedResponse));

        // When
        Mono<ServerResponse> responseMono = bootcampValidateHandler.handle(request);

        // Then
        StepVerifier.create(responseMono)
                .expectNext(expectedResponse)
                .verifyComplete();
        verify(support).buildErrorResponse(any(), any(), any());
    }
}