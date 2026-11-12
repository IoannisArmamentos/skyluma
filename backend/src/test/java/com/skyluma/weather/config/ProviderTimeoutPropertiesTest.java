package com.skyluma.weather.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.BindException;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProviderTimeoutPropertiesTest {

    @Test
    void usesDefaultTimeoutsWhenNotConfigured() {
        OpenMeteoProperties openMeteo = bindOpenMeteo(Map.of());
        OpenWeatherProperties openWeather = bindOpenWeather(Map.of());

        assertThat(openMeteo.connectTimeout()).isEqualTo(Duration.ofSeconds(3));
        assertThat(openMeteo.readTimeout()).isEqualTo(Duration.ofSeconds(10));
        assertThat(openWeather.connectTimeout()).isEqualTo(Duration.ofSeconds(3));
        assertThat(openWeather.readTimeout()).isEqualTo(Duration.ofSeconds(10));
    }

    @Test
    void readsTimeoutsWithoutUnitAsSeconds() {
        OpenMeteoProperties openMeteo = bindOpenMeteo(Map.of("connect-timeout", "2", "read-timeout", "5"));
        OpenWeatherProperties openWeather = bindOpenWeather(Map.of("connect-timeout", "2", "read-timeout", "5"));

        assertThat(openMeteo.connectTimeout()).isEqualTo(Duration.ofSeconds(2));
        assertThat(openMeteo.readTimeout()).isEqualTo(Duration.ofSeconds(5));
        assertThat(openWeather.connectTimeout()).isEqualTo(Duration.ofSeconds(2));
        assertThat(openWeather.readTimeout()).isEqualTo(Duration.ofSeconds(5));
    }

    @Test
    void acceptsExplicitTimeoutUnits() {
        OpenMeteoProperties openMeteo = bindOpenMeteo(Map.of("read-timeout", "500ms"));

        assertThat(openMeteo.readTimeout()).isEqualTo(Duration.ofMillis(500));
    }

    @Test
    void rejectsNonPositiveTimeouts() {
        assertThatThrownBy(() -> bindOpenMeteo(Map.of("read-timeout", "0")))
                .isInstanceOf(BindException.class)
                .rootCause()
                .hasMessage("Open-Meteo timeouts must be positive");
        assertThatThrownBy(() -> bindOpenWeather(Map.of("connect-timeout", "-1s")))
                .isInstanceOf(BindException.class)
                .rootCause()
                .hasMessage("OpenWeather timeouts must be positive");
    }

    @Test
    void rejectsOpenWeatherUnitsThatWouldMixMeasurements() {
        assertThatThrownBy(() -> bindOpenWeather(Map.of("units", "imperial")))
                .isInstanceOf(BindException.class)
                .rootCause()
                .hasMessage("OpenWeather units must be metric for the normalized API");
    }

    private OpenMeteoProperties bindOpenMeteo(Map<String, String> timeouts) {
        Map<String, String> properties = new HashMap<>();
        properties.put("openmeteo.base-url", "https://api.open-meteo.com");
        timeouts.forEach((key, value) -> properties.put("openmeteo." + key, value));
        return new Binder(new MapConfigurationPropertySource(properties))
                .bind("openmeteo", OpenMeteoProperties.class)
                .get();
    }

    private OpenWeatherProperties bindOpenWeather(Map<String, String> timeouts) {
        Map<String, String> properties = new HashMap<>();
        properties.put("openweather.base-url", "https://api.openweathermap.org/data/3.0");
        properties.put("openweather.units", "metric");
        properties.put("openweather.language", "en");
        timeouts.forEach((key, value) -> properties.put("openweather." + key, value));
        return new Binder(new MapConfigurationPropertySource(properties))
                .bind("openweather", OpenWeatherProperties.class)
                .get();
    }
}
