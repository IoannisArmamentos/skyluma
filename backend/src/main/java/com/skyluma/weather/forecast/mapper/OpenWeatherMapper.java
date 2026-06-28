package com.skyluma.weather.forecast.mapper;

import com.skyluma.weather.forecast.dto.CurrentWeatherResponse;
import com.skyluma.weather.forecast.dto.DailyForecastResponse;
import com.skyluma.weather.forecast.dto.LocationResponse;
import com.skyluma.weather.forecast.dto.WeatherAlertResponse;
import com.skyluma.weather.forecast.dto.WeatherResponse;
import com.skyluma.weather.openweather.client.OpenWeatherClientException;
import com.skyluma.weather.openweather.dto.OpenWeatherAlert;
import com.skyluma.weather.openweather.dto.OpenWeatherCondition;
import com.skyluma.weather.openweather.dto.OpenWeatherCurrent;
import com.skyluma.weather.openweather.dto.OpenWeatherDaily;
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
        if (openWeatherResponse.current() == null) {
            throw new OpenWeatherClientException("OpenWeather response is missing current weather data");
        }

        OpenWeatherCurrent current = openWeatherResponse.current();
        OpenWeatherCondition currentCondition = getFirstCondition(current.weather());

        return new WeatherResponse(
                new LocationResponse(latitude, longitude),
                new CurrentWeatherResponse(
                        current.temp(),
                        current.feels_like(),
                        current.humidity(),
                        currentCondition == null ? null : currentCondition.description(),
                        currentCondition == null ? null : currentCondition.icon()
                ),
                mapDailyForecasts(openWeatherResponse.daily()),
                mapAlerts(openWeatherResponse.alerts())
        );
    }

    private List<DailyForecastResponse> mapDailyForecasts(List<OpenWeatherDaily> dailyForecasts) {
        if (dailyForecasts == null) {
            return List.of();
        }

        return dailyForecasts.stream()
                .map(this::mapDailyForecast)
                .toList();
    }

    private DailyForecastResponse mapDailyForecast(OpenWeatherDaily dailyForecast) {
        OpenWeatherCondition condition = getFirstCondition(dailyForecast.weather());

        return new DailyForecastResponse(
                dailyForecast.dt(),
                dailyForecast.temp().min(),
                dailyForecast.temp().max(),
                dailyForecast.humidity(),
                condition == null ? null : condition.description(),
                condition == null ? null : condition.icon()
        );
    }

    private List<WeatherAlertResponse> mapAlerts(List<OpenWeatherAlert> alerts) {
        if (alerts == null) {
            return List.of();
        }

        return alerts.stream()
                .map(this::mapAlert)
                .toList();
    }

    private WeatherAlertResponse mapAlert(OpenWeatherAlert alert) {
        return new WeatherAlertResponse(
                alert.sender_name(),
                alert.event(),
                alert.start(),
                alert.end(),
                alert.description()
        );
    }

    private OpenWeatherCondition getFirstCondition(List<OpenWeatherCondition> conditions) {
        if (conditions == null || conditions.isEmpty()) {
            return null;
        }

        return conditions.getFirst();
    }
}