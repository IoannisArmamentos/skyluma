package com.skyluma.weather.forecast.dto;

public record CurrentWeatherResponse(
        double temperature,
        double feelsLike,
        int humidity,
        String description,
        String icon
) {
}
