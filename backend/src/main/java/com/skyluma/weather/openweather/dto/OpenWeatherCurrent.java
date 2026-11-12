package com.skyluma.weather.openweather.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record OpenWeatherCurrent(
        Double temp,

        @JsonProperty("feels_like")
        Double feelsLike,

        Integer humidity,
        List<OpenWeatherCondition> weather
) {
}
