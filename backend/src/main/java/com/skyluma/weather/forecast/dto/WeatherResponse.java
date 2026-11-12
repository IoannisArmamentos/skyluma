package com.skyluma.weather.forecast.dto;

import java.util.List;

public record WeatherResponse(
        String provider,
        LocationResponse location,
        CurrentWeatherResponse current,
        List<DailyForecastResponse> daily,
        List<WeatherAlertResponse> alerts,
        String timezone,
        Integer utcOffsetSeconds,
        WeatherUnitsResponse units,
        List<HourlyForecastResponse> hourly,
        WeatherCapabilitiesResponse capabilities
) {
    public WeatherResponse(String provider, LocationResponse location, CurrentWeatherResponse current,
                           List<DailyForecastResponse> daily, List<WeatherAlertResponse> alerts) {
        this(provider, location, current, daily, alerts, null, null, null, List.of(), null);
    }
}
