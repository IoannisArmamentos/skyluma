package com.skyluma.weather.config;

import jakarta.validation.constraints.NotBlank;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "openmeteo")
public record OpenMeteoProperties(
        @NotBlank(message = "Open-Meteo base URL must not be blank")
        String baseUrl,

        @DefaultValue("3s")
        @DurationUnit(ChronoUnit.SECONDS)
        Duration connectTimeout,

        @DefaultValue("10s")
        @DurationUnit(ChronoUnit.SECONDS)
        Duration readTimeout
) {
    public OpenMeteoProperties {
        if (connectTimeout == null || connectTimeout.isZero() || connectTimeout.isNegative()
                || readTimeout == null || readTimeout.isZero() || readTimeout.isNegative()) {
            throw new IllegalArgumentException("Open-Meteo timeouts must be positive");
        }
    }
}
