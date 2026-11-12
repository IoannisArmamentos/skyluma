package com.skyluma.weather.config;

import com.skyluma.weather.openmeteo.client.OpenMeteoClient;
import com.skyluma.weather.openmeteo.client.OpenMeteoClientException;
import com.skyluma.weather.openweather.client.OpenWeatherClient;
import com.skyluma.weather.openweather.client.OpenWeatherClientException;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@ExtendWith(OutputCaptureExtension.class)
class WeatherClientReliabilityTest {

    private static final String API_KEY = "secret-test-api-key";
    private static final Duration SHORT_READ_TIMEOUT = Duration.ofMillis(300);

    private final RestClientConfig config = new RestClientConfig();
    private final CountDownLatch releaseStalledRequests = new CountDownLatch(1);
    private HttpServer server;
    private ExecutorService serverExecutor;

    @AfterEach
    void stopServer() {
        releaseStalledRequests.countDown();
        if (server != null) {
            server.stop(0);
            serverExecutor.shutdownNow();
        }
    }

    @Test
    void requestsMeanSeaLevelPressureAndMapsOpenMeteoHttpError() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://example.test");
        MockRestServiceServer mockServer = MockRestServiceServer.bindTo(builder).build();
        mockServer.expect(once(), requestTo(startsWith("https://example.test/v1/forecast")))
                .andExpect(request -> assertThat(request.getURI().getQuery())
                        .contains("current=temperature_2m,relative_humidity_2m,apparent_temperature,weather_code,wind_speed_10m,wind_gusts_10m,wind_direction_10m,pressure_msl",
                                "hourly=temperature_2m,apparent_temperature,precipitation_probability,precipitation,wind_speed_10m,wind_gusts_10m,wind_direction_10m,relative_humidity_2m,pressure_msl,weather_code",
                                "forecast_hours=48", "timeformat=unixtime", "wind_speed_unit=ms",
                                "temperature_unit=celsius", "precipitation_unit=mm")
                        .doesNotContain("surface_pressure"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThatThrownBy(() -> new OpenMeteoClient(builder.build()).getWeather(50, 4))
                .isInstanceOf(OpenMeteoClientException.class)
                .hasMessage("Could not fetch weather data from Open-Meteo");
        mockServer.verify();
    }

    @Test
    void mapsOpenWeatherHttpErrorToProviderException() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://example.test");
        MockRestServiceServer mockServer = MockRestServiceServer.bindTo(builder).build();
        mockServer.expect(once(), requestTo(startsWith("https://example.test/onecall")))
                .andExpect(request -> assertThat(request.getURI().getRawQuery()).contains("units=metric"))
                .andRespond(withStatus(HttpStatus.BAD_GATEWAY));

        assertThatThrownBy(() -> new OpenWeatherClient(builder.build(), openWeatherProperties("https://example.test")).getWeather(50, 4))
                .isInstanceOf(OpenWeatherClientException.class)
                .hasMessage("Could not fetch weather data from OpenWeather");
        mockServer.verify();
    }

    @Test
    void mapsMalformedOpenMeteoJsonToProviderException() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://example.test");
        MockRestServiceServer mockServer = MockRestServiceServer.bindTo(builder).build();
        mockServer.expect(once(), requestTo(startsWith("https://example.test/v1/forecast")))
                .andRespond(withSuccess("{invalid", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> new OpenMeteoClient(builder.build()).getWeather(50, 4))
                .isInstanceOf(OpenMeteoClientException.class)
                .hasMessage("Could not fetch weather data from Open-Meteo");
        mockServer.verify();
    }

    @Test
    void mapsMalformedOpenWeatherJsonToProviderException() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://example.test");
        MockRestServiceServer mockServer = MockRestServiceServer.bindTo(builder).build();
        mockServer.expect(once(), requestTo(startsWith("https://example.test/onecall")))
                .andRespond(withSuccess("{invalid", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> new OpenWeatherClient(builder.build(), openWeatherProperties("https://example.test")).getWeather(50, 4))
                .isInstanceOf(OpenWeatherClientException.class)
                .hasMessage("Could not fetch weather data from OpenWeather");
        mockServer.verify();
    }

    @Test
    void openMeteoReadTimeoutStopsAStalledRequest() throws IOException {
        String baseUrl = startServer(stalledResponse());
        RestClient restClient = config.openMeteoRestClient(
                new OpenMeteoProperties(baseUrl, Duration.ofSeconds(1), SHORT_READ_TIMEOUT));

        long startedAt = System.nanoTime();
        assertThatThrownBy(() -> new OpenMeteoClient(restClient).getWeather(50, 4))
                .isInstanceOf(OpenMeteoClientException.class)
                .hasMessage("Could not fetch weather data from Open-Meteo");

        assertThat(elapsedSince(startedAt)).isLessThan(Duration.ofSeconds(3));
    }

    @Test
    void openWeatherReadTimeoutStopsAStalledRequest(CapturedOutput output) throws IOException {
        String baseUrl = startServer(stalledResponse());
        OpenWeatherProperties properties = openWeatherProperties(baseUrl);
        RestClient restClient = config.openWeatherRestClient(properties);

        long startedAt = System.nanoTime();
        assertThatThrownBy(() -> new OpenWeatherClient(restClient, properties).getWeather(50, 4))
                .isInstanceOf(OpenWeatherClientException.class)
                .hasMessage("Could not fetch weather data from OpenWeather");

        assertThat(elapsedSince(startedAt)).isLessThan(Duration.ofSeconds(3));
        assertThat(output).contains("OpenWeather request failed: ResourceAccessException caused by SocketTimeoutException");
        assertThat(output).doesNotContain(API_KEY);
    }

    @Test
    void openWeatherHttpErrorLogsStatusWithoutApiKeyOrResponseBody(CapturedOutput output) throws IOException {
        String baseUrl = startServer(exchange -> {
            byte[] body = "{\"message\":\"provider-error-detail\"}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(401, body.length);
            try (OutputStream responseBody = exchange.getResponseBody()) {
                responseBody.write(body);
            }
        });
        OpenWeatherProperties properties = openWeatherProperties(baseUrl);

        assertThatThrownBy(() -> new OpenWeatherClient(config.openWeatherRestClient(properties), properties).getWeather(50, 4))
                .isInstanceOf(OpenWeatherClientException.class);

        assertThat(output).contains("OpenWeather request failed with HTTP status 401");
        assertThat(output).doesNotContain(API_KEY);
        assertThat(output).doesNotContain("provider-error-detail");
    }

    private String startServer(HttpHandler handler) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        serverExecutor = Executors.newCachedThreadPool();
        server.setExecutor(serverExecutor);
        server.createContext("/", handler);
        server.start();
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    // Accepts the request but sends nothing until the test ends.
    private HttpHandler stalledResponse() {
        return exchange -> {
            try {
                releaseStalledRequests.await(10, TimeUnit.SECONDS);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            } finally {
                exchange.close();
            }
        };
    }

    private Duration elapsedSince(long startedAt) {
        return Duration.ofNanos(System.nanoTime() - startedAt);
    }

    private OpenWeatherProperties openWeatherProperties(String baseUrl) {
        return new OpenWeatherProperties(baseUrl, API_KEY, "metric", "en", Duration.ofSeconds(1), SHORT_READ_TIMEOUT);
    }
}
