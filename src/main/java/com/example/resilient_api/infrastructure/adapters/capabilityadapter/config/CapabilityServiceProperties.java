package com.example.resilient_api.infrastructure.adapters.capabilityadapter.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties("capability-service")
public class CapabilityServiceProperties {
    private String baseUrl;
    private String timeout;
}
