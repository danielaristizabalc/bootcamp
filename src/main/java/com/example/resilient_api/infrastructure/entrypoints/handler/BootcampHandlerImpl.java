package com.example.resilient_api.infrastructure.entrypoints.handler;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class BootcampHandlerImpl {

    private final BootcampCreateHandler bootcampCreateHandler;
    private final BootcampListHandler bootcampListHandler;
    private final BootcampDeleteHandler bootcampDeleteHandler;
    private final BootcampValidateHandler bootcampValidateHandler;

    public Mono<ServerResponse> createBootcamp(ServerRequest request) {
        return bootcampCreateHandler.handle(request);
    }

    public Mono<ServerResponse> listBootcamps(ServerRequest request) {
        return bootcampListHandler.handle(request);
    }

    public Mono<ServerResponse> deleteBootcamp(ServerRequest request) {
        return bootcampDeleteHandler.handle(request);
    }

    public Mono<ServerResponse> validateBootcamps(ServerRequest request) {
        return bootcampValidateHandler.handle(request);
    }
}
