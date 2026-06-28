package com.skyluma.weather.openmeteo.mapper;

import com.skyluma.weather.forecast.dto.WeatherResponse;
import com.skyluma.weather.openmeteo.client.OpenMeteoClientException;
import com.skyluma.weather.openmeteo.dto.OpenMeteoCurrent;
import com.skyluma.weather.openmeteo.dto.OpenMeteoDaily;
import com.skyluma.weather.openmeteo.dto.OpenMeteoResponse;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OpenMeteoMapperTest {

    private final OpenMeteoMapper mapper = new OpenMeteoMapper();

    @Test
    void mapsCurrentWeatherResponse() {
        OpenMeteoResponse openMeteoResponse = new OpenMeteoResponse(
                50.8798,
                4.7005,
                new OpenMeteoCurrent(
                        "2026-06-28T14:00",
                        21.5,
                        20.8,
                        65,
                        1
                ),
                new OpenMeteoDaily(
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of()
                )
        );

        WeatherResponse response = mapper.toWeatherResponse(openMeteoResponse, 50.8798, 4.7005);

        assertThat(response.provider()).isEqualTo("openmeteo");
        assertThat(response.location().latitude()).isEqualTo(50.8798);
        assertThat(response.location().longitude()).isEqualTo(4.7005);
        assertThat(response.current().temperature()).isEqualTo(21.5);
        assertThat(response.current().feelsLike()).isEqualTo(20.8);
        assertThat(response.current().humidity()).isEqualTo(65);
        assertThat(response.current().description()).isEqualTo("Partly cloudy");
        assertThat(response.current().icon()).isEqualTo("1");
        assertThat(response.daily()).isEmpty();
        assertThat(response.alerts()).isEmpty();
    }

    @Test
    void mapsDailyForecastResponseWithoutHumidity() {
        OpenMeteoResponse openMeteoResponse = new OpenMeteoResponse(
                50.8798,
                4.7005,
                new OpenMeteoCurrent(
                        "2026-06-28T14:00",
                        21.5,
                        20.8,
                        65,
                        0
                ),
                new OpenMeteoDaily(
                        List.of("2026-06-28"),
                        List.of(12.4),
                        List.of(24.8),
                        List.of(61)
                )
        );

        WeatherResponse response = mapper.toWeatherResponse(openMeteoResponse, 50.8798, 4.7005);

        assertThat(response.daily()).hasSize(1);
        assertThat(response.daily().getFirst().dateTime()).isEqualTo(Instant.parse("2026-06-28T00:00:00Z"));
        assertThat(response.daily().getFirst().minTemperature()).isEqualTo(12.4);
        assertThat(response.daily().getFirst().maxTemperature()).isEqualTo(24.8);
        assertThat(response.daily().getFirst().humidity()).isNull();
        assertThat(response.daily().getFirst().description()).isEqualTo("Rain");
        assertThat(response.daily().getFirst().icon()).isEqualTo("61");
        assertThat(response.alerts()).isEmpty();
    }

    @Test
    void returnsEmptyDailyForecastWhenDailyDataIsMissing() {
        OpenMeteoResponse openMeteoResponse = new OpenMeteoResponse(
                50.8798,
                4.7005,
                new OpenMeteoCurrent(
                        "2026-06-28T14:00",
                        21.5,
                        20.8,
                        65,
                        0
                ),
                null
        );

        WeatherResponse response = mapper.toWeatherResponse(openMeteoResponse, 50.8798, 4.7005);

        assertThat(response.daily()).isEmpty();
    }

    @Test
    void throwsExceptionWhenCurrentWeatherIsMissing() {
        OpenMeteoResponse openMeteoResponse = new OpenMeteoResponse(
                50.8798,
                4.7005,
                null,
                null
        );

        assertThatThrownBy(() -> mapper.toWeatherResponse(openMeteoResponse, 50.8798, 4.7005))
                .isInstanceOf(OpenMeteoClientException.class)
                .hasMessage("Open-Meteo response is missing current weather data");
    }
}
