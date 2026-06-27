package com.skyluma.weather.forecast.dto;

import java.util.List;

public record WeatherResponse(
        LocationResponse location,
        CurrentWeatherResponse current,
        List<DailyForecastResponse> daily
) {
}