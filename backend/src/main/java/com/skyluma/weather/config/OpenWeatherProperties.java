package com.skyluma.weather.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "openweather")
public record OpenWeatherProperties(
        String baseUrl,
        String apiKey,
        String units,
        String language
) {
}