package com.example.resilient_api.infrastructure.entrypoints.handler;

import com.example.resilient_api.domain.api.BootcampServicePort;
import com.example.resilient_api.domain.enums.TechnicalMessage;
import com.example.resilient_api.domain.exceptions.BusinessException;
import com.example.resilient_api.domain.exceptions.TechnicalException;
import com.example.resilient_api.infrastructure.entrypoints.dto.BootcampDTO;
import com.example.resilient_api.infrastructure.entrypoints.mapper.BootcampMapper;
import com.example.resilient_api.infrastructure.entrypoints.util.APIResponse;
import com.example.resilient_api.infrastructure.entrypoints.util.ErrorDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

import static com.example.resilient_api.infrastructure.entrypoints.util.Constants.X_MESSAGE_ID;

@Component
@RequiredArgsConstructor
@Slf4j
public class BootcampHandlerImpl {

    private final BootcampServicePort bootcampServicePort;
    private final BootcampMapper bootcampMapper;

    public Mono<ServerResponse> createBootcamp(ServerRequest request) {
        String messageId = getMessageId(request);
        
        return request.bodyToMono(BootcampDTO.class)
                .switchIfEmpty(Mono.error(new BusinessException(TechnicalMessage.INVALID_REQUEST)))
                .flatMap(bootcampDTO -> 
                    bootcampServicePort.registerBootcamp(
                        bootcampMapper.bootcampDTOToBootcamp(bootcampDTO), 
                        messageId
                    )
                    .doOnSuccess(savedBootcamp -> 
                        log.info("Bootcamp created successfully with messageId: {}", messageId))
                )
                .flatMap(savedBootcamp -> 
                    ServerResponse
                        .status(HttpStatus.CREATED)
                        .bodyValue(buildSuccessResponse(savedBootcamp, messageId))
                )
                .doOnError(ex -> 
                    log.error("Error creating bootcamp for messageId: {}", messageId, ex))
                .onErrorResume(BusinessException.class, ex -> 
                    buildErrorResponse(
                        HttpStatus.BAD_REQUEST,
                        messageId,
                        ex.getTechnicalMessage(),
                        List.of(ErrorDTO.builder()
                                .code(ex.getTechnicalMessage().getCode())
                                .message(ex.getTechnicalMessage().getMessage())
                                .param(ex.getTechnicalMessage().getParam())
                                .build())))
                .onErrorResume(TechnicalException.class, ex -> 
                    buildErrorResponse(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        messageId,
                        ex.getTechnicalMessage(),
                        List.of(ErrorDTO.builder()
                                .code(ex.getTechnicalMessage().getCode())
                                .message(ex.getTechnicalMessage().getMessage())
                                .param(ex.getTechnicalMessage().getParam())
                                .build())))
                .onErrorResume(ex -> {
                    log.error("Unexpected error occurred for messageId: {}", messageId, ex);
                    return buildErrorResponse(
                            HttpStatus.INTERNAL_SERVER_ERROR,
                            messageId,
                            TechnicalMessage.INTERNAL_ERROR,
                            List.of(ErrorDTO.builder()
                                    .code(TechnicalMessage.INTERNAL_ERROR.getCode())
                                    .message(TechnicalMessage.INTERNAL_ERROR.getMessage())
                                    .build()));
                });
    }

    private APIResponse buildSuccessResponse(com.example.resilient_api.domain.model.Bootcamp bootcamp, 
                                               String messageId) {
        return APIResponse.builder()
                .code(TechnicalMessage.BOOTCAMP_CREATED.getCode())
                .message(TechnicalMessage.BOOTCAMP_CREATED.getMessage())
                .identifier(messageId)
                .date(Instant.now().toString())
                .data(bootcampMapper.bootcampToBootcampDTO(bootcamp))
                .build();
    }

    private Mono<ServerResponse> buildErrorResponse(HttpStatus httpStatus, 
                                                     String identifier, 
                                                     TechnicalMessage error, 
                                                     List<ErrorDTO> errors) {
        return Mono.defer(() -> {
            APIResponse apiErrorResponse = APIResponse.builder()
                    .code(error.getCode())
                    .message(error.getMessage())
                    .identifier(identifier)
                    .date(Instant.now().toString())
                    .errors(errors)
                    .build();
            return ServerResponse.status(httpStatus)
                    .bodyValue(apiErrorResponse);
        });
    }

    private String getMessageId(ServerRequest serverRequest) {
        return serverRequest.headers().firstHeader(X_MESSAGE_ID);
    }
}
