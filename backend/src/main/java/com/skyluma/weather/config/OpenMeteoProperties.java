package com.skyluma.weather.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "openmeteo")
public record OpenMeteoProperties(
        @NotBlank(message = "Open-Meteo base URL must not be blank")
        String baseUrl
) {
}
