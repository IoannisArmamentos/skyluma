package com.skyluma.weather.openweather.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record OpenWeatherResponse(
        double lat,
        double lon,
        String timezone,
        @JsonProperty("timezone_offset") Integer timezoneOffset,
        OpenWeatherCurrent current,
        List<OpenWeatherDaily> daily,
        List<OpenWeatherAlert> alerts
) {
}
