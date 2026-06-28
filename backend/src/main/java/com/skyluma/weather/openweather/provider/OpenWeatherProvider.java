package com.skyluma.weather.openweather.provider;

import com.skyluma.weather.forecast.dto.WeatherResponse;
import com.skyluma.weather.openweather.mapper.OpenWeatherMapper;
import com.skyluma.weather.forecast.provider.WeatherProvider;
import com.skyluma.weather.forecast.provider.WeatherProviderType;
import com.skyluma.weather.openweather.client.OpenWeatherClient;
import com.skyluma.weather.openweather.dto.OpenWeatherResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OpenWeatherProvider implements WeatherProvider {

    private final OpenWeatherClient openWeatherClient;
    private final OpenWeatherMapper openWeatherMapper;

    @Override
    public WeatherProviderType type() {
        return WeatherProviderType.OPENWEATHER;
    }

    @Override
    public WeatherResponse getWeather(double latitude, double longitude) {
        OpenWeatherResponse openWeatherResponse = openWeatherClient.getWeather(latitude, longitude);
        return openWeatherMapper.toWeatherResponse(openWeatherResponse, latitude, longitude);
    }
}