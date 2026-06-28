package com.skyluma.weather.forecast.service;

import com.skyluma.weather.config.WeatherProviderProperties;
import com.skyluma.weather.forecast.dto.WeatherResponse;
import com.skyluma.weather.forecast.provider.WeatherProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WeatherService {

    private final List<WeatherProvider> weatherProviders;
    private final WeatherProviderProperties properties;

    public WeatherResponse getWeather(double latitude, double longitude) {
        WeatherProvider weatherProvider = weatherProviders.stream()
                .filter(provider -> provider.type() == properties.provider())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Configured weather provider is not available: " + properties.provider()
                ));

        return weatherProvider.getWeather(latitude, longitude);
    }
}