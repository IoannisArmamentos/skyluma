package com.skyluma.weather.forecast.dto;

import java.time.Instant;

public record DailyForecastResponse(
        Instant dateTime,
        double minTemperature,
        double maxTemperature,
        int humidity,
        String description,
        String icon
) {
}