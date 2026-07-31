package com.example.resilient_api.infrastructure.entrypoints.handler;

import com.example.resilient_api.domain.enums.TechnicalMessage;
import com.example.resilient_api.domain.exceptions.BusinessException;
import com.example.resilient_api.domain.model.BootcampListCriteria;
import com.example.resilient_api.infrastructure.entrypoints.util.APIResponse;
import com.example.resilient_api.infrastructure.entrypoints.util.ErrorDTO;
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
@Slf4j
public class BootcampHandlerSupport {

    public String getMessageId(ServerRequest serverRequest) {
        return serverRequest.headers().firstHeader(X_MESSAGE_ID);
    }

    public BootcampListCriteria buildCriteria(ServerRequest request) {
        try {
            int page = request.queryParam("page").map(Integer::parseInt).orElse(0);
            int size = request.queryParam("size").map(Integer::parseInt).orElse(10);
            String sortBy = request.queryParam("sortBy").orElse("name");
            String sortDirection = request.queryParam("sortDirection").orElse("asc");
            return new BootcampListCriteria(page, size, sortBy, sortDirection);
        } catch (NumberFormatException ex) {
            throw new BusinessException(TechnicalMessage.INVALID_PARAMETERS);
        }
    }

    public List<ErrorDTO> buildErrorDetails(TechnicalMessage technicalMessage) {
        return List.of(ErrorDTO.builder()
                .code(technicalMessage.getCode())
                .message(technicalMessage.getMessage())
                .param(technicalMessage.getParam())
                .build());
    }

    public APIResponse buildApiResponse(TechnicalMessage technicalMessage, String identifier, Object data) {
        return APIResponse.builder()
                .code(technicalMessage.getCode())
                .message(technicalMessage.getMessage())
                .identifier(identifier)
                .date(Instant.now().toString())
                .data(data)
                .build();
    }

    public APIResponse buildApiResponse(TechnicalMessage technicalMessage, String identifier) {
        return APIResponse.builder()
                .code(technicalMessage.getCode())
                .message(technicalMessage.getMessage())
                .identifier(identifier)
                .date(Instant.now().toString())
                .build();
    }

    public Mono<ServerResponse> buildErrorResponse(HttpStatus httpStatus,
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

    public Mono<ServerResponse> buildErrorResponse(HttpStatus httpStatus,
                                                   String identifier,
                                                   TechnicalMessage error) {
        return buildErrorResponse(httpStatus, identifier, error, buildErrorDetails(error));
    }
}