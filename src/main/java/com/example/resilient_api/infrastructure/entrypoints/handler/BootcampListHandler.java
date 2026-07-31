package com.example.resilient_api.infrastructure.entrypoints.handler;

import com.example.resilient_api.domain.api.BootcampListServicePort;
import com.example.resilient_api.domain.enums.TechnicalMessage;
import com.example.resilient_api.domain.exceptions.BusinessException;
import com.example.resilient_api.infrastructure.entrypoints.mapper.BootcampMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
public class BootcampListHandler {

    private final BootcampListServicePort bootcampListServicePort;
    private final BootcampMapper bootcampMapper;
    private final BootcampHandlerSupport support;

    public Mono<ServerResponse> handle(ServerRequest request) {
        String messageId = support.getMessageId(request);

        return Mono.fromSupplier(() -> support.buildCriteria(request))
                .flatMap(criteria -> bootcampListServicePort.listBootcamps(criteria, messageId))
                .map(bootcampMapper::bootcampListResultToBootcampPageDTO)
                .flatMap(page -> ServerResponse.ok().bodyValue(
                        support.buildApiResponse(TechnicalMessage.BOOTCAMP_LISTED, messageId, page)))
                .doOnError(ex -> log.error("Error listing bootcamps for messageId: {}", messageId, ex))
                .onErrorResume(BusinessException.class, ex -> support.buildErrorResponse(
                        HttpStatus.BAD_REQUEST,
                        messageId,
                        ex.getTechnicalMessage()))
                .onErrorResume(ex -> support.buildErrorResponse(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        messageId,
                        TechnicalMessage.INTERNAL_ERROR));
    }
}