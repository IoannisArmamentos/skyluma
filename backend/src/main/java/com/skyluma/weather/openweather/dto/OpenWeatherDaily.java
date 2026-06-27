package com.skyluma.weather.openweather.dto;

import java.util.List;

public record OpenWeatherDaily(
        long dt,
        OpenWeatherTemperature temp,
        int humidity,
        List<OpenWeatherCondition> weather
) {
}