package com.skyluma.weather.forecast.controller;

import com.skyluma.weather.common.error.GlobalExceptionHandler;
import com.skyluma.weather.config.WeatherProviderProperties;
import com.skyluma.weather.forecast.dto.WeatherResponse;
import com.skyluma.weather.forecast.provider.WeatherProvider;
import com.skyluma.weather.forecast.provider.WeatherProviderType;
import com.skyluma.weather.forecast.service.WeatherService;
import com.skyluma.weather.openmeteo.client.OpenMeteoClientException;
import com.skyluma.weather.openmeteo.dto.OpenMeteoResponse;
import com.skyluma.weather.openmeteo.mapper.OpenMeteoMapper;
import com.skyluma.weather.openweather.client.OpenWeatherClientException;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WeatherControllerProviderFailureTest {

    @Test
    void openMeteoFailureUsesApiErrorResponse() throws Exception {
        assertBadGateway(WeatherProviderType.OPENMETEO,
                () -> { throw new OpenMeteoClientException("Could not fetch weather data from Open-Meteo"); },
                "Could not fetch weather data from Open-Meteo");
    }

    @Test
    void openWeatherFailureUsesApiErrorResponse() throws Exception {
        assertBadGateway(WeatherProviderType.OPENWEATHER,
                () -> { throw new OpenWeatherClientException("Could not fetch weather data from OpenWeather"); },
                "Could not fetch weather data from OpenWeather");
    }

    @Test
    void malformedProviderResponseUsesApiErrorResponse() throws Exception {
        assertBadGateway(WeatherProviderType.OPENMETEO,
                () -> new OpenMeteoMapper().toWeatherResponse(new OpenMeteoResponse(50, 4, null, null), 50, 4),
                "Open-Meteo response is missing current weather data");
    }

    private void assertBadGateway(WeatherProviderType type, WeatherAction action, String message) throws Exception {
        WeatherProvider provider = new WeatherProvider() {
            @Override
            public WeatherProviderType type() {
                return type;
            }

            @Override
            public WeatherResponse getWeather(double latitude, double longitude) {
                return action.get();
            }
        };
        WeatherService service = new WeatherService(List.of(provider), new WeatherProviderProperties(type));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new WeatherController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockMvc.perform(get("/api/weather")
                        .param("latitude", "50")
                        .param("longitude", "4")
                        .param("provider", type.value()))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(502))
                .andExpect(jsonPath("$.error").value("Bad Gateway"))
                .andExpect(jsonPath("$.messages[0]").value(message));
    }

    private interface WeatherAction {
        WeatherResponse get();
    }
}
