package com.skyluma.weather.openmeteo.client;

import com.skyluma.weather.openmeteo.dto.OpenMeteoResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class OpenMeteoClient {

    private final RestClient openMeteoRestClient;

    public OpenMeteoClient(RestClient openMeteoRestClient) {
        this.openMeteoRestClient = openMeteoRestClient;
    }

    public OpenMeteoResponse getWeather(double latitude, double longitude) {
        try {
            OpenMeteoResponse response = openMeteoRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1/forecast")
                            .queryParam("latitude", latitude)
                            .queryParam("longitude", longitude)
                            .queryParam("current", "temperature_2m,relative_humidity_2m,apparent_temperature,weather_code")
                            .queryParam("daily", "weather_code,temperature_2m_max,temperature_2m_min")
                            .queryParam("timezone", "auto")
                            .build())
                    .retrieve()
                    .body(OpenMeteoResponse.class);

            if (response == null) {
                throw new OpenMeteoClientException("Open-Meteo returned an empty response");
            }

            return response;
        } catch (RestClientException exception) {
            throw new OpenMeteoClientException("Could not fetch weather data from Open-Meteo");
        }
    }
}
