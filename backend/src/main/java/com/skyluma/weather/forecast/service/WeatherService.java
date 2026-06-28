package com.skyluma.weather.forecast.service;

import com.skyluma.weather.config.WeatherProviderProperties;
import com.skyluma.weather.forecast.dto.WeatherResponse;
import com.skyluma.weather.forecast.provider.WeatherProvider;
import com.skyluma.weather.forecast.provider.WeatherProviderType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WeatherService {

    private final List<WeatherProvider> weatherProviders;
    private final WeatherProviderProperties properties;

    public WeatherResponse getWeather(
            double latitude,
            double longitude,
            WeatherProviderType provider
    ) {
        WeatherProviderType selectedProvider = provider == null
                ? properties.provider()
                : provider;

        WeatherProvider weatherProvider = weatherProviders.stream()
                .filter(candidate -> candidate.type() == selectedProvider)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Configured weather provider is not available: " + selectedProvider
                ));

        return weatherProvider.getWeather(latitude, longitude);
    }
}