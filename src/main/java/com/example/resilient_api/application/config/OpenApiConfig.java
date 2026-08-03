package com.example.resilient_api.application.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Bootcamp API",
                version = "1.0.0",
                description = "Documentación de OpenAPI en los endpoints para las funcionalidades del Bootcamp"
        )
)
public class OpenApiConfig {
}