package com.skyluma.weather.forecast.service;

import com.skyluma.weather.forecast.dto.WeatherResponse;
import com.skyluma.weather.forecast.provider.WeatherProvider;
import org.springframework.stereotype.Service;

@Service
public class WeatherService {

    private final WeatherProvider weatherProvider;

    public WeatherService(WeatherProvider weatherProvider) {
        this.weatherProvider = weatherProvider;
    }

    public WeatherResponse getWeather(double latitude, double longitude) {
        return weatherProvider.getWeather(latitude, longitude);
    }
}