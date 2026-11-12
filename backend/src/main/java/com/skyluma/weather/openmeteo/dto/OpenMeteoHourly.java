package com.skyluma.weather.openmeteo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record OpenMeteoHourly(
        List<Long> time,
        @JsonProperty("temperature_2m") List<Double> temperature,
        @JsonProperty("apparent_temperature") List<Double> feelsLike,
        @JsonProperty("precipitation_probability") List<Integer> precipitationProbability,
        List<Double> precipitation,
        @JsonProperty("wind_speed_10m") List<Double> windSpeed,
        @JsonProperty("wind_gusts_10m") List<Double> windGusts,
        @JsonProperty("wind_direction_10m") List<Integer> windDirection,
        @JsonProperty("relative_humidity_2m") List<Integer> humidity,
        @JsonProperty("surface_pressure") List<Double> pressure,
        @JsonProperty("weather_code") List<Integer> weatherCode
) {
}
