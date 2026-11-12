package com.skyluma.weather.forecast.dto;

/** Provider support, not a guarantee that every response contains each measurement. */
public record WeatherCapabilitiesResponse(
        boolean supportsHourlyForecast,
        boolean supportsAlerts,
        boolean supportsSunriseSunset
) {
    public static final WeatherCapabilitiesResponse OPEN_METEO =
            new WeatherCapabilitiesResponse(true, false, true);
    public static final WeatherCapabilitiesResponse OPEN_WEATHER =
            new WeatherCapabilitiesResponse(true, true, true);
}
