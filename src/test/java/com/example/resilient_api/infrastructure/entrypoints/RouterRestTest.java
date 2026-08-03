package com.example.resilient_api.infrastructure.entrypoints;

import com.example.resilient_api.infrastructure.entrypoints.handler.BootcampHandlerImpl;
import com.example.resilient_api.infrastructure.entrypoints.handler.UserHandlerImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RouterRestTest {

    @Mock
    private BootcampHandlerImpl bootcampHandler;

    private RouterRest routerRest;
    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        routerRest = new RouterRest();
            RouterFunction<ServerResponse> routerFunction = routerRest.routerFunction(bootcampHandler);
        webTestClient = WebTestClient.bindToRouterFunction(routerFunction).build();
        }

    @Test
    void debeCrearRouterFunctionCuandoSeInvocaElBean() {
        // Given

        // When
            RouterFunction<ServerResponse> routerFunction = routerRest.routerFunction(bootcampHandler);

        // Then
        org.junit.jupiter.api.Assertions.assertNotNull(routerFunction);
    }

    @Test
    void debeDelegarEnCreateBootcampCuandoLaRutaEsPostBootcamp() {
        // Given
        when(bootcampHandler.createBootcamp(any())).thenReturn(ServerResponse.ok().build());

        // When
        webTestClient.post()
                .uri("/bootcamp")
                .bodyValue("{}")
                .exchange()
                .expectStatus().isOk();

        // Then
        verify(bootcampHandler).createBootcamp(any());
    }

    @Test
    void debeDelegarEnValidateBootcampsCuandoLaRutaEsPostBootcampValidate() {
        // Given
        when(bootcampHandler.validateBootcamps(any())).thenReturn(ServerResponse.ok().build());

        // When
        webTestClient.post()
                .uri("/bootcamp/validate")
                .bodyValue("{}")
                .exchange()
                .expectStatus().isOk();

        // Then
        verify(bootcampHandler).validateBootcamps(any());
    }

    @Test
    void debeDelegarEnListBootcampsCuandoLaRutaEsGetBootcamp() {
        // Given
        when(bootcampHandler.listBootcamps(any())).thenReturn(ServerResponse.ok().build());

        // When
        webTestClient.get()
                .uri("/bootcamp")
                .exchange()
                .expectStatus().isOk();

        // Then
        verify(bootcampHandler).listBootcamps(any());
    }

    @Test
    void debeDelegarEnDeleteBootcampCuandoLaRutaEsDeleteBootcampId() {
        // Given
        when(bootcampHandler.deleteBootcamp(any())).thenReturn(ServerResponse.ok().build());

        // When
        webTestClient.delete()
                .uri("/bootcamp/1")
                .exchange()
                .expectStatus().isOk();

        // Then
        verify(bootcampHandler).deleteBootcamp(any());
    }
}