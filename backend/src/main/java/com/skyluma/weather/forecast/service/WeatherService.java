package com.skyluma.weather.forecast.service;

import com.skyluma.weather.forecast.dto.CurrentWeatherResponse;
import com.skyluma.weather.forecast.dto.LocationResponse;
import com.skyluma.weather.forecast.dto.WeatherResponse;
import org.springframework.stereotype.Service;

@Service
public class WeatherService {

    public WeatherResponse getWeather(double latitude, double longitude) {
        return new WeatherResponse(
                new LocationResponse(latitude, longitude),
                new CurrentWeatherResponse(
                        21.5,
                        20.8,
                        65,
                        "clear sky",
                        "01d"
                )
        );
    }
}
