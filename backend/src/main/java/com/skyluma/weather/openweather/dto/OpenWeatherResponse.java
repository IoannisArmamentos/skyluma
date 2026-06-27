package com.skyluma.weather.openweather.dto;

import java.util.List;

public record OpenWeatherResponse(
        double lat,
        double lon,
        OpenWeatherCurrent current,
        List<OpenWeatherDaily> daily
) {
}