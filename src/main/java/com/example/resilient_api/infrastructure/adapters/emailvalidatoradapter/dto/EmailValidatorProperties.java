package com.example.resilient_api.infrastructure.adapters.emailvalidatoradapter.dto;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties("email-validator")
public class EmailValidatorProperties {
    private String baseUrl;
    private String apiKey;
    private String timeout;
}
