package com.skyluma.weather.openweather.client;

import com.skyluma.weather.config.OpenWeatherProperties;
import com.skyluma.weather.openweather.dto.OpenWeatherResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
@Component
@RequiredArgsConstructor
public class OpenWeatherClient {

    private final RestClient openWeatherRestClient;
    private final OpenWeatherProperties properties;

    public OpenWeatherResponse getWeather(double latitude, double longitude) {
        if (!StringUtils.hasText(properties.apiKey())) {
            throw new OpenWeatherClientException("OpenWeather API key is not configured");
        }

        try {
            OpenWeatherResponse response = openWeatherRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/onecall")
                            .queryParam("lat", latitude)
                            .queryParam("lon", longitude)
                            .queryParam("appid", properties.apiKey())
                            .queryParam("units", properties.units())
                            .queryParam("lang", properties.language())
                            .build())
                    .retrieve()
                    .body(OpenWeatherResponse.class);

            if (response == null) {
                throw new OpenWeatherClientException("OpenWeather returned an empty response");
            }

            return response;
        } catch (RestClientResponseException exception) {
            log.warn("OpenWeather request failed with HTTP status {}", exception.getStatusCode().value());
            throw new OpenWeatherClientException("Could not fetch weather data from OpenWeather");
        } catch (RestClientException exception) {
            // Exception messages can contain request URLs or response content, so only types are logged.
            log.warn("OpenWeather request failed: {} caused by {}",
                    exception.getClass().getSimpleName(),
                    NestedExceptionUtils.getMostSpecificCause(exception).getClass().getSimpleName());
            throw new OpenWeatherClientException("Could not fetch weather data from OpenWeather");
        }
    }
}
