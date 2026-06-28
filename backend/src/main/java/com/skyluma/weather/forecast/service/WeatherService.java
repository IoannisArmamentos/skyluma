package com.skyluma.weather.forecast.service;

import com.skyluma.weather.forecast.dto.WeatherResponse;
import com.skyluma.weather.forecast.provider.WeatherProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WeatherService {

    private final WeatherProvider weatherProvider;

    public WeatherResponse getWeather(double latitude, double longitude) {
        return weatherProvider.getWeather(latitude, longitude);
    }
}