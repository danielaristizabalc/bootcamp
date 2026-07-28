package com.example.resilient_api.domain.spi;

import reactor.core.publisher.Mono;
import java.util.List;

public interface CapabilityGateway {
    /**
     * Valida que todas las capacidades existan en el microservicio de capacidades.
     * @param capabilityIds Lista de IDs de capacidades a validar
     * @param messageId ID de correlación para trazabilidad
     * @return Mono<List<Long>> con los IDs validados, o error si alguno no existe
     */
    Mono<List<Long>> validateCapabilities(List<Long> capabilityIds, String messageId);
}
