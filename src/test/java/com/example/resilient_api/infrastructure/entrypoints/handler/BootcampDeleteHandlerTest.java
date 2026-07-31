package com.example.resilient_api.infrastructure.entrypoints.handler;

import com.example.resilient_api.domain.api.BootcampDeleteServicePort;
import com.example.resilient_api.domain.enums.TechnicalMessage;
import com.example.resilient_api.domain.exceptions.BusinessException;
import com.example.resilient_api.domain.exceptions.TechnicalException;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BootcampDeleteHandlerTest {

    private static final String MESSAGE_ID = "message-id-123";
    private static final String BOOTCAMP_ID = "7";

    @Mock
    private BootcampDeleteServicePort bootcampDeleteServicePort;

    @Mock
    private BootcampHandlerSupport support;

    @Mock
    private ServerRequest request;

    private BootcampDeleteHandler bootcampDeleteHandler;

    @BeforeEach
    void setUp() {
        bootcampDeleteHandler = new BootcampDeleteHandler(bootcampDeleteServicePort, support);
    }

    @Test
    void debeEliminarBootcampCuandoLaPeticionEsValida() {
        // Given
        when(support.getMessageId(request)).thenReturn(MESSAGE_ID);
        when(request.pathVariable("id")).thenReturn(BOOTCAMP_ID);
        when(bootcampDeleteServicePort.deleteBootcamp(7L, MESSAGE_ID)).thenReturn(Mono.empty());
        when(support.buildApiResponse(TechnicalMessage.BOOTCAMP_DELETED, MESSAGE_ID))
                .thenReturn(mock(com.example.resilient_api.infrastructure.entrypoints.util.APIResponse.class));

        // When
        Mono<ServerResponse> responseMono = bootcampDeleteHandler.handle(request);

        // Then
        StepVerifier.create(responseMono)
                .expectNextCount(1)
                .verifyComplete();
        verify(support).getMessageId(request);
        verify(request).pathVariable("id");
        verify(bootcampDeleteServicePort).deleteBootcamp(7L, MESSAGE_ID);
        verify(support).buildApiResponse(TechnicalMessage.BOOTCAMP_DELETED, MESSAGE_ID);
    }

    @Test
    void debeRetornarBadRequestCuandoElIdNoEsNumerico() {
        // Given
        ServerResponse expectedResponse = mock(ServerResponse.class);
        when(support.getMessageId(request)).thenReturn(MESSAGE_ID);
        when(request.pathVariable("id")).thenReturn("abc");
        when(support.buildErrorResponse(HttpStatus.BAD_REQUEST, MESSAGE_ID, TechnicalMessage.INVALID_PARAMETERS))
                .thenReturn(Mono.just(expectedResponse));

        // When
        Mono<ServerResponse> responseMono = bootcampDeleteHandler.handle(request);

        // Then
        StepVerifier.create(responseMono)
                .expectNext(expectedResponse)
                .verifyComplete();
        verify(support).buildErrorResponse(HttpStatus.BAD_REQUEST, MESSAGE_ID, TechnicalMessage.INVALID_PARAMETERS);
    }

    @Test
    void debeRetornarBadRequestCuandoElServicioLanzaBusinessException() {
        // Given
        ServerResponse expectedResponse = mock(ServerResponse.class);
        BusinessException businessException = new BusinessException(TechnicalMessage.BOOTCAMP_NOT_FOUND);
        when(support.getMessageId(request)).thenReturn(MESSAGE_ID);
        when(request.pathVariable("id")).thenReturn(BOOTCAMP_ID);
        when(bootcampDeleteServicePort.deleteBootcamp(7L, MESSAGE_ID)).thenReturn(Mono.error(businessException));
                when(support.buildErrorResponse(any(), any(), any()))
                .thenReturn(Mono.just(expectedResponse));

        // When
        Mono<ServerResponse> responseMono = bootcampDeleteHandler.handle(request);

        // Then
        StepVerifier.create(responseMono)
                .expectNext(expectedResponse)
                .verifyComplete();
        verify(support).buildErrorResponse(any(), any(), any());
    }

    @Test
    void debeRetornarInternalServerErrorCuandoElServicioLanzaTechnicalException() {
        // Given
        ServerResponse expectedResponse = mock(ServerResponse.class);
        TechnicalException technicalException = new TechnicalException(TechnicalMessage.CAPABILITY_SERVICE_UNAVAILABLE);
        when(support.getMessageId(request)).thenReturn(MESSAGE_ID);
        when(request.pathVariable("id")).thenReturn(BOOTCAMP_ID);
        when(bootcampDeleteServicePort.deleteBootcamp(7L, MESSAGE_ID)).thenReturn(Mono.error(technicalException));
                when(support.buildErrorResponse(any(), any(), any()))
                .thenReturn(Mono.just(expectedResponse));

        // When
        Mono<ServerResponse> responseMono = bootcampDeleteHandler.handle(request);

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
        RuntimeException unexpectedException = new RuntimeException("unexpected");
        when(support.getMessageId(request)).thenReturn(MESSAGE_ID);
        when(request.pathVariable("id")).thenReturn(BOOTCAMP_ID);
        when(bootcampDeleteServicePort.deleteBootcamp(7L, MESSAGE_ID)).thenReturn(Mono.error(unexpectedException));
                when(support.buildErrorResponse(any(), any(), any()))
                .thenReturn(Mono.just(expectedResponse));

        // When
        Mono<ServerResponse> responseMono = bootcampDeleteHandler.handle(request);

        // Then
        StepVerifier.create(responseMono)
                .expectNext(expectedResponse)
                .verifyComplete();
        verify(support).buildErrorResponse(any(), any(), any());
    }
}