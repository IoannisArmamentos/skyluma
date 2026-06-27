package com.skyluma.weather.openweather.dto;

public record OpenWeatherResponse(
        double lat,
        double lon,
        OpenWeatherCurrent current
) {
}
