package com.skyluma.weather.config;

import com.skyluma.weather.openmeteo.client.OpenMeteoClient;
import com.skyluma.weather.openmeteo.client.OpenMeteoClientException;
import com.skyluma.weather.openweather.client.OpenWeatherClient;
import com.skyluma.weather.openweather.client.OpenWeatherClientException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

import java.net.SocketTimeoutException;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class WeatherClientReliabilityTest {

    private final RestClientConfig config = new RestClientConfig();

    @Test
    void mapsOpenMeteoHttpErrorToProviderException() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://example.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(once(), requestTo(startsWith("https://example.test/v1/forecast")))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThatThrownBy(() -> new OpenMeteoClient(builder.build()).getWeather(50, 4))
                .isInstanceOf(OpenMeteoClientException.class)
                .hasMessage("Could not fetch weather data from Open-Meteo");
        server.verify();
    }

    @Test
    void mapsOpenWeatherHttpErrorToProviderException() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://example.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(once(), requestTo(startsWith("https://example.test/onecall")))
                .andRespond(withStatus(HttpStatus.BAD_GATEWAY));
        OpenWeatherProperties properties = weatherProperties("https://example.test", Duration.ofSeconds(3), Duration.ofSeconds(10));

        assertThatThrownBy(() -> new OpenWeatherClient(builder.build(), properties).getWeather(50, 4))
                .isInstanceOf(OpenWeatherClientException.class)
                .hasMessage("Could not fetch weather data from OpenWeather");
        server.verify();
    }

    @Test
    void mapsMalformedOpenMeteoJsonToProviderException() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://example.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(once(), requestTo(startsWith("https://example.test/v1/forecast")))
                .andRespond(withSuccess("{invalid", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> new OpenMeteoClient(builder.build()).getWeather(50, 4))
                .isInstanceOf(OpenMeteoClientException.class)
                .hasMessage("Could not fetch weather data from Open-Meteo");
        server.verify();
    }

    @Test
    void mapsMalformedOpenWeatherJsonToProviderException() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://example.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(once(), requestTo(startsWith("https://example.test/onecall")))
                .andRespond(withSuccess("{invalid", MediaType.APPLICATION_JSON));
        OpenWeatherProperties properties = weatherProperties("https://example.test", Duration.ofSeconds(3), Duration.ofSeconds(10));

        assertThatThrownBy(() -> new OpenWeatherClient(builder.build(), properties).getWeather(50, 4))
                .isInstanceOf(OpenWeatherClientException.class)
                .hasMessage("Could not fetch weather data from OpenWeather");
        server.verify();
    }

    @Test
    void openMeteoTimeoutBecomesProviderException() {
        RestClient clientWithTimeout = RestClient.builder()
                .requestFactory((uri, method) -> { throw new SocketTimeoutException("Read timed out"); })
                .build();

        assertThatThrownBy(() -> new OpenMeteoClient(clientWithTimeout).getWeather(50, 4))
                .isInstanceOf(OpenMeteoClientException.class)
                .hasMessage("Could not fetch weather data from Open-Meteo");
    }

    @Test
    void openWeatherTimeoutBecomesProviderException() {
        RestClient clientWithTimeout = RestClient.builder()
                .requestFactory((uri, method) -> { throw new SocketTimeoutException("Read timed out"); })
                .build();
        OpenWeatherProperties properties = weatherProperties("https://example.test", Duration.ofSeconds(1), Duration.ofMillis(50));

        assertThatThrownBy(() -> new OpenWeatherClient(clientWithTimeout, properties).getWeather(50, 4))
                .isInstanceOf(OpenWeatherClientException.class)
                .hasMessage("Could not fetch weather data from OpenWeather");
    }

    @Test
    void configuresBothTimeoutsForEachProvider() {
        Duration connectTimeout = Duration.ofMillis(125);
        Duration readTimeout = Duration.ofMillis(450);
        assertTimeouts(config.openMeteoRestClient(new OpenMeteoProperties("https://example.test", connectTimeout, readTimeout)), 125, 450);
        assertTimeouts(config.openWeatherRestClient(weatherProperties("https://example.test", connectTimeout, readTimeout)), 125, 450);
    }

    private void assertTimeouts(RestClient client, int connectMillis, int readMillis) {
        Object factory = ReflectionTestUtils.getField(client, "clientRequestFactory");
        assertThat(factory).isInstanceOf(SimpleClientHttpRequestFactory.class);
        assertThat(ReflectionTestUtils.getField(factory, "connectTimeout")).isEqualTo(connectMillis);
        assertThat(ReflectionTestUtils.getField(factory, "readTimeout")).isEqualTo(readMillis);
    }

    private OpenWeatherProperties weatherProperties(String baseUrl, Duration connectTimeout, Duration readTimeout) {
        return new OpenWeatherProperties(baseUrl, "test-api-key", "metric", "en", connectTimeout, readTimeout);
    }
}
