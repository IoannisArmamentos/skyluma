package com.skyluma.weather.openweather.client;

import com.skyluma.weather.config.OpenWeatherProperties;
import com.skyluma.weather.openweather.dto.OpenWeatherResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class OpenWeatherClient {

    private final RestClient openWeatherRestClient;
    private final OpenWeatherProperties properties;

    public OpenWeatherClient(RestClient openWeatherRestClient, OpenWeatherProperties properties) {
        this.openWeatherRestClient = openWeatherRestClient;
        this.properties = properties;
    }

    public OpenWeatherResponse getWeather(double latitude, double longitude) {
        try {
            return openWeatherRestClient.get()
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
        } catch (RestClientException exception) {
            throw new OpenWeatherClientException("Could not fetch weather data from OpenWeather");
        }
    }
}