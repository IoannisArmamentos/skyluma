package com.skyluma.weather.forecast.dto;

import java.util.List;

/**
 * @param timezone IANA location timezone for converting forecast instants to local time
 * @param utcOffsetSeconds provider's current/reference UTC offset, which can differ at future DST boundaries
 */
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
