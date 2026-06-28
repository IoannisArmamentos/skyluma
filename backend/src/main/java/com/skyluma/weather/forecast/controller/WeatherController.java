package com.skyluma.weather.forecast.controller;

import com.skyluma.weather.forecast.dto.WeatherResponse;
import com.skyluma.weather.forecast.provider.WeatherProviderType;
import com.skyluma.weather.forecast.service.WeatherService;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
public class WeatherController {

    private final WeatherService weatherService;

    @GetMapping("/api/weather")
    public WeatherResponse getWeather(
            @RequestParam
            @DecimalMin(value = "-90.0", message = "Latitude must be greater than or equal to -90")
            @DecimalMax(value = "90.0", message = "Latitude must be less than or equal to 90")
            double latitude,

            @RequestParam
            @DecimalMin(value = "-180.0", message = "Longitude must be greater than or equal to -180")
            @DecimalMax(value = "180.0", message = "Longitude must be less than or equal to 180")
            double longitude,

            @RequestParam(required = false)
            String provider
    ) {
        return weatherService.getWeather(latitude, longitude, WeatherProviderType.fromValue(provider));
    }
}