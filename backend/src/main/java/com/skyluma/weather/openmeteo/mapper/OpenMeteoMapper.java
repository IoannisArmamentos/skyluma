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
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.stream.IntStream;

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
        if (daily == null) {
            return List.of();
        }
        if (daily.time() == null || daily.temperatureMin() == null || daily.temperatureMax() == null
                || daily.weatherCode() == null || daily.temperatureMin().size() != daily.time().size()
                || daily.temperatureMax().size() != daily.time().size()
                || daily.weatherCode().size() != daily.time().size()) {
            throw new OpenMeteoClientException("Open-Meteo response has incomplete daily forecast data");
        }
        try {
            return IntStream.range(0, daily.time().size())
                    .mapToObj(index -> {
                        if (daily.time().get(index) == null || daily.temperatureMin().get(index) == null
                                || daily.temperatureMax().get(index) == null || daily.weatherCode().get(index) == null) {
                            throw new OpenMeteoClientException("Open-Meteo response has incomplete daily forecast data");
                        }
                        LocalDate date = LocalDate.parse(daily.time().get(index));
                        return new DailyForecastResponse(
                                date.atStartOfDay().toInstant(ZoneOffset.UTC),
                                daily.temperatureMin().get(index),
                                daily.temperatureMax().get(index),
                                null,
                                toDescription(daily.weatherCode().get(index)),
                                String.valueOf(daily.weatherCode().get(index)),
                                date
                        );
                    })
                    .toList();
        } catch (DateTimeParseException exception) {
            throw new OpenMeteoClientException("Open-Meteo response has an invalid daily forecast date");
        }
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
