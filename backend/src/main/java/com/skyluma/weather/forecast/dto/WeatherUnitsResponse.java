package com.skyluma.weather.forecast.dto;

public record WeatherUnitsResponse(
        String temperature,
        String precipitationProbability,
        String precipitationAmount,
        String windSpeed,
        String windDirection,
        String humidity,
        String pressure
) {
    public static final WeatherUnitsResponse METRIC =
            new WeatherUnitsResponse("°C", "%", "mm", "m/s", "°", "%", "hPa");
}
