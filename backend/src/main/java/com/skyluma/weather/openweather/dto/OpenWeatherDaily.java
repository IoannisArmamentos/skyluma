package com.skyluma.weather.openweather.dto;

import java.util.List;

public record OpenWeatherDaily(
        Long dt,
        OpenWeatherTemperature temp,
        Integer humidity,
        List<OpenWeatherCondition> weather,
        Long sunrise,
        Long sunset
) {
    public OpenWeatherDaily(Long dt, OpenWeatherTemperature temp, Integer humidity,
                            List<OpenWeatherCondition> weather) {
        this(dt, temp, humidity, weather, null, null);
    }
}
