package com.example.resilient_api.infrastructure.adapters.reportadapter.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties("report-service")
public class ReportServiceProperties {
    private String baseUrl;
    private String timeout;
}
