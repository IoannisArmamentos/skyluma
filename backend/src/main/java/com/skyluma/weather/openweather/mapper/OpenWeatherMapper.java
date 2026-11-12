package com.skyluma.weather.openweather.mapper;

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

import java.time.Instant;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.time.ZoneOffset;
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
                "openweather",
                new LocationResponse(latitude, longitude),
                new CurrentWeatherResponse(
                        current.temp(),
                        current.feelsLike(),
                        current.humidity(),
                        currentCondition == null ? null : currentCondition.description(),
                        currentCondition == null ? null : currentCondition.icon()
                ),
                mapDailyForecasts(openWeatherResponse),
                mapAlerts(openWeatherResponse.alerts())
        );
    }

    private List<DailyForecastResponse> mapDailyForecasts(OpenWeatherResponse response) {
        List<OpenWeatherDaily> dailyForecasts = response.daily();
        if (dailyForecasts == null || dailyForecasts.isEmpty()) {
            return List.of();
        }

        ZoneId timezone = locationTimezone(response);
        return dailyForecasts.stream()
                .map(day -> mapDailyForecast(day, timezone))
                .toList();
    }

    private ZoneId locationTimezone(OpenWeatherResponse response) {
        try {
            if (response.timezone() != null && !response.timezone().isBlank()) {
                return ZoneId.of(response.timezone());
            }
            if (response.timezoneOffset() != null) {
                return ZoneOffset.ofTotalSeconds(response.timezoneOffset());
            }
        } catch (DateTimeException exception) {
            throw new OpenWeatherClientException("OpenWeather response has invalid timezone data");
        }
        throw new OpenWeatherClientException("OpenWeather response is missing timezone data");
    }

    private DailyForecastResponse mapDailyForecast(OpenWeatherDaily dailyForecast, ZoneId timezone) {
        OpenWeatherCondition condition = getFirstCondition(dailyForecast.weather());
        Instant timestamp = Instant.ofEpochSecond(dailyForecast.dt());

        return new DailyForecastResponse(
                timestamp,
                dailyForecast.temp().min(),
                dailyForecast.temp().max(),
                dailyForecast.humidity(),
                condition == null ? null : condition.description(),
                condition == null ? null : condition.icon(),
                timestamp.atZone(timezone).toLocalDate()
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
                alert.senderName(),
                alert.event(),
                Instant.ofEpochSecond(alert.start()),
                Instant.ofEpochSecond(alert.end()),
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
