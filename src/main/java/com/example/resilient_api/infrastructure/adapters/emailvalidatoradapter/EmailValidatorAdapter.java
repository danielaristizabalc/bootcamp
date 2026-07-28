package com.example.resilient_api.infrastructure.adapters.emailvalidatoradapter;

import com.example.resilient_api.domain.enums.TechnicalMessage;
import com.example.resilient_api.domain.exceptions.BusinessException;
import com.example.resilient_api.domain.exceptions.TechnicalException;
import com.example.resilient_api.domain.model.EmailValidationResult;
import com.example.resilient_api.domain.spi.EmailValidatorGateway;
import com.example.resilient_api.infrastructure.adapters.emailvalidatoradapter.dto.EmailValidationResponse;
import com.example.resilient_api.infrastructure.adapters.emailvalidatoradapter.dto.EmailValidatorProperties;
import com.example.resilient_api.infrastructure.adapters.emailvalidatoradapter.util.Constants;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.retry.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.concurrent.TimeoutException;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailValidatorAdapter implements EmailValidatorGateway {

    private final WebClient webClient;
    private final EmailValidatorProperties emailValidatorProperties;
    private final Retry retry;
    private final Bulkhead bulkhead;

    @Override
    @CircuitBreaker(name = "emailValidator", fallbackMethod = "fallback")
    public Mono<EmailValidationResult> validateEmail(String email, String messageId) {
        log.info("Starting email validation for email: {} with messageId: {}", email, messageId);
        return webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .queryParam("api_key", emailValidatorProperties.getApiKey())
                            .queryParam("email", email)
                            .build())
                    .header(HttpHeaders.CONTENT_TYPE, "application/json")
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, response -> buildErrorResponse(response, TechnicalMessage.ADAPTER_RESPONSE_NOT_FOUND))
                    .onStatus(HttpStatusCode::is5xxServerError, response -> buildErrorResponse(response, TechnicalMessage.INTERNAL_ERROR_IN_ADAPTERS))
                    .bodyToMono(EmailValidationResponse.class)
                    .doOnNext(response -> log.info("Received API response for messageId: {}: {}", messageId, response))
                    .filter(response -> "DELIVERABLE".equalsIgnoreCase(response.deliverability()))
                    .map(response -> new EmailValidationResult(
                            response.deliverability(),
                            response.quality_score()
                    ))
                    .transformDeferred(RetryOperator.of(retry))
                    .transformDeferred(mono -> Mono.defer(() -> bulkhead.executeSupplier(() -> mono)))
                    .doOnTerminate(() -> log.info("Completed email validation process for messageId: {}", messageId))
                    .doOnError(e -> log.error("Error occurred in email validation for messageId: {}", messageId, e));
    }

    public Mono<EmailValidationResult> fallback(Throwable t) {
        return Mono.defer(() ->
                Mono.justOrEmpty(t instanceof TimeoutException
                                ? new EmailValidationResult("UNKOWN", "0.0") // Respuesta por timeout
                                : null)
                        .switchIfEmpty(Mono.error(t))  // Si no es timeout, lanza el error
        );
    }

    private Mono<Throwable> buildErrorResponse(ClientResponse response, TechnicalMessage technicalMessage) {
        return response.bodyToMono(String.class)
                .defaultIfEmpty(Constants.NO_ADITIONAL_ERROR_DETAILS)
                .flatMap(errorBody -> {
                    log.error(Constants.STRING_ERROR_BODY_DATA, errorBody);
                    return Mono.error(
                            response.statusCode().is5xxServerError() ?
                                    new TechnicalException(technicalMessage):
                                    new BusinessException(technicalMessage));
                });
    }
}
