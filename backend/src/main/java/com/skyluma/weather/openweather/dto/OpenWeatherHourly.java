package com.skyluma.weather.openweather.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

public record OpenWeatherHourly(
        Long dt,
        Double temp,
        @JsonProperty("feels_like") Double feelsLike,
        Double pop,
        Map<String, Double> rain,
        Map<String, Double> snow,
        @JsonProperty("wind_speed") Double windSpeed,
        @JsonProperty("wind_gust") Double windGusts,
        @JsonProperty("wind_deg") Integer windDirection,
        Integer humidity,
        Double pressure,
        List<OpenWeatherCondition> weather
) {
}
