package com.skyluma.weather.openweather.mapper;

import com.skyluma.weather.forecast.dto.WeatherResponse;
import com.skyluma.weather.openweather.client.OpenWeatherClientException;
import com.skyluma.weather.openweather.dto.*;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OpenWeatherMapperTest {

    private final OpenWeatherMapper mapper = new OpenWeatherMapper();

    @Test
    void mapsCurrentWeatherResponse() {
        OpenWeatherResponse openWeatherResponse = new OpenWeatherResponse(
                50.8798,
                4.7005,
                "America/Los_Angeles",
                -25200,
                new OpenWeatherCurrent(
                        21.5,
                        20.8,
                        65,
                        List.of(new OpenWeatherCondition("clear sky", "01d"))
                ),
                List.of(),
                List.of()
        );

        WeatherResponse response = mapper.toWeatherResponse(openWeatherResponse, 50.8798, 4.7005);

        assertThat(response.provider()).isEqualTo("openweather");
        assertThat(response.location().latitude()).isEqualTo(50.8798);
        assertThat(response.location().longitude()).isEqualTo(4.7005);
        assertThat(response.current().temperature()).isEqualTo(21.5);
        assertThat(response.current().feelsLike()).isEqualTo(20.8);
        assertThat(response.current().humidity()).isEqualTo(65);
        assertThat(response.current().description()).isEqualTo("clear sky");
        assertThat(response.current().icon()).isEqualTo("01d");
        assertThat(response.daily()).isEmpty();
        assertThat(response.alerts()).isEmpty();
    }

    @Test
    void mapsDailyForecastResponse() {
        OpenWeatherResponse openWeatherResponse = new OpenWeatherResponse(
                50.8798,
                4.7005,
                "America/Los_Angeles",
                -25200,
                new OpenWeatherCurrent(
                        21.5,
                        20.8,
                        65,
                        List.of(new OpenWeatherCondition("clear sky", "01d"))
                ),
                List.of(
                        new OpenWeatherDaily(
                                1719504000L,
                                new OpenWeatherTemperature(12.4, 24.8),
                                70,
                                List.of(new OpenWeatherCondition("light rain", "10d"))
                        )
                ),
                List.of()
        );

        WeatherResponse response = mapper.toWeatherResponse(openWeatherResponse, 50.8798, 4.7005);

        assertThat(response.daily()).hasSize(1);
        assertThat(response.daily().getFirst().date()).isEqualTo(LocalDate.of(2024, 6, 27));
        assertThat(response.daily().getFirst().dateTime()).isEqualTo(Instant.ofEpochSecond(1719504000L));
        assertThat(response.daily().getFirst().minTemperature()).isEqualTo(12.4);
        assertThat(response.daily().getFirst().maxTemperature()).isEqualTo(24.8);
        assertThat(response.daily().getFirst().humidity()).isEqualTo(70);
        assertThat(response.daily().getFirst().description()).isEqualTo("light rain");
        assertThat(response.daily().getFirst().icon()).isEqualTo("10d");
        assertThat(response.alerts()).isEmpty();
    }

    @Test
    void usesForecastLocationTimezoneAcrossUtcYearBoundary() {
        for (String timezone : List.of("America/Los_Angeles", "Pacific/Kiritimati")) {
            Instant sample = timezone.equals("America/Los_Angeles")
                    ? Instant.parse("2026-01-01T00:30:00Z")
                    : Instant.parse("2025-12-31T23:30:00Z");
            OpenWeatherResponse providerResponse = new OpenWeatherResponse(50, 4, timezone,
                    null, new OpenWeatherCurrent(10.0, 9.0, 80, List.of()),
                    List.of(new OpenWeatherDaily(sample.getEpochSecond(),
                            new OpenWeatherTemperature(1.0, 5.0), 70, List.of())), List.of());

            WeatherResponse response = mapper.toWeatherResponse(providerResponse, 50, 4);

            LocalDate expected = timezone.equals("America/Los_Angeles")
                    ? LocalDate.of(2025, 12, 31) : LocalDate.of(2026, 1, 1);
            assertThat(response.daily().getFirst().date()).isEqualTo(expected);
            assertThat(response.daily().getFirst().dateTime()).isEqualTo(sample);
        }
    }

    @Test
    void usesUtcOffsetsWhenTimezoneNameIsMissing() {
        for (int offsetSeconds : List.of(3600, -28800)) {
            Instant sample = offsetSeconds > 0
                    ? Instant.parse("2025-12-31T23:30:00Z")
                    : Instant.parse("2026-01-01T00:30:00Z");
            OpenWeatherResponse providerResponse = new OpenWeatherResponse(50, 4, null, offsetSeconds,
                    new OpenWeatherCurrent(10.0, 9.0, 80, List.of()),
                    List.of(new OpenWeatherDaily(sample.getEpochSecond(),
                            new OpenWeatherTemperature(1.0, 5.0), 70, List.of())), List.of());

            WeatherResponse response = mapper.toWeatherResponse(providerResponse, 50, 4);

            LocalDate expected = offsetSeconds > 0
                    ? LocalDate.of(2026, 1, 1) : LocalDate.of(2025, 12, 31);
            assertThat(response.daily().getFirst().date()).isEqualTo(expected);
        }
    }

    @Test
    void rejectsDailyForecastWithoutLocationTimezone() {
        OpenWeatherResponse providerResponse = new OpenWeatherResponse(50, 4, null, null,
                new OpenWeatherCurrent(10.0, 9.0, 80, List.of()),
                List.of(new OpenWeatherDaily(Instant.parse("2026-01-01T00:30:00Z").getEpochSecond(),
                        new OpenWeatherTemperature(1.0, 5.0), 70, List.of())), List.of());

        assertThatThrownBy(() -> mapper.toWeatherResponse(providerResponse, 50, 4))
                .isInstanceOf(OpenWeatherClientException.class)
                .hasMessage("OpenWeather response is missing timezone data");
    }

    @Test
    void mapsWeatherAlerts() {
        OpenWeatherResponse openWeatherResponse = new OpenWeatherResponse(
                50.8798,
                4.7005,
                "America/Los_Angeles",
                -25200,
                new OpenWeatherCurrent(
                        21.5,
                        20.8,
                        65,
                        List.of(new OpenWeatherCondition("clear sky", "01d"))
                ),
                List.of(),
                List.of(
                        new OpenWeatherAlert(
                                "Royal Meteorological Institute",
                                "Thunderstorm warning",
                                1719504000L,
                                1719511200L,
                                "Thunderstorms expected in the area."
                        )
                )
        );

        WeatherResponse response = mapper.toWeatherResponse(openWeatherResponse, 50.8798, 4.7005);

        assertThat(response.daily()).isEmpty();
        assertThat(response.alerts()).hasSize(1);
        assertThat(response.alerts().getFirst().senderName()).isEqualTo("Royal Meteorological Institute");
        assertThat(response.alerts().getFirst().event()).isEqualTo("Thunderstorm warning");
        assertThat(response.alerts().getFirst().start()).isEqualTo(Instant.ofEpochSecond(1719504000L));
        assertThat(response.alerts().getFirst().end()).isEqualTo(Instant.ofEpochSecond(1719511200L));
        assertThat(response.alerts().getFirst().description()).isEqualTo("Thunderstorms expected in the area.");
    }

    @Test
    void throwsExceptionWhenCurrentWeatherIsMissing() {
        OpenWeatherResponse openWeatherResponse = new OpenWeatherResponse(
                50.8798,
                4.7005,
                "America/Los_Angeles",
                -25200,
                null,
                List.of(),
                List.of()
        );

        assertThatThrownBy(() -> mapper.toWeatherResponse(openWeatherResponse, 50.8798, 4.7005))
                .isInstanceOf(OpenWeatherClientException.class)
                .hasMessage("OpenWeather response is missing current weather data");
    }

    @Test
    void rejectsMissingCurrentTemperature() {
        OpenWeatherResponse response = new OpenWeatherResponse(50, 4, "Europe/Brussels", 7200,
                new OpenWeatherCurrent(null, 20.8, 65, List.of()), List.of(), List.of());

        assertThatThrownBy(() -> mapper.toWeatherResponse(response, 50, 4))
                .isInstanceOf(OpenWeatherClientException.class)
                .hasMessage("OpenWeather response has incomplete current weather data");
    }

    @Test
    void skipsDailyForecastWithoutTemperature() {
        OpenWeatherResponse response = new OpenWeatherResponse(50, 4, "Europe/Brussels", 7200,
                new OpenWeatherCurrent(21.5, 20.8, 65, List.of()),
                List.of(
                        new OpenWeatherDaily(1719504000L, null, 70, List.of()),
                        new OpenWeatherDaily(1719590400L, new OpenWeatherTemperature(12.0, 22.0), 60, List.of())
                ),
                List.of());

        WeatherResponse weatherResponse = mapper.toWeatherResponse(response, 50, 4);

        assertThat(weatherResponse.current().temperature()).isEqualTo(21.5);
        assertThat(weatherResponse.daily()).hasSize(1);
        assertThat(weatherResponse.daily().getFirst().dateTime()).isEqualTo(Instant.ofEpochSecond(1719590400L));
    }

    @Test
    void keepsDailyForecastWithoutHumidity() {
        OpenWeatherResponse response = new OpenWeatherResponse(50, 4, "Europe/Brussels", 7200,
                new OpenWeatherCurrent(21.5, 20.8, 65, List.of()),
                List.of(new OpenWeatherDaily(1719504000L, new OpenWeatherTemperature(12.0, 22.0), null, List.of())),
                List.of());

        WeatherResponse weatherResponse = mapper.toWeatherResponse(response, 50, 4);

        assertThat(weatherResponse.daily()).hasSize(1);
        assertThat(weatherResponse.daily().getFirst().humidity()).isNull();
    }

    @Test
    void skipsIncompleteAlertsAndKeepsValidOnes() {
        OpenWeatherResponse response = new OpenWeatherResponse(50, 4, "Europe/Brussels", 7200,
                new OpenWeatherCurrent(21.5, 20.8, 65, List.of()),
                List.of(),
                Arrays.asList(
                        new OpenWeatherAlert("KMI", "Wind", 1719504000L, null, "No end time"),
                        null,
                        new OpenWeatherAlert("KMI", "Heat", 1719504000L, 1719590400L, "Valid alert")
                ));

        WeatherResponse weatherResponse = mapper.toWeatherResponse(response, 50, 4);

        assertThat(weatherResponse.alerts()).extracting(alert -> alert.event()).containsExactly("Heat");
    }

    @Test
    void ignoresMissingWeatherConditionEntries() {
        OpenWeatherResponse response = new OpenWeatherResponse(50, 4, "Europe/Brussels", 7200,
                new OpenWeatherCurrent(21.5, 20.8, 65,
                        Arrays.asList(null, new OpenWeatherCondition("clear sky", "01d"))),
                List.of(),
                List.of());

        WeatherResponse weatherResponse = mapper.toWeatherResponse(response, 50, 4);

        assertThat(weatherResponse.current().description()).isEqualTo("clear sky");
        assertThat(weatherResponse.current().icon()).isEqualTo("01d");
    }
}
