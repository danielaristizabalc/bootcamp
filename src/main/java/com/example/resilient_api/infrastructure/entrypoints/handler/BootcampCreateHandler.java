package com.example.resilient_api.infrastructure.entrypoints.handler;

import com.example.resilient_api.domain.api.BootcampServicePort;
import com.example.resilient_api.domain.enums.TechnicalMessage;
import com.example.resilient_api.domain.exceptions.BusinessException;
import com.example.resilient_api.domain.exceptions.TechnicalException;
import com.example.resilient_api.infrastructure.entrypoints.dto.BootcampDTO;
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
public class BootcampCreateHandler {

    private final BootcampServicePort bootcampServicePort;
    private final BootcampMapper bootcampMapper;
    private final BootcampHandlerSupport support;

    public Mono<ServerResponse> handle(ServerRequest request) {
        String messageId = support.getMessageId(request);

        return request.bodyToMono(BootcampDTO.class)
                .switchIfEmpty(Mono.error(new BusinessException(TechnicalMessage.INVALID_REQUEST)))
                .flatMap(bootcampDTO -> bootcampServicePort.registerBootcamp(
                        bootcampMapper.bootcampDTOToBootcamp(bootcampDTO),
                        messageId)
                        .doOnSuccess(savedBootcamp -> log.info("Bootcamp created successfully with messageId: {}", messageId)))
                .flatMap(savedBootcamp -> ServerResponse
                        .status(HttpStatus.CREATED)
                        .bodyValue(support.buildApiResponse(
                                TechnicalMessage.BOOTCAMP_CREATED,
                                messageId,
                                bootcampMapper.bootcampToBootcampDTO(savedBootcamp))))
                .doOnError(ex -> log.error("Error creating bootcamp for messageId: {}", messageId, ex))
                .onErrorResume(BusinessException.class, ex -> support.buildErrorResponse(
                        HttpStatus.BAD_REQUEST,
                        messageId,
                        ex.getTechnicalMessage()))
                .onErrorResume(TechnicalException.class, ex -> support.buildErrorResponse(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        messageId,
                        ex.getTechnicalMessage()))
                .onErrorResume(ex -> {
                    log.error("Unexpected error occurred for messageId: {}", messageId, ex);
                    return support.buildErrorResponse(
                            HttpStatus.INTERNAL_SERVER_ERROR,
                            messageId,
                            TechnicalMessage.INTERNAL_ERROR);
                });
    }
}