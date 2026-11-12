package com.skyluma.weather.openweather.dto;

import java.util.List;

public record OpenWeatherDaily(
        Long dt,
        OpenWeatherTemperature temp,
        Integer humidity,
        List<OpenWeatherCondition> weather
) {
}
