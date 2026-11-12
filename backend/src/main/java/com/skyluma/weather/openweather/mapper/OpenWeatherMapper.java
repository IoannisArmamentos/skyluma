package com.skyluma.weather.openweather.mapper;

import com.skyluma.weather.forecast.dto.CurrentWeatherResponse;
import com.skyluma.weather.forecast.dto.WeatherCapabilitiesResponse;
import com.skyluma.weather.forecast.dto.DailyForecastResponse;
import com.skyluma.weather.forecast.dto.HourlyForecastResponse;
import com.skyluma.weather.forecast.dto.LocationResponse;
import com.skyluma.weather.forecast.dto.WeatherAlertResponse;
import com.skyluma.weather.forecast.dto.WeatherResponse;
import com.skyluma.weather.forecast.dto.WeatherUnitsResponse;
import com.skyluma.weather.openweather.client.OpenWeatherClientException;
import com.skyluma.weather.openweather.dto.OpenWeatherAlert;
import com.skyluma.weather.openweather.dto.OpenWeatherCondition;
import com.skyluma.weather.openweather.dto.OpenWeatherCurrent;
import com.skyluma.weather.openweather.dto.OpenWeatherDaily;
import com.skyluma.weather.openweather.dto.OpenWeatherHourly;
import com.skyluma.weather.openweather.dto.OpenWeatherResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Component
public class OpenWeatherMapper {

    public WeatherResponse toWeatherResponse(
            OpenWeatherResponse openWeatherResponse,
            double latitude,
            double longitude
    ) {
        if (openWeatherResponse == null || openWeatherResponse.current() == null) {
            throw new OpenWeatherClientException("OpenWeather response is missing current weather data");
        }

        OpenWeatherCurrent current = openWeatherResponse.current();
        if (current.temp() == null || current.feelsLike() == null || current.humidity() == null) {
            throw new OpenWeatherClientException("OpenWeather response has incomplete current weather data");
        }
        OpenWeatherCondition currentCondition = getFirstCondition(current.weather());

        return new WeatherResponse(
                "openweather",
                new LocationResponse(latitude, longitude),
                new CurrentWeatherResponse(
                        current.temp(),
                        current.feelsLike(),
                        current.humidity(),
                        currentCondition == null ? null : currentCondition.description(),
                        currentCondition == null ? null : currentCondition.icon(),
                        current.windSpeed(), current.windGusts(), current.windDirection(), current.pressure(),
                        toInstant(current.sunrise()), toInstant(current.sunset())
                ),
                mapDailyForecasts(openWeatherResponse),
                mapAlerts(openWeatherResponse.alerts()),
                openWeatherResponse.timezone(),
                openWeatherResponse.timezoneOffset(),
                WeatherUnitsResponse.METRIC,
                mapHourlyForecasts(openWeatherResponse.hourly()),
                WeatherCapabilitiesResponse.OPEN_WEATHER
        );
    }

    private List<DailyForecastResponse> mapDailyForecasts(OpenWeatherResponse response) {
        List<OpenWeatherDaily> dailyForecasts = response.daily();
        if (dailyForecasts == null || dailyForecasts.isEmpty()) {
            return List.of();
        }

        ZoneId timezone = locationTimezone(response);
        List<DailyForecastResponse> forecast = dailyForecasts.stream()
                .map(day -> mapDailyForecast(day, timezone))
                .filter(Objects::nonNull)
                .toList();

        if (forecast.size() < dailyForecasts.size()) {
            log.warn("Skipped {} incomplete OpenWeather daily forecast entries", dailyForecasts.size() - forecast.size());
        }
        return forecast;
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

    // Returns null for a day without a timestamp or temperatures, so the other days are kept.
    // Humidity is optional and stays null when the provider omits it.
    private DailyForecastResponse mapDailyForecast(OpenWeatherDaily dailyForecast, ZoneId timezone) {
        if (dailyForecast == null || dailyForecast.dt() == null || dailyForecast.temp() == null
                || dailyForecast.temp().min() == null || dailyForecast.temp().max() == null) {
            return null;
        }
        OpenWeatherCondition condition = getFirstCondition(dailyForecast.weather());
        Instant timestamp = toInstant(dailyForecast.dt());
        if (timestamp == null) {
            return null;
        }

        return new DailyForecastResponse(
                timestamp,
                dailyForecast.temp().min(),
                dailyForecast.temp().max(),
                dailyForecast.humidity(),
                condition == null ? null : condition.description(),
                condition == null ? null : condition.icon(),
                timestamp.atZone(timezone).toLocalDate(),
                toInstant(dailyForecast.sunrise()),
                toInstant(dailyForecast.sunset())
        );
    }

    private List<HourlyForecastResponse> mapHourlyForecasts(List<OpenWeatherHourly> hourly) {
        if (hourly == null) {
            return List.of();
        }
        return hourly.stream()
                .map(this::mapHourlyForecast)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(HourlyForecastResponse::time))
                .limit(48)
                .toList();
    }

    private HourlyForecastResponse mapHourlyForecast(OpenWeatherHourly hour) {
        if (hour == null || hour.dt() == null) {
            return null;
        }
        Instant timestamp = toInstant(hour.dt());
        if (timestamp == null) {
            return null;
        }
        OpenWeatherCondition condition = getFirstCondition(hour.weather());
        return new HourlyForecastResponse(timestamp, hour.temp(), hour.feelsLike(),
                probabilityPercent(hour.pop()), precipitationAmount(hour.rain(), hour.snow()),
                hour.windSpeed(), hour.windGusts(), hour.windDirection(), hour.humidity(), hour.pressure(),
                condition == null ? null : condition.description(), condition == null ? null : condition.icon());
    }

    private Integer probabilityPercent(Double probability) {
        return probability == null || !Double.isFinite(probability) || probability < 0 || probability > 1
                ? null : (int) Math.round(probability * 100);
    }

    private Double precipitationAmount(Map<String, Double> rain, Map<String, Double> snow) {
        Double rainAmount = rain == null ? null : rain.get("1h");
        Double snowAmount = snow == null ? null : snow.get("1h");
        if (rainAmount == null) {
            return snowAmount;
        }
        return snowAmount == null ? rainAmount : rainAmount + snowAmount;
    }

    private Instant toInstant(Long seconds) {
        if (seconds == null) {
            return null;
        }
        try {
            return Instant.ofEpochSecond(seconds);
        } catch (DateTimeException exception) {
            return null;
        }
    }

    private List<WeatherAlertResponse> mapAlerts(List<OpenWeatherAlert> alerts) {
        if (alerts == null) {
            return List.of();
        }

        List<WeatherAlertResponse> mappedAlerts = alerts.stream()
                .map(this::mapAlert)
                .filter(Objects::nonNull)
                .toList();

        if (mappedAlerts.size() < alerts.size()) {
            log.warn("Skipped {} incomplete OpenWeather alerts", alerts.size() - mappedAlerts.size());
        }
        return mappedAlerts;
    }

    // Returns null for an alert without start or end, so the remaining alerts and forecast are kept.
    private WeatherAlertResponse mapAlert(OpenWeatherAlert alert) {
        if (alert == null || alert.start() == null || alert.end() == null) {
            return null;
        }
        return new WeatherAlertResponse(
                alert.senderName(),
                alert.event(),
                Instant.ofEpochSecond(alert.start()),
                Instant.ofEpochSecond(alert.end()),
                alert.description()
        );
    }

    private OpenWeatherCondition getFirstCondition(List<OpenWeatherCondition> conditions) {
        if (conditions == null) {
            return null;
        }

        return conditions.stream()
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }
}
