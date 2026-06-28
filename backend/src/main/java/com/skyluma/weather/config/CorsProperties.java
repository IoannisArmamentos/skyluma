package com.skyluma.weather.config;

import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Validated
@ConfigurationProperties(prefix = "skyluma.cors")
public record CorsProperties(
        @NotEmpty(message = "CORS allowed origins must not be empty")
        List<String> allowedOrigins
) {
}