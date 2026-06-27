package com.skyluma.weather.forecast.service;

import com.skyluma.weather.forecast.dto.WeatherResponse;
import com.skyluma.weather.forecast.mapper.OpenWeatherMapper;
import com.skyluma.weather.openweather.client.OpenWeatherClient;
import com.skyluma.weather.openweather.dto.OpenWeatherResponse;
import org.springframework.stereotype.Service;

@Service
public class WeatherService {

    private final OpenWeatherClient openWeatherClient;
    private final OpenWeatherMapper openWeatherMapper;

    public WeatherService(OpenWeatherClient openWeatherClient, OpenWeatherMapper openWeatherMapper) {
        this.openWeatherClient = openWeatherClient;
        this.openWeatherMapper = openWeatherMapper;
    }

    public WeatherResponse getWeather(double latitude, double longitude) {
        OpenWeatherResponse openWeatherResponse = openWeatherClient.getWeather(latitude, longitude);

        return openWeatherMapper.toWeatherResponse(openWeatherResponse, latitude, longitude);
    }
}