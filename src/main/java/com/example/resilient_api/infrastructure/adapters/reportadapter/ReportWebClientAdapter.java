package com.example.resilient_api.infrastructure.adapters.reportadapter;

import com.example.resilient_api.domain.enums.TechnicalMessage;
import com.example.resilient_api.domain.exceptions.TechnicalException;
import com.example.resilient_api.domain.model.Bootcamp;
import com.example.resilient_api.domain.model.Capability;
import com.example.resilient_api.domain.model.Technology;
import com.example.resilient_api.domain.spi.ReportGateway;
import com.example.resilient_api.infrastructure.adapters.reportadapter.dto.BootcampReportRequestDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReportWebClientAdapter implements ReportGateway {

    @Qualifier("reportWebClient")
    private final WebClient reportWebClient;

    @Override
    public Mono<Void> sendBootcampCreatedReport(Bootcamp bootcamp, List<Capability> capabilities, String messageId) {
        BootcampReportRequestDTO request = toRequest(bootcamp, capabilities);

        return reportWebClient.post()
                .uri("/reports/bootcamp")
                .bodyValue(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> Mono.error(new TechnicalException(TechnicalMessage.REPORT_SERVICE_UNAVAILABLE)))
                .bodyToMono(Void.class)
                .doOnSuccess(ignored -> log.info("Bootcamp report sent successfully for messageId: {}", messageId));
    }

    private BootcampReportRequestDTO toRequest(Bootcamp bootcamp, List<Capability> capabilities) {
        List<BootcampReportRequestDTO.CapabilityReportDTO> capabilityDTOs = capabilities.stream()
                .map(capability -> {
                    List<Technology> technologies = capability.technologies() == null ? List.of() : capability.technologies();
                    List<BootcampReportRequestDTO.TechnologyReportDTO> technologyDTOs = technologies.stream()
                            .map(technology -> new BootcampReportRequestDTO.TechnologyReportDTO(technology.id(), technology.name()))
                            .toList();

                    return new BootcampReportRequestDTO.CapabilityReportDTO(capability.id(), capability.name(), technologyDTOs);
                })
                .toList();

        return new BootcampReportRequestDTO(
                bootcamp.id(),
                bootcamp.name(),
                bootcamp.description(),
                bootcamp.releaseDate(),
                bootcamp.duration(),
                capabilityDTOs
        );
    }
}
