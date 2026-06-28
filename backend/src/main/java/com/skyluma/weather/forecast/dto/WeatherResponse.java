package com.skyluma.weather.forecast.dto;

import java.util.List;

public record WeatherResponse(
        String provider,
        LocationResponse location,
        CurrentWeatherResponse current,
        List<DailyForecastResponse> daily,
        List<WeatherAlertResponse> alerts
) {
}