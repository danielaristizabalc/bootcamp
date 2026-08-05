package com.example.resilient_api.infrastructure.adapters.capabilityadapter;

import com.example.resilient_api.domain.enums.TechnicalMessage;
import com.example.resilient_api.domain.exceptions.BusinessException;
import com.example.resilient_api.domain.exceptions.TechnicalException;
import com.example.resilient_api.domain.model.Capability;
import com.example.resilient_api.domain.model.Technology;
import com.example.resilient_api.infrastructure.adapters.capabilityadapter.dto.CapabilityResponseDTO;
import com.example.resilient_api.infrastructure.adapters.capabilityadapter.dto.DeleteCapabilityRequestDTO;
import io.github.resilience4j.bulkhead.Bulkhead;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.URI;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CapabilityAdapterTest {

    private static final String MESSAGE_ID = "message-id-123";
    private static final List<Long> REQUESTED_CAPABILITY_IDS = List.of(1L, 2L);
    private static final String SPRING_BOOT_NAME = "Spring Boot";
    private static final String ABILITY_PATH = "/ability";

    @Mock
    private WebClient capabilityWebClient;

    @Mock
    private Bulkhead bulkhead;

    @Mock
    @SuppressWarnings("rawtypes")
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    @SuppressWarnings("rawtypes")
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    @SuppressWarnings("rawtypes")
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    @SuppressWarnings("rawtypes")
    private WebClient.RequestBodySpec requestBodySpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    private CapabilityAdapter capabilityAdapter;

    @BeforeEach
    void setUp() {
        capabilityAdapter = new CapabilityAdapter(capabilityWebClient, bulkhead);
    }

    @Test
    void debeValidarCapacidadesCuandoTodasExisten() {
        // Given
        CapabilityResponseDTO responseDTO = capabilityResponseDTO(
            capabilityDTO(1L, "Java", List.of(tecnologiaDTO(11L, SPRING_BOOT_NAME))),
            capabilityDTO(2L, "SQL", List.of(tecnologiaDTO(12L, "PostgreSQL")))
        );

        stubCapabilityCatalog(responseDTO);
        stubBulkhead();

        // When
        Mono<List<Long>> responseMono = capabilityAdapter.validateCapabilities(REQUESTED_CAPABILITY_IDS, MESSAGE_ID);

        // Then
        StepVerifier.create(responseMono)
                .expectNext(REQUESTED_CAPABILITY_IDS)
                .verifyComplete();

        verify(capabilityWebClient).get();
        verify(responseSpec).bodyToMono(CapabilityResponseDTO.class);
    }

    @Test
    void debeLanzarBusinessExceptionCuandoFaltanCapacidades() {
        // Given
        CapabilityResponseDTO responseDTO = capabilityResponseDTO(
            capabilityDTO(1L, "Java", List.of(tecnologiaDTO(11L, SPRING_BOOT_NAME)))
        );

        stubCapabilityCatalog(responseDTO);
        stubBulkhead();

        // When
        Mono<List<Long>> responseMono = capabilityAdapter.validateCapabilities(REQUESTED_CAPABILITY_IDS, MESSAGE_ID);

        // Then
        StepVerifier.create(responseMono)
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(BusinessException.class);
                    BusinessException businessException = (BusinessException) error;
                    assertThat(businessException.getTechnicalMessage()).isEqualTo(TechnicalMessage.CAPABILITIES_NOT_FOUND);
                })
                .verify();

        verify(capabilityWebClient).get();
        verify(responseSpec).bodyToMono(CapabilityResponseDTO.class);
    }

    @Test
    void debeRetornarCapacidadesFiltradasCuandoBuscaPorIds() {
        // Given
        CapabilityResponseDTO responseDTO = capabilityResponseDTO(
            capabilityDTO(1L, "Java", List.of(tecnologiaDTO(11L, SPRING_BOOT_NAME))),
            capabilityDTO(2L, "SQL", List.of(tecnologiaDTO(12L, "PostgreSQL"))),
                capabilityDTO(3L, "Cloud", List.of())
        );

        stubCapabilityCatalog(responseDTO);
        stubBulkhead();

        // When
        Mono<List<Capability>> responseMono = capabilityAdapter.findCapabilitiesByIds(List.of(1L, 3L), MESSAGE_ID);

        // Then
        StepVerifier.create(responseMono)
                .assertNext(capabilities -> {
                    assertThat(capabilities).hasSize(2);
                    assertThat(capabilities.get(0).id()).isEqualTo(1L);
                    assertThat(capabilities.get(0).name()).isEqualTo("Java");
                    assertThat(capabilities.get(0).technologies())
                            .extracting(Technology::id, Technology::name)
                            .containsExactly(org.assertj.core.groups.Tuple.tuple(11L, SPRING_BOOT_NAME));
                    assertThat(capabilities.get(1).id()).isEqualTo(3L);
                    assertThat(capabilities.get(1).name()).isEqualTo("Cloud");
                    assertThat(capabilities.get(1).technologies()).isEmpty();
                })
                .verifyComplete();

        verify(capabilityWebClient).get();
        verify(responseSpec).bodyToMono(CapabilityResponseDTO.class);
    }

    @Test
    void debeCompletarSinHacerNadaCuandoNoHayCapacidadesParaEliminar() {
        // When
        Mono<Void> responseMono = capabilityAdapter.deleteCapabilitiesByIds(List.of(), MESSAGE_ID);

        // Then
        StepVerifier.create(responseMono)
                .verifyComplete();

        verifyNoInteractions(capabilityWebClient, bulkhead);
    }

    @Test
    void debeEliminarCapacidadesCuandoElServicioRespondeOk() {
        // Given
        when(capabilityWebClient.method(HttpMethod.DELETE)).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(ABILITY_PATH)).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any(DeleteCapabilityRequestDTO.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just("Registros eliminados exitosamente"));

        // When
        Mono<Void> responseMono = capabilityAdapter.deleteCapabilitiesByIds(REQUESTED_CAPABILITY_IDS, MESSAGE_ID);

        // Then
        StepVerifier.create(responseMono)
                .verifyComplete();

        verify(capabilityWebClient).method(HttpMethod.DELETE);
        verify(requestBodyUriSpec).uri(ABILITY_PATH);
        verify(requestBodySpec).bodyValue(org.mockito.ArgumentMatchers.argThat(body -> {
            if (!(body instanceof DeleteCapabilityRequestDTO requestDTO)) {
                return false;
            }
            return requestDTO.capabilityIds().equals(REQUESTED_CAPABILITY_IDS);
        }));
        verify(responseSpec).onStatus(any(), any());
        verify(responseSpec).bodyToMono(String.class);
    }

    @Test
    void debeLanzarBusinessExceptionCuandoElServicioDevuelveBadRequest() {
        // Given
        when(capabilityWebClient.method(HttpMethod.DELETE)).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(ABILITY_PATH)).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any(DeleteCapabilityRequestDTO.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class))
                .thenReturn(Mono.error(new BusinessException(TechnicalMessage.INVALID_PARAMETERS)));

        // When
        Mono<Void> responseMono = capabilityAdapter.deleteCapabilitiesByIds(REQUESTED_CAPABILITY_IDS, MESSAGE_ID);

        // Then
        StepVerifier.create(responseMono)
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(BusinessException.class);
                    BusinessException businessException = (BusinessException) error;
                    assertThat(businessException.getTechnicalMessage()).isEqualTo(TechnicalMessage.INVALID_PARAMETERS);
                })
                .verify();

        verify(capabilityWebClient).method(HttpMethod.DELETE);
        verify(requestBodyUriSpec).uri(ABILITY_PATH);
        verify(requestBodySpec).bodyValue(any(DeleteCapabilityRequestDTO.class));
        verify(responseSpec).bodyToMono(String.class);
    }

    @Test
    void debeRetornarTechnicalExceptionEnFallback() {
        // When
        Mono<List<Long>> responseMono = capabilityAdapter.fallback(new RuntimeException("unexpected"));

        // Then
        StepVerifier.create(responseMono)
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(TechnicalException.class);
                    TechnicalException technicalException = (TechnicalException) error;
                    assertThat(technicalException.getTechnicalMessage()).isEqualTo(TechnicalMessage.CAPABILITY_SERVICE_UNAVAILABLE);
                })
                .verify();
    }

    private void stubCapabilityCatalog(CapabilityResponseDTO responseDTO) {
        when(capabilityWebClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(org.mockito.ArgumentMatchers.<Function<UriBuilder, URI>>any()))
            .thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(CapabilityResponseDTO.class)).thenReturn(Mono.just(responseDTO));
    }

    private void stubBulkhead() {
        when(bulkhead.executeSupplier(any())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Supplier<Mono<?>> supplier = invocation.getArgument(0);
            return supplier.get();
        });
    }

    private CapabilityResponseDTO capabilityResponseDTO(CapabilityResponseDTO.CapabilityDTO... capabilities) {
        CapabilityResponseDTO responseDTO = new CapabilityResponseDTO();
        responseDTO.setContent(List.of(capabilities));
        return responseDTO;
    }

    private CapabilityResponseDTO.CapabilityDTO capabilityDTO(Long id, String name, List<CapabilityResponseDTO.CapabilityDTO.TecnologiaDTO> technologies) {
        CapabilityResponseDTO.CapabilityDTO capabilityDTO = new CapabilityResponseDTO.CapabilityDTO();
        capabilityDTO.setId(id);
        capabilityDTO.setNombre(name);
        capabilityDTO.setTecnologias(technologies);
        return capabilityDTO;
    }

    private CapabilityResponseDTO.CapabilityDTO.TecnologiaDTO tecnologiaDTO(Long id, String name) {
        CapabilityResponseDTO.CapabilityDTO.TecnologiaDTO tecnologiaDTO = new CapabilityResponseDTO.CapabilityDTO.TecnologiaDTO();
        tecnologiaDTO.setId(id);
        tecnologiaDTO.setName(name);
        return tecnologiaDTO;
    }
}