package com.skyluma.weather.openweather.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record OpenWeatherCurrent(
        double temp,

        @JsonProperty("feels_like")
        double feelsLike,

        int humidity,
        List<OpenWeatherCondition> weather
) {
}