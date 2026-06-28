package com.skyluma.weather.forecast.provider;

import com.skyluma.weather.forecast.dto.WeatherResponse;

public interface WeatherProvider {

    WeatherProviderType type();

    WeatherResponse getWeather(double latitude, double longitude);
}