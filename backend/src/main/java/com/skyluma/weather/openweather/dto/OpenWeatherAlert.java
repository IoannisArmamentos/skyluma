package com.skyluma.weather.openweather.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record OpenWeatherAlert(
        @JsonProperty("sender_name")
        String senderName,

        String event,
        long start,
        long end,
        String description
) {
}