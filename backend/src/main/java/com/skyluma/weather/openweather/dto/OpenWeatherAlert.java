package com.skyluma.weather.openweather.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record OpenWeatherAlert(
        @JsonProperty("sender_name")
        String senderName,

        String event,
        Long start,
        Long end,
        String description
) {
}
