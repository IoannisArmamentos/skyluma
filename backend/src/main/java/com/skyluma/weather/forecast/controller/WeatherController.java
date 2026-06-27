package com.skyluma.weather.forecast.controller;

import com.skyluma.weather.forecast.dto.WeatherResponse;
import com.skyluma.weather.forecast.service.WeatherService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WeatherController {

    private final WeatherService weatherService;

    public WeatherController(WeatherService weatherService) {
        this.weatherService = weatherService;
    }

    @GetMapping("/api/weather")
    public WeatherResponse getWeather(
            @RequestParam double latitude,
            @RequestParam double longitude
    ) {
        return weatherService.getWeather(latitude, longitude);
    }
}

