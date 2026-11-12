package com.skyluma.weather.openmeteo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record OpenMeteoCurrent(
        String time,

        @JsonProperty("temperature_2m")
        Double temperature,

        @JsonProperty("apparent_temperature")
        Double apparentTemperature,

        @JsonProperty("relative_humidity_2m")
        Integer relativeHumidity,

        @JsonProperty("weather_code")
        Integer weatherCode,
        @JsonProperty("wind_speed_10m") Double windSpeed,
        @JsonProperty("wind_gusts_10m") Double windGusts,
        @JsonProperty("wind_direction_10m") Integer windDirection,
        @JsonProperty("surface_pressure") Double pressure
) {
    public OpenMeteoCurrent(String time, Double temperature, Double apparentTemperature,
                            Integer relativeHumidity, Integer weatherCode) {
        this(time, temperature, apparentTemperature, relativeHumidity, weatherCode,
                null, null, null, null);
    }
}
