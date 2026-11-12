package com.skyluma.weather.forecast.dto;

/** All pressure values in this response refer to mean sea-level pressure. */
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
