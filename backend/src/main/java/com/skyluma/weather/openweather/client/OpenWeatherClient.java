package com.skyluma.weather.openweather.client;

import com.skyluma.weather.config.OpenWeatherProperties;
import com.skyluma.weather.openweather.dto.OpenWeatherResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class OpenWeatherClient {

    private final RestClient restClient;
    private final OpenWeatherProperties properties;

    public OpenWeatherClient(RestClient restClient, OpenWeatherProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    public OpenWeatherResponse getWeather(double latitude, double longitude) {
        try {
            return restClient.get()
                    .uri(properties.baseUrl() + "/onecall?lat={latitude}&lon={longitude}&appid={apiKey}&units={units}&lang={language}",
                            latitude,
                            longitude,
                            properties.apiKey(),
                            properties.units(),
                            properties.language())
                    .retrieve()
                    .body(OpenWeatherResponse.class);
        } catch (RestClientException exception) {
            throw new OpenWeatherClientException("Could not fetch weather data from OpenWeather");
        }
    }
}