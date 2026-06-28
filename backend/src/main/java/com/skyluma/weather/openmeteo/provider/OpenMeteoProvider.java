package com.skyluma.weather.openmeteo.provider;

import com.skyluma.weather.forecast.dto.WeatherResponse;
import com.skyluma.weather.forecast.provider.WeatherProvider;
import com.skyluma.weather.openmeteo.client.OpenMeteoClient;
import com.skyluma.weather.openmeteo.dto.OpenMeteoResponse;
import com.skyluma.weather.openmeteo.mapper.OpenMeteoMapper;
import org.springframework.stereotype.Component;

@Component
public class OpenMeteoProvider implements WeatherProvider {

    private final OpenMeteoClient openMeteoClient;
    private final OpenMeteoMapper openMeteoMapper;

    public OpenMeteoProvider(
            OpenMeteoClient openMeteoClient,
            OpenMeteoMapper openMeteoMapper
    ) {
        this.openMeteoClient = openMeteoClient;
        this.openMeteoMapper = openMeteoMapper;
    }

    @Override
    public WeatherResponse getWeather(double latitude, double longitude) {
        OpenMeteoResponse openMeteoResponse = openMeteoClient.getWeather(latitude, longitude);
        return openMeteoMapper.toWeatherResponse(openMeteoResponse, latitude, longitude);
    }
}
