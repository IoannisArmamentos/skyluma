package com.skyluma.weather.forecast.dto;

import java.time.Instant;
import java.time.LocalDate;

public record DailyForecastResponse(
        Instant dateTime,
        double minTemperature,
        double maxTemperature,
        Integer humidity,
        String description,
        String icon,
        LocalDate date,
        Instant sunrise,
        Instant sunset
) {
    public DailyForecastResponse(Instant dateTime, double minTemperature, double maxTemperature,
                                 Integer humidity, String description, String icon, LocalDate date) {
        this(dateTime, minTemperature, maxTemperature, humidity, description, icon, date, null, null);
    }
}
