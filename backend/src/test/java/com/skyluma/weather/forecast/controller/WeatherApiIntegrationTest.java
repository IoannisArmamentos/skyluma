package com.skyluma.weather.forecast.controller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Calls the real weather endpoint with both provider clients pointed at a local HTTP server,
 * covering location calendar dates, tolerant provider mapping and read timeouts end to end.
 */
@SpringBootTest
@AutoConfigureMockMvc
class WeatherApiIntegrationTest {

    // Los Angeles: the provider returns location calendar dates; the third day has no minimum temperature.
    private static final String OPEN_METEO_RESPONSE = """
            {
              "latitude": 34.05, "longitude": -118.24, "timezone": "America/Los_Angeles",
              "current": {"time": "2025-12-31T16:00", "temperature_2m": 15.2, "apparent_temperature": 14.1,
                          "relative_humidity_2m": 60, "weather_code": 1},
              "daily": {"time": ["2025-12-31", "2026-01-01", "2026-01-02"],
                        "temperature_2m_min": [8.0, 9.0, null],
                        "temperature_2m_max": [18.0, 19.0, 20.0],
                        "weather_code": [1, 3, 61]}
            }
            """;

    // Kiritimati (UTC+14): 2025-12-31T23:30Z is already 1 January locally. Humidity is missing and
    // one alert has no end time.
    private static final String OPEN_WEATHER_RESPONSE = """
            {
              "lat": 1.87, "lon": -157.43, "timezone": "Pacific/Kiritimati", "timezone_offset": 50400,
              "current": {"temp": 27.5, "feels_like": 30.1, "humidity": 78,
                          "weather": [{"description": "few clouds", "icon": "02d"}]},
              "daily": [{"dt": 1767223800, "temp": {"min": 25.0, "max": 29.0},
                         "weather": [{"description": "light rain", "icon": "10d"}]}],
              "alerts": [
                {"sender_name": "Met Service", "event": "Wind", "start": 1767223800, "description": "No end time"},
                {"sender_name": "Met Service", "event": "Heat", "start": 1767223800, "end": 1767310200,
                 "description": "Valid alert"}
              ]
            }
            """;

    private static final HttpServer PROVIDER_SERVER = startProviderServer();
    private static volatile boolean stallResponses;
    private static volatile CountDownLatch releaseStalledResponses = new CountDownLatch(0);

    @Autowired
    private MockMvc mockMvc;

    @DynamicPropertySource
    static void providerProperties(DynamicPropertyRegistry registry) {
        String baseUrl = "http://127.0.0.1:" + PROVIDER_SERVER.getAddress().getPort();
        registry.add("openmeteo.base-url", () -> baseUrl);
        registry.add("openmeteo.read-timeout", () -> "500ms");
        registry.add("openweather.base-url", () -> baseUrl);
        registry.add("openweather.read-timeout", () -> "500ms");
    }

    @AfterEach
    void releaseStalledRequests() {
        stallResponses = false;
        releaseStalledResponses.countDown();
    }

    @AfterAll
    static void stopProviderServer() {
        PROVIDER_SERVER.stop(0);
    }

    @Test
    void returnsOpenMeteoLocationDatesAndSkipsIncompleteDays() throws Exception {
        mockMvc.perform(get("/api/weather")
                        .param("latitude", "34.05")
                        .param("longitude", "-118.24")
                        .param("provider", "openmeteo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.current.temperature").value(15.2))
                .andExpect(jsonPath("$.daily", hasSize(2)))
                .andExpect(jsonPath("$.daily[0].date").value("2025-12-31"))
                .andExpect(jsonPath("$.daily[0].dateTime").value("2025-12-31T00:00:00Z"))
                .andExpect(jsonPath("$.daily[1].date").value("2026-01-01"));
    }

    @Test
    void returnsOpenWeatherLocationDatesWithOptionalDataPreserved() throws Exception {
        mockMvc.perform(get("/api/weather")
                        .param("latitude", "1.87")
                        .param("longitude", "-157.43")
                        .param("provider", "openweather"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.daily", hasSize(1)))
                .andExpect(jsonPath("$.daily[0].date").value("2026-01-01"))
                .andExpect(jsonPath("$.daily[0].dateTime").value("2025-12-31T23:30:00Z"))
                .andExpect(jsonPath("$.daily[0].humidity").value(nullValue()))
                .andExpect(jsonPath("$.alerts", hasSize(1)))
                .andExpect(jsonPath("$.alerts[0].event").value("Heat"));
    }

    @Test
    void returnsApiErrorResponseWhenTheProviderStalls() throws Exception {
        releaseStalledResponses = new CountDownLatch(1);
        stallResponses = true;

        long startedAt = System.nanoTime();
        mockMvc.perform(get("/api/weather")
                        .param("latitude", "34.05")
                        .param("longitude", "-118.24")
                        .param("provider", "openmeteo"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status").value(502))
                .andExpect(jsonPath("$.error").value("Bad Gateway"))
                .andExpect(jsonPath("$.messages[0]").value("Could not fetch weather data from Open-Meteo"));

        assertThat(Duration.ofNanos(System.nanoTime() - startedAt)).isLessThan(Duration.ofSeconds(3));
    }

    private static HttpServer startProviderServer() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.setExecutor(Executors.newCachedThreadPool());
            server.createContext("/v1/forecast", exchange -> respond(exchange, OPEN_METEO_RESPONSE));
            server.createContext("/onecall", exchange -> respond(exchange, OPEN_WEATHER_RESPONSE));
            server.start();
            return server;
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private static void respond(HttpExchange exchange, String json) throws IOException {
        try (exchange) {
            if (stallResponses) {
                releaseStalledResponses.await(10, TimeUnit.SECONDS);
                return;
            }
            byte[] body = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream responseBody = exchange.getResponseBody()) {
                responseBody.write(body);
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}
