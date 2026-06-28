package com.skyluma.weather.openweather.provider;

import com.skyluma.weather.forecast.dto.WeatherResponse;
import com.skyluma.weather.forecast.mapper.OpenWeatherMapper;
import com.skyluma.weather.forecast.provider.WeatherProvider;
import com.skyluma.weather.openweather.client.OpenWeatherClient;
import com.skyluma.weather.openweather.dto.OpenWeatherResponse;
import org.springframework.stereotype.Component;

@Component
public class OpenWeatherProvider implements WeatherProvider {

    private final OpenWeatherClient openWeatherClient;
    private final OpenWeatherMapper openWeatherMapper;

    public OpenWeatherProvider(
            OpenWeatherClient openWeatherClient,
            OpenWeatherMapper openWeatherMapper
    ) {
        this.openWeatherClient = openWeatherClient;
        this.openWeatherMapper = openWeatherMapper;
    }

    @Override
    public WeatherResponse getWeather(double latitude, double longitude) {
        OpenWeatherResponse openWeatherResponse = openWeatherClient.getWeather(latitude, longitude);
        return openWeatherMapper.toWeatherResponse(openWeatherResponse, latitude, longitude);
    }
}
