package com.skyluma.weather.openmeteo.mapper;

import com.skyluma.weather.forecast.dto.CurrentWeatherResponse;
import com.skyluma.weather.forecast.dto.WeatherCapabilitiesResponse;
import com.skyluma.weather.forecast.dto.DailyForecastResponse;
import com.skyluma.weather.forecast.dto.HourlyForecastResponse;
import com.skyluma.weather.forecast.dto.LocationResponse;
import com.skyluma.weather.forecast.dto.WeatherAlertResponse;
import com.skyluma.weather.forecast.dto.WeatherResponse;
import com.skyluma.weather.forecast.dto.WeatherUnitsResponse;
import com.skyluma.weather.openmeteo.client.OpenMeteoClientException;
import com.skyluma.weather.openmeteo.dto.OpenMeteoCurrent;
import com.skyluma.weather.openmeteo.dto.OpenMeteoDaily;
import com.skyluma.weather.openmeteo.dto.OpenMeteoHourly;
import com.skyluma.weather.openmeteo.dto.OpenMeteoResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.Comparator;
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

        List<DailyForecastResponse> daily = toDailyForecast(openMeteoResponse.daily(), openMeteoResponse.timezone());
        return new WeatherResponse(
                "openmeteo",
                new LocationResponse(latitude, longitude),
                toCurrentWeather(openMeteoResponse.current(), daily, openMeteoResponse.timezone()),
                daily,
                List.of(),
                openMeteoResponse.timezone(),
                openMeteoResponse.utcOffsetSeconds(),
                WeatherUnitsResponse.METRIC,
                toHourlyForecast(openMeteoResponse.hourly()),
                WeatherCapabilitiesResponse.OPEN_METEO
        );
    }

    private CurrentWeatherResponse toCurrentWeather(OpenMeteoCurrent current,
                                                    List<DailyForecastResponse> daily, String timezone) {
        LocalDate currentDate = currentDate(current.time(), timezone);
        DailyForecastResponse currentDay = daily.stream()
                .filter(day -> day.date().equals(currentDate))
                .findFirst()
                .orElse(null);
        return new CurrentWeatherResponse(
                current.temperature(),
                current.apparentTemperature(),
                current.relativeHumidity(),
                toDescription(current.weatherCode()),
                String.valueOf(current.weatherCode()),
                current.windSpeed(), current.windGusts(), current.windDirection(), current.pressure(),
                currentDay == null ? null : currentDay.sunrise(),
                currentDay == null ? null : currentDay.sunset()
        );
    }

    private LocalDate currentDate(String time, String timezone) {
        if (time == null) {
            return null;
        }
        try {
            if (timezone != null && !timezone.isBlank()) {
                return Instant.ofEpochSecond(Long.parseLong(time)).atZone(ZoneId.of(timezone)).toLocalDate();
            }
            return LocalDateTime.parse(time).toLocalDate();
        } catch (DateTimeException | NumberFormatException exception) {
            return null;
        }
    }

    private List<DailyForecastResponse> toDailyForecast(OpenMeteoDaily daily, String timezone) {
        if (daily == null || daily.time() == null || daily.temperatureMin() == null
                || daily.temperatureMax() == null || daily.weatherCode() == null) {
            return List.of();
        }

        int days = Math.min(
                Math.min(daily.time().size(), daily.temperatureMin().size()),
                Math.min(daily.temperatureMax().size(), daily.weatherCode().size())
        );
        List<DailyForecastResponse> forecast = IntStream.range(0, days)
                .mapToObj(index -> toDailyForecast(daily, index, timezone))
                .filter(Objects::nonNull)
                .toList();

        if (forecast.size() < daily.time().size()) {
            log.warn("Skipped {} incomplete Open-Meteo daily forecast entries", daily.time().size() - forecast.size());
        }
        return forecast;
    }

    // Returns null for a day with missing values or an invalid date, so the other days are kept.
    private DailyForecastResponse toDailyForecast(OpenMeteoDaily daily, int index, String timezone) {
        Object time = daily.time().get(index);
        Double minTemperature = daily.temperatureMin().get(index);
        Double maxTemperature = daily.temperatureMax().get(index);
        Integer weatherCode = daily.weatherCode().get(index);
        if (time == null || minTemperature == null || maxTemperature == null || weatherCode == null) {
            return null;
        }

        LocalDate date;
        try {
            if (time instanceof Number unixTime) {
                if (timezone == null || timezone.isBlank()) {
                    throw new OpenMeteoClientException("Open-Meteo response is missing timezone data");
                }
                date = Instant.ofEpochSecond(unixTime.longValue()).atZone(ZoneId.of(timezone)).toLocalDate();
            } else if (time instanceof String dateText) {
                date = LocalDate.parse(dateText);
            } else {
                return null;
            }
        } catch (DateTimeException exception) {
            return null;
        }

        return new DailyForecastResponse(
                date.atStartOfDay().toInstant(ZoneOffset.UTC),
                minTemperature,
                maxTemperature,
                null,
                toDescription(weatherCode),
                String.valueOf(weatherCode),
                date,
                instantAt(daily.sunrise(), index),
                instantAt(daily.sunset(), index)
        );
    }

    private List<HourlyForecastResponse> toHourlyForecast(OpenMeteoHourly hourly) {
        if (hourly == null || hourly.time() == null) {
            return List.of();
        }
        return IntStream.range(0, hourly.time().size())
                .mapToObj(index -> toHourlyForecast(hourly, index))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(HourlyForecastResponse::time))
                .limit(48)
                .toList();
    }

    private HourlyForecastResponse toHourlyForecast(OpenMeteoHourly hourly, int index) {
        Instant time = instantAt(hourly.time(), index);
        if (time == null) {
            return null;
        }
        Integer code = valueAt(hourly.weatherCode(), index);
        return new HourlyForecastResponse(time,
                valueAt(hourly.temperature(), index), valueAt(hourly.feelsLike(), index),
                valueAt(hourly.precipitationProbability(), index), valueAt(hourly.precipitation(), index),
                valueAt(hourly.windSpeed(), index), valueAt(hourly.windGusts(), index),
                valueAt(hourly.windDirection(), index), valueAt(hourly.humidity(), index),
                valueAt(hourly.pressure(), index), code == null ? null : toDescription(code),
                code == null ? null : String.valueOf(code));
    }

    private <T> T valueAt(List<T> values, int index) {
        return values != null && index < values.size() ? values.get(index) : null;
    }

    private Instant instantAt(List<Long> values, int index) {
        Long seconds = valueAt(values, index);
        if (seconds == null) {
            return null;
        }
        try {
            return Instant.ofEpochSecond(seconds);
        } catch (DateTimeException exception) {
            return null;
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
