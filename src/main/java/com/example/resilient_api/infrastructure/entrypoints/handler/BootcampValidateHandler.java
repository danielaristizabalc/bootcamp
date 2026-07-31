package com.example.resilient_api.infrastructure.entrypoints.handler;

import com.example.resilient_api.domain.api.BootcampValidateServicePort;
import com.example.resilient_api.domain.enums.TechnicalMessage;
import com.example.resilient_api.domain.exceptions.BusinessException;
import com.example.resilient_api.domain.exceptions.TechnicalException;
import com.example.resilient_api.infrastructure.entrypoints.dto.BootcampIdsRequestDTO;
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
public class BootcampValidateHandler {

    private final BootcampValidateServicePort bootcampValidateServicePort;
    private final BootcampMapper bootcampMapper;
    private final BootcampHandlerSupport support;

    public Mono<ServerResponse> handle(ServerRequest request) {
        String messageId = support.getMessageId(request);

        return request.bodyToMono(BootcampIdsRequestDTO.class)
                .switchIfEmpty(Mono.error(new BusinessException(TechnicalMessage.INVALID_REQUEST)))
                .flatMap(body -> {
                    if (body.getIdBootcamps() == null || body.getIdBootcamps().isEmpty()) {
                        return Mono.error(new BusinessException(TechnicalMessage.INVALID_REQUEST));
                    }
                    return bootcampValidateServicePort.validateBootcamps(body.getIdBootcamps(), messageId);
                })
                .map(bootcampMapper::bootcampValidationResultToDto)
                .flatMap(result -> ServerResponse.ok().bodyValue(
                        support.buildApiResponse(TechnicalMessage.BOOTCAMP_VALIDATED, messageId, result)))
                .doOnError(ex -> log.error("Error validating bootcamps for messageId: {}", messageId, ex))
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