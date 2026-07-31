package com.example.resilient_api.infrastructure.entrypoints.handler;

import com.example.resilient_api.domain.api.BootcampDeleteServicePort;
import com.example.resilient_api.domain.enums.TechnicalMessage;
import com.example.resilient_api.domain.exceptions.BusinessException;
import com.example.resilient_api.domain.exceptions.TechnicalException;
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
public class BootcampDeleteHandler {

    private final BootcampDeleteServicePort bootcampDeleteServicePort;
    private final BootcampHandlerSupport support;

    public Mono<ServerResponse> handle(ServerRequest request) {
        String messageId = support.getMessageId(request);

        return Mono.fromSupplier(() -> Long.valueOf(request.pathVariable("id")))
                .flatMap(bootcampId -> bootcampDeleteServicePort.deleteBootcamp(bootcampId, messageId)
                        .then(ServerResponse.ok().bodyValue(
                                support.buildApiResponse(TechnicalMessage.BOOTCAMP_DELETED, messageId))))
                .onErrorResume(NumberFormatException.class, ex -> support.buildErrorResponse(
                        HttpStatus.BAD_REQUEST,
                        messageId,
                        TechnicalMessage.INVALID_PARAMETERS))
                .onErrorResume(BusinessException.class, ex -> support.buildErrorResponse(
                        HttpStatus.BAD_REQUEST,
                        messageId,
                        ex.getTechnicalMessage()))
                .onErrorResume(TechnicalException.class, ex -> support.buildErrorResponse(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        messageId,
                        ex.getTechnicalMessage()))
                .onErrorResume(ex -> support.buildErrorResponse(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        messageId,
                        TechnicalMessage.INTERNAL_ERROR));
    }
}