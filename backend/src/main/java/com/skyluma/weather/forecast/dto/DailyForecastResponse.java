package com.skyluma.weather.forecast.dto;

import java.time.Instant;

public record DailyForecastResponse(
        Instant dateTime,
        double minTemperature,
        double maxTemperature,
        Integer humidity,
        String description,
        String icon
) {
}