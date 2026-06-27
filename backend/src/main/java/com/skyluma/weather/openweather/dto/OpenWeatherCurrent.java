package com.skyluma.weather.openweather.dto;

import java.util.List;

public record OpenWeatherCurrent(
        double temp,
        double feels_like,
        int humidity,
        List<OpenWeatherCondition> weather
) {
}

