package com.skyluma.weather.forecast.mapper;

import com.skyluma.weather.forecast.dto.CurrentWeatherResponse;
import com.skyluma.weather.forecast.dto.LocationResponse;
import com.skyluma.weather.forecast.dto.WeatherResponse;
import com.skyluma.weather.openweather.dto.OpenWeatherCondition;
import com.skyluma.weather.openweather.dto.OpenWeatherCurrent;
import com.skyluma.weather.openweather.dto.OpenWeatherResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OpenWeatherMapper {

    public WeatherResponse toWeatherResponse(
            OpenWeatherResponse openWeatherResponse,
            double latitude,
            double longitude
    ) {
        OpenWeatherCurrent current = openWeatherResponse.current();
        OpenWeatherCondition condition = getFirstCondition(current.weather());

        return new WeatherResponse(
                new LocationResponse(latitude, longitude),
                new CurrentWeatherResponse(
                        current.temp(),
                        current.feels_like(),
                        current.humidity(),
                        condition == null ? null : condition.description(),
                        condition == null ? null : condition.icon()
                )
        );
    }

    private OpenWeatherCondition getFirstCondition(List<OpenWeatherCondition> conditions) {
        if (conditions == null || conditions.isEmpty()) {
            return null;
        }

        return conditions.get(0);
    }
}
