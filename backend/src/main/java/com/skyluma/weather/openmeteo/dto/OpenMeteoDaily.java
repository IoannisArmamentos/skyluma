package com.skyluma.weather.openmeteo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record OpenMeteoDaily(
        List<?> time,

        @JsonProperty("temperature_2m_min")
        List<Double> temperatureMin,

        @JsonProperty("temperature_2m_max")
        List<Double> temperatureMax,

        @JsonProperty("weather_code")
        List<Integer> weatherCode,
        List<Long> sunrise,
        List<Long> sunset
) {
    public OpenMeteoDaily(List<?> time, List<Double> temperatureMin, List<Double> temperatureMax,
                          List<Integer> weatherCode) {
        this(time, temperatureMin, temperatureMax, weatherCode, null, null);
    }
}
