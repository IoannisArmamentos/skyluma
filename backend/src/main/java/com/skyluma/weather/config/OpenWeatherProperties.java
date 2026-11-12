package com.skyluma.weather.config;

import jakarta.validation.constraints.NotBlank;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "openweather")
public record OpenWeatherProperties(
        @NotBlank(message = "OpenWeather base URL must not be blank")
        String baseUrl,

        String apiKey,

        @NotBlank(message = "OpenWeather units must not be blank")
        String units,

        @NotBlank(message = "OpenWeather language must not be blank")
        String language,

        @DefaultValue("3s")
        @DurationUnit(ChronoUnit.SECONDS)
        Duration connectTimeout,

        @DefaultValue("10s")
        @DurationUnit(ChronoUnit.SECONDS)
        Duration readTimeout
) {
    public OpenWeatherProperties {
        if (!"metric".equals(units)) {
            throw new IllegalArgumentException("OpenWeather units must be metric for the normalized API");
        }
        if (connectTimeout == null || connectTimeout.isZero() || connectTimeout.isNegative()
                || readTimeout == null || readTimeout.isZero() || readTimeout.isNegative()) {
            throw new IllegalArgumentException("OpenWeather timeouts must be positive");
        }
    }
}
