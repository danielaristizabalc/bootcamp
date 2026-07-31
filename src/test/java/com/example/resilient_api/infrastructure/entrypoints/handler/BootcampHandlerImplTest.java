package com.example.resilient_api.infrastructure.entrypoints.handler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BootcampHandlerImplTest {

    @Mock
    private BootcampCreateHandler bootcampCreateHandler;

    @Mock
    private BootcampListHandler bootcampListHandler;

    @Mock
    private BootcampDeleteHandler bootcampDeleteHandler;

    @Mock
    private BootcampValidateHandler bootcampValidateHandler;

    @Mock
    private ServerRequest request;

    private BootcampHandlerImpl bootcampHandlerImpl;

    @BeforeEach
    void setUp() {
        bootcampHandlerImpl = new BootcampHandlerImpl(
                bootcampCreateHandler,
                bootcampListHandler,
                bootcampDeleteHandler,
                bootcampValidateHandler
        );
    }

    @Test
    void debeDelegarEnCreateBootcampCuandoSeInvocaCreateBootcamp() {
        // Given
        ServerResponse expectedResponse = mock(ServerResponse.class);
        when(bootcampCreateHandler.handle(request)).thenReturn(Mono.just(expectedResponse));

        // When
        Mono<ServerResponse> responseMono = bootcampHandlerImpl.createBootcamp(request);

        // Then
        StepVerifier.create(responseMono)
                .expectNext(expectedResponse)
                .verifyComplete();
        verify(bootcampCreateHandler).handle(request);
    }

    @Test
    void debeDelegarEnListBootcampsCuandoSeInvocaListBootcamps() {
        // Given
        ServerResponse expectedResponse = mock(ServerResponse.class);
        when(bootcampListHandler.handle(request)).thenReturn(Mono.just(expectedResponse));

        // When
        Mono<ServerResponse> responseMono = bootcampHandlerImpl.listBootcamps(request);

        // Then
        StepVerifier.create(responseMono)
                .expectNext(expectedResponse)
                .verifyComplete();
        verify(bootcampListHandler).handle(request);
    }

    @Test
    void debeDelegarEnDeleteBootcampCuandoSeInvocaDeleteBootcamp() {
        // Given
        ServerResponse expectedResponse = mock(ServerResponse.class);
        when(bootcampDeleteHandler.handle(request)).thenReturn(Mono.just(expectedResponse));

        // When
        Mono<ServerResponse> responseMono = bootcampHandlerImpl.deleteBootcamp(request);

        // Then
        StepVerifier.create(responseMono)
                .expectNext(expectedResponse)
                .verifyComplete();
        verify(bootcampDeleteHandler).handle(request);
    }

    @Test
    void debeDelegarEnValidateBootcampsCuandoSeInvocaValidateBootcamps() {
        // Given
        ServerResponse expectedResponse = mock(ServerResponse.class);
        when(bootcampValidateHandler.handle(request)).thenReturn(Mono.just(expectedResponse));

        // When
        Mono<ServerResponse> responseMono = bootcampHandlerImpl.validateBootcamps(request);

        // Then
        StepVerifier.create(responseMono)
                .expectNext(expectedResponse)
                .verifyComplete();
        verify(bootcampValidateHandler).handle(request);
    }
}