package com.skyluma.weather.openmeteo.mapper;

import com.skyluma.weather.forecast.dto.CurrentWeatherResponse;
import com.skyluma.weather.forecast.dto.DailyForecastResponse;
import com.skyluma.weather.forecast.dto.LocationResponse;
import com.skyluma.weather.forecast.dto.WeatherAlertResponse;
import com.skyluma.weather.forecast.dto.WeatherResponse;
import com.skyluma.weather.openmeteo.client.OpenMeteoClientException;
import com.skyluma.weather.openmeteo.dto.OpenMeteoCurrent;
import com.skyluma.weather.openmeteo.dto.OpenMeteoDaily;
import com.skyluma.weather.openmeteo.dto.OpenMeteoResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

@Slf4j
@Component
public class OpenMeteoMapper {

    public WeatherResponse toWeatherResponse(
            OpenMeteoResponse openMeteoResponse,
            double latitude,
            double longitude
    ) {
        if (openMeteoResponse == null || openMeteoResponse.current() == null) {
            throw new OpenMeteoClientException("Open-Meteo response is missing current weather data");
        }
        OpenMeteoCurrent current = openMeteoResponse.current();
        if (current.temperature() == null || current.apparentTemperature() == null
                || current.relativeHumidity() == null || current.weatherCode() == null) {
            throw new OpenMeteoClientException("Open-Meteo response has incomplete current weather data");
        }

        return new WeatherResponse(
                "openmeteo",
                new LocationResponse(latitude, longitude),
                toCurrentWeather(openMeteoResponse.current()),
                toDailyForecast(openMeteoResponse.daily()),
                List.of()
        );
    }

    private CurrentWeatherResponse toCurrentWeather(OpenMeteoCurrent current) {
        return new CurrentWeatherResponse(
                current.temperature(),
                current.apparentTemperature(),
                current.relativeHumidity(),
                toDescription(current.weatherCode()),
                String.valueOf(current.weatherCode())
        );
    }

    private List<DailyForecastResponse> toDailyForecast(OpenMeteoDaily daily) {
        if (daily == null || daily.time() == null || daily.temperatureMin() == null
                || daily.temperatureMax() == null || daily.weatherCode() == null) {
            return List.of();
        }

        int days = Math.min(
                Math.min(daily.time().size(), daily.temperatureMin().size()),
                Math.min(daily.temperatureMax().size(), daily.weatherCode().size())
        );
        List<DailyForecastResponse> forecast = IntStream.range(0, days)
                .mapToObj(index -> toDailyForecast(daily, index))
                .filter(Objects::nonNull)
                .toList();

        if (forecast.size() < daily.time().size()) {
            log.warn("Skipped {} incomplete Open-Meteo daily forecast entries", daily.time().size() - forecast.size());
        }
        return forecast;
    }

    // Returns null for a day with missing values or an invalid date, so the other days are kept.
    private DailyForecastResponse toDailyForecast(OpenMeteoDaily daily, int index) {
        String time = daily.time().get(index);
        Double minTemperature = daily.temperatureMin().get(index);
        Double maxTemperature = daily.temperatureMax().get(index);
        Integer weatherCode = daily.weatherCode().get(index);
        if (time == null || minTemperature == null || maxTemperature == null || weatherCode == null) {
            return null;
        }

        LocalDate date;
        try {
            date = LocalDate.parse(time);
        } catch (DateTimeParseException exception) {
            return null;
        }

        return new DailyForecastResponse(
                date.atStartOfDay().toInstant(ZoneOffset.UTC),
                minTemperature,
                maxTemperature,
                null,
                toDescription(weatherCode),
                String.valueOf(weatherCode),
                date
        );
    }

    private String toDescription(int weatherCode) {
        return switch (weatherCode) {
            case 0 -> "Clear sky";
            case 1, 2, 3 -> "Partly cloudy";
            case 45, 48 -> "Fog";
            case 51, 53, 55 -> "Drizzle";
            case 56, 57 -> "Freezing drizzle";
            case 61, 63, 65 -> "Rain";
            case 66, 67 -> "Freezing rain";
            case 71, 73, 75 -> "Snow";
            case 77 -> "Snow grains";
            case 80, 81, 82 -> "Rain showers";
            case 85, 86 -> "Snow showers";
            case 95 -> "Thunderstorm";
            case 96, 99 -> "Thunderstorm with hail";
            default -> "Unknown";
        };
    }
}
