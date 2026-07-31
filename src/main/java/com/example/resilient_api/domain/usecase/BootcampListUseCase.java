package com.example.resilient_api.domain.usecase;

import com.example.resilient_api.domain.api.BootcampListServicePort;
import com.example.resilient_api.domain.model.*;
import com.example.resilient_api.domain.spi.BootcampListQueryPort;
import com.example.resilient_api.domain.spi.CapabilityGateway;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

public class BootcampListUseCase implements BootcampListServicePort {

    private final BootcampListQueryPort bootcampListQueryPort;
    private final CapabilityGateway capabilityGateway;

    public BootcampListUseCase(BootcampListQueryPort bootcampListQueryPort,
                               CapabilityGateway capabilityGateway) {
        this.bootcampListQueryPort = bootcampListQueryPort;
        this.capabilityGateway = capabilityGateway;
    }

    @Override
    public Mono<BootcampListResult> listBootcamps(BootcampListCriteria criteria, String messageId) {
        return bootcampListQueryPort.listBootcamps(criteria)
                .flatMap(page -> enrichPage(page, messageId));
    }

    private Mono<BootcampListResult> enrichPage(BootcampPageResult page, String messageId) {
        List<Long> capabilityIds = page.content().stream()
                .flatMap(bootcamp -> bootcamp.capabilityIds().stream())
                .distinct()
                .toList();

        if (capabilityIds.isEmpty()) {
            return Mono.just(toResult(page, List.of()));
        }

        return capabilityGateway.findCapabilitiesByIds(capabilityIds, messageId)
                .map(capabilities -> toResult(page, capabilities));
    }

    private BootcampListResult toResult(BootcampPageResult page, List<Capability> capabilities) {
        Map<Long, Capability> capabilityById = capabilities.stream()
                .collect(Collectors.toMap(Capability::id, Function.identity(), (first, second) -> first));

        List<BootcampListItem> content = page.content().stream()
                .map(bootcamp -> toItem(bootcamp, capabilityById))
                .toList();

        return new BootcampListResult(
                content,
                page.page(),
                page.size(),
                page.totalElements(),
                page.totalPages()
        );
    }

    private BootcampListItem toItem(BootcampSummary bootcamp, Map<Long, Capability> capabilityById) {
        List<Capability> capabilities = bootcamp.capabilityIds().stream()
                .map(capabilityById::get)
                .filter(Objects::nonNull)
                .toList();

        return new BootcampListItem(
                bootcamp.id(),
                bootcamp.name(),
                bootcamp.description(),
                bootcamp.releaseDate(),
                bootcamp.duration(),
                capabilities
        );
    }
}