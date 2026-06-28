package com.skyluma.weather.forecast.service;

import com.skyluma.weather.config.WeatherProviderProperties;
import com.skyluma.weather.forecast.dto.CurrentWeatherResponse;
import com.skyluma.weather.forecast.dto.LocationResponse;
import com.skyluma.weather.forecast.dto.WeatherResponse;
import com.skyluma.weather.forecast.provider.WeatherProvider;
import com.skyluma.weather.forecast.provider.WeatherProviderType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WeatherServiceTest {

    @Test
    void usesConfiguredWeatherProvider() {
        WeatherResponse openMeteoResponse = weatherResponse("Open-Meteo");
        WeatherResponse openWeatherResponse = weatherResponse("OpenWeather");

        WeatherService weatherService = new WeatherService(
                List.of(
                        new TestWeatherProvider(WeatherProviderType.OPENMETEO, openMeteoResponse),
                        new TestWeatherProvider(WeatherProviderType.OPENWEATHER, openWeatherResponse)
                ),
                new WeatherProviderProperties(WeatherProviderType.OPENMETEO)
        );

        WeatherResponse response = weatherService.getWeather(50.8798, 4.7005);

        assertThat(response).isSameAs(openMeteoResponse);
        assertThat(response.current().description()).isEqualTo("Open-Meteo");
    }

    @Test
    void throwsExceptionWhenConfiguredWeatherProviderIsNotAvailable() {
        WeatherService weatherService = new WeatherService(
                List.of(new TestWeatherProvider(WeatherProviderType.OPENWEATHER, weatherResponse("OpenWeather"))),
                new WeatherProviderProperties(WeatherProviderType.OPENMETEO)
        );

        assertThatThrownBy(() -> weatherService.getWeather(50.8798, 4.7005))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Configured weather provider is not available: OPENMETEO");
    }

    private static WeatherResponse weatherResponse(String description) {
        return new WeatherResponse(
                new LocationResponse(50.8798, 4.7005),
                new CurrentWeatherResponse(
                        21.5,
                        20.8,
                        65,
                        description,
                        "01d"
                ),
                List.of(),
                List.of()
        );
    }

    private record TestWeatherProvider(
            WeatherProviderType type,
            WeatherResponse weatherResponse
    ) implements WeatherProvider {

        @Override
        public WeatherResponse getWeather(double latitude, double longitude) {
            return weatherResponse;
        }
    }
}