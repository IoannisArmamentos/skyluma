package com.skyluma.weather.forecast.dto;

public record WeatherResponse(
        LocationResponse location,
        CurrentWeatherResponse current
) {
}
