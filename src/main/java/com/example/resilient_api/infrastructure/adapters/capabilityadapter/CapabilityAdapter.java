package com.example.resilient_api.infrastructure.adapters.capabilityadapter;

import com.example.resilient_api.domain.enums.TechnicalMessage;
import com.example.resilient_api.domain.exceptions.BusinessException;
import com.example.resilient_api.domain.exceptions.TechnicalException;
import com.example.resilient_api.domain.model.Capability;
import com.example.resilient_api.domain.model.Technology;
import com.example.resilient_api.domain.spi.CapabilityGateway;
import com.example.resilient_api.infrastructure.adapters.capabilityadapter.dto.CapabilityResponseDTO;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class CapabilityAdapter implements CapabilityGateway {

    @Qualifier("capabilityWebClient")
    private final WebClient capabilityWebClient;
    private final Bulkhead bulkhead;

    @Override
    @CircuitBreaker(name = "capabilityService", fallbackMethod = "fallback")
    public Mono<List<Long>> validateCapabilities(List<Long> capabilityIds, String messageId) {
        log.info("Validating capabilities: {} with messageId: {}", capabilityIds, messageId);
        return fetchCapabilityCatalog()
                .doOnNext(response -> log.info("Received capability service response for messageId: {}", messageId))
                .flatMap(response -> validateCapabilityIds(response, capabilityIds, messageId))
                .transformDeferred(mono -> Mono.defer(() -> bulkhead.executeSupplier(() -> mono)))
                .doOnTerminate(() -> log.info("Completed capability validation for messageId: {}", messageId))
                .doOnError(e -> log.error("Error validating capabilities for messageId: {}", messageId, e));
    }

        @Override
        public Mono<List<Capability>> findCapabilitiesByIds(List<Long> capabilityIds, String messageId) {
        log.info("Finding capabilities by ids: {} with messageId: {}", capabilityIds, messageId);

        return fetchCapabilityCatalog()
            .map(response -> response.getContent().stream()
                .filter(capability -> capabilityIds.contains(capability.getId()))
                .map(this::toDomainCapability)
                .toList())
            .transformDeferred(mono -> Mono.defer(() -> bulkhead.executeSupplier(() -> mono)))
            .doOnTerminate(() -> log.info("Completed capability lookup for messageId: {}", messageId))
            .doOnError(e -> log.error("Error finding capabilities for messageId: {}", messageId, e));
        }

        private Mono<CapabilityResponseDTO> fetchCapabilityCatalog() {
        return capabilityWebClient.get()
            .uri(uriBuilder -> uriBuilder.path("/ability").build())
            .retrieve()
            .onStatus(HttpStatusCode::is4xxClientError,
                response -> buildErrorResponse(response, TechnicalMessage.CAPABILITIES_NOT_FOUND))
            .onStatus(HttpStatusCode::is5xxServerError,
                response -> buildErrorResponse(response, TechnicalMessage.CAPABILITY_SERVICE_UNAVAILABLE))
            .bodyToMono(CapabilityResponseDTO.class);
        }

    private Mono<List<Long>> validateCapabilityIds(CapabilityResponseDTO response, 
                                                     List<Long> requestedIds, 
                                                     String messageId) {
        List<Long> foundIds = response.getContent().stream()
                .map(CapabilityResponseDTO. CapabilityDTO::getId)
                .toList();
        
        List<Long> missingIds = requestedIds.stream()
                .filter(id -> !foundIds.contains(id))
                .collect(Collectors.toList());
        
        if (!missingIds.isEmpty()) {
            log.warn("Missing capabilities: {} for messageId: {}", missingIds, messageId);
            return Mono.error(new BusinessException(TechnicalMessage.CAPABILITIES_NOT_FOUND));
        }
        
        log.info("All capabilities validated successfully for messageId: {}", messageId);
        return Mono.just(requestedIds);
    }

    private Capability toDomainCapability(CapabilityResponseDTO.CapabilityDTO capabilityDTO) {
        return new Capability(
                capabilityDTO.getId(),
                capabilityDTO.getNombre(),
                capabilityDTO.getTecnologias() == null ? List.of() : capabilityDTO.getTecnologias().stream()
                        .map(technologyDTO -> new Technology(technologyDTO.getId(), technologyDTO.getName()))
                        .toList()
        );
    }

    @Override
    public Mono<Void> deleteCapabilitiesByIds(List<Long> capabilityIds, String messageId) {
        if (capabilityIds == null || capabilityIds.isEmpty()) {
            log.info("No capabilities to delete for messageId: {}", messageId);
            return Mono.empty();
        }

        log.info("Simulating capability deletion for ids: {} with messageId: {}", capabilityIds, messageId);
        return Mono.empty();
    }

    public Mono<List<Long>> fallback(Throwable t) {
        log.error("Fallback triggered for capability service", t);
        return Mono.error(new TechnicalException(TechnicalMessage.CAPABILITY_SERVICE_UNAVAILABLE));
    }

    private Mono<Throwable> buildErrorResponse(org.springframework.web.reactive.function.client.ClientResponse response, 
                                                  TechnicalMessage technicalMessage) {
        return response.bodyToMono(String.class)
                .defaultIfEmpty("No additional error details")
                .flatMap(errorBody -> {
                    log.error("Error response from capability service: {}", errorBody);
                    return Mono.error(
                            response.statusCode().is5xxServerError() ?
                                    new TechnicalException(technicalMessage) :
                                    new BusinessException(technicalMessage));
                });
    }
}
