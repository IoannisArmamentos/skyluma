package com.skyluma.weather.forecast.dto;

import java.time.Instant;

public record HourlyForecastResponse(
        Instant time,
        Double temperature,
        Double feelsLike,
        Integer precipitationProbability,
        Double precipitationAmount,
        Double windSpeed,
        Double windGusts,
        Integer windDirection,
        Integer humidity,
        Double pressure,
        String description,
        String icon
) {
}
