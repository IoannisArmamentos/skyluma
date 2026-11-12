package com.skyluma.weather.forecast.dto;

import java.time.Instant;

public record CurrentWeatherResponse(
        double temperature,
        double feelsLike,
        int humidity,
        String description,
        String icon,
        Double windSpeed,
        Double windGusts,
        Integer windDirection,
        Double pressure,
        Instant sunrise,
        Instant sunset
) {
    public CurrentWeatherResponse(double temperature, double feelsLike, int humidity,
                                  String description, String icon) {
        this(temperature, feelsLike, humidity, description, icon, null, null, null, null, null, null);
    }
}
