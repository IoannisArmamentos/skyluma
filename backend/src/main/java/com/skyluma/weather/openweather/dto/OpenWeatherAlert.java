package com.skyluma.weather.openweather.dto;

public record OpenWeatherAlert(
        String sender_name,
        String event,
        long start,
        long end,
        String description
) {
}