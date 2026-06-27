package com.skyluma.weather.forecast.dto;

public record DailyForecastResponse(
        long timestamp,
        double minTemperature,
        double maxTemperature,
        int humidity,
        String description,
        String icon
) {
}