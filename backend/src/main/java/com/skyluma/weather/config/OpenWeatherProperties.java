package com.skyluma.weather.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
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
        String language
) {
}