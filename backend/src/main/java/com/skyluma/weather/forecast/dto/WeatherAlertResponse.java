package com.skyluma.weather.forecast.dto;

import java.time.Instant;

public record WeatherAlertResponse(
        String senderName,
        String event,
        Instant start,
        Instant end,
        String description
) {
}