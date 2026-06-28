package com.skyluma.weather.openweather.client;

import com.skyluma.weather.config.OpenWeatherProperties;
import com.skyluma.weather.openweather.dto.OpenWeatherResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Component
@RequiredArgsConstructor
public class OpenWeatherClient {

    private final RestClient openWeatherRestClient;
    private final OpenWeatherProperties properties;

    public OpenWeatherResponse getWeather(double latitude, double longitude) {
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
        } catch (RestClientException exception) {
            log.warn("Failed to fetch weather data from OpenWeather", exception);
            throw new OpenWeatherClientException("Could not fetch weather data from OpenWeather");
        }
    }
}