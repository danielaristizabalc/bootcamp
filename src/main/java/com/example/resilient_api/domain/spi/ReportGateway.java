package com.example.resilient_api.domain.spi;

import com.example.resilient_api.domain.model.Bootcamp;
import com.example.resilient_api.domain.model.Capability;
import reactor.core.publisher.Mono;

import java.util.List;

public interface ReportGateway {
    Mono<Void> sendBootcampCreatedReport(Bootcamp bootcamp, List<Capability> capabilities, String messageId);
}
