package com.skyluma.weather.config;

import com.skyluma.weather.forecast.provider.WeatherProviderType;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "skyluma.weather")
public record WeatherProviderProperties(
        @NotNull(message = "Weather provider must not be null")
        WeatherProviderType provider
) {
}
