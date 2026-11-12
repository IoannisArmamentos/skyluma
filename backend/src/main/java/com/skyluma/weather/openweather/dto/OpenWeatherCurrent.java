package com.skyluma.weather.openweather.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record OpenWeatherCurrent(
        Double temp,

        @JsonProperty("feels_like")
        Double feelsLike,

        Integer humidity,
        List<OpenWeatherCondition> weather,
        @JsonProperty("wind_speed") Double windSpeed,
        @JsonProperty("wind_gust") Double windGusts,
        @JsonProperty("wind_deg") Integer windDirection,
        Double pressure,
        Long sunrise,
        Long sunset
) {
    public OpenWeatherCurrent(Double temp, Double feelsLike, Integer humidity,
                              List<OpenWeatherCondition> weather) {
        this(temp, feelsLike, humidity, weather, null, null, null, null, null, null);
    }
}
