package com.example.resilient_api.infrastructure.adapters.emailvalidatoradapter.config;

import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RetryConfiguration {

    private final RetryRegistry retryRegistry;

    public RetryConfiguration(RetryRegistry retryRegistry) {
        this.retryRegistry = retryRegistry;
    }

    @Bean
    public Retry retryPolicy() {
        return retryRegistry.retry("emailValidatorRetry");
    }

}
