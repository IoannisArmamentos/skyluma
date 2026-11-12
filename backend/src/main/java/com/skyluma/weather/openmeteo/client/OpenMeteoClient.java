package com.skyluma.weather.openmeteo.client;

import com.skyluma.weather.openmeteo.dto.OpenMeteoResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
@Component
@RequiredArgsConstructor
public class OpenMeteoClient {

    private final RestClient openMeteoRestClient;

    public OpenMeteoResponse getWeather(double latitude, double longitude) {
        try {
            OpenMeteoResponse response = openMeteoRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1/forecast")
                            .queryParam("latitude", latitude)
                            .queryParam("longitude", longitude)
                            .queryParam("current", "temperature_2m,relative_humidity_2m,apparent_temperature,weather_code,wind_speed_10m,wind_gusts_10m,wind_direction_10m,pressure_msl")
                            .queryParam("hourly", "temperature_2m,apparent_temperature,precipitation_probability,precipitation,wind_speed_10m,wind_gusts_10m,wind_direction_10m,relative_humidity_2m,pressure_msl,weather_code")
                            .queryParam("daily", "weather_code,temperature_2m_max,temperature_2m_min,sunrise,sunset")
                            .queryParam("timezone", "auto")
                            .queryParam("timeformat", "unixtime")
                            .queryParam("forecast_hours", 48)
                            .queryParam("temperature_unit", "celsius")
                            .queryParam("precipitation_unit", "mm")
                            .queryParam("wind_speed_unit", "ms")
                            .build())
                    .retrieve()
                    .body(OpenMeteoResponse.class);

            if (response == null) {
                throw new OpenMeteoClientException("Open-Meteo returned an empty response");
            }

            return response;
        } catch (RestClientResponseException exception) {
            log.warn("Open-Meteo request failed with HTTP status {}", exception.getStatusCode().value());
            throw new OpenMeteoClientException("Could not fetch weather data from Open-Meteo");
        } catch (RestClientException exception) {
            // Exception messages can contain request URLs or response content, so only types are logged.
            log.warn("Open-Meteo request failed: {} caused by {}",
                    exception.getClass().getSimpleName(),
                    NestedExceptionUtils.getMostSpecificCause(exception).getClass().getSimpleName());
            throw new OpenMeteoClientException("Could not fetch weather data from Open-Meteo");
        }
    }
}
