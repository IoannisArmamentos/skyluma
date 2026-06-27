package com.skyluma.weather.forecast.dto;

public record WeatherAlertResponse(
        String senderName,
        String event,
        long start,
        long end,
        String description
) {
}