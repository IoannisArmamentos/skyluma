package com.skyluma.weather.forecast.mapper;

import com.skyluma.weather.forecast.dto.WeatherResponse;
import com.skyluma.weather.openweather.client.OpenWeatherClientException;
import com.skyluma.weather.openweather.dto.*;
import org.junit.jupiter.api.Test;

import java.time.Instant;
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
        assertThat(response.daily().getFirst().dateTime()).isEqualTo(Instant.ofEpochSecond(1719504000L));
        assertThat(response.daily().getFirst().minTemperature()).isEqualTo(12.4);
        assertThat(response.daily().getFirst().maxTemperature()).isEqualTo(24.8);
        assertThat(response.daily().getFirst().humidity()).isEqualTo(70);
        assertThat(response.daily().getFirst().description()).isEqualTo("light rain");
        assertThat(response.daily().getFirst().icon()).isEqualTo("10d");
        assertThat(response.alerts()).isEmpty();
    }

    @Test
    void mapsWeatherAlerts() {
        OpenWeatherResponse openWeatherResponse = new OpenWeatherResponse(
                50.8798,
                4.7005,
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
                null,
                List.of(),
                List.of()
        );

        assertThatThrownBy(() -> mapper.toWeatherResponse(openWeatherResponse, 50.8798, 4.7005))
                .isInstanceOf(OpenWeatherClientException.class)
                .hasMessage("OpenWeather response is missing current weather data");
    }
}