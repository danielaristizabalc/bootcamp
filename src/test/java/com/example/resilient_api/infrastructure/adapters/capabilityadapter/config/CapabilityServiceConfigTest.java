package com.example.resilient_api.infrastructure.adapters.capabilityadapter.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CapabilityServiceConfigTest {

    private static final String BASE_URL = "http://localhost:8089/capabilities";
    private static final String TIMEOUT = "5000";

    @Mock
    private CapabilityServiceProperties properties;

    private CapabilityServiceConfig capabilityServiceConfig;

    @BeforeEach
    void setUp() {
        capabilityServiceConfig = new CapabilityServiceConfig(properties);
    }

    @Test
    void debeConstruirWebClientConBaseUrlYHeadersPorDefecto() {
        // Given
        when(properties.getBaseUrl()).thenReturn(BASE_URL);
        when(properties.getTimeout()).thenReturn(TIMEOUT);

        AtomicReference<ClientRequest> capturedRequest = new AtomicReference<>();
        ExchangeFunction exchangeFunction = request -> {
            capturedRequest.set(request);
            return Mono.just(ClientResponse.create(HttpStatus.OK)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body("{}")
                    .build());
        };

        WebClient webClient = capabilityServiceConfig.capabilityWebClient()
                .mutate()
                .exchangeFunction(exchangeFunction)
                .build();

        // When
        Mono<String> responseMono = webClient.get()
                .uri("/ability")
                .retrieve()
                .bodyToMono(String.class);

        // Then
        StepVerifier.create(responseMono)
                .expectNext("{}")
                .verifyComplete();

        assertThat(capturedRequest.get()).isNotNull();
        assertThat(capturedRequest.get().url().toString()).isEqualTo(BASE_URL + "/ability");
        assertThat(capturedRequest.get().headers().getFirst(HttpHeaders.CONTENT_TYPE))
                .isEqualTo(MediaType.APPLICATION_JSON_VALUE);
        assertThat(capturedRequest.get().headers().getAccept())
                .contains(MediaType.APPLICATION_JSON);
    }

    @Test
    void debeLanzarNumberFormatExceptionCuandoElTimeoutNoEsNumerico() {
        // Given
        when(properties.getBaseUrl()).thenReturn(BASE_URL);
        when(properties.getTimeout()).thenReturn("invalid-timeout");

        // When / Then
        assertThatThrownBy(() -> capabilityServiceConfig.capabilityWebClient())
                .isInstanceOf(NumberFormatException.class)
                .hasMessageContaining("invalid-timeout");
    }
}