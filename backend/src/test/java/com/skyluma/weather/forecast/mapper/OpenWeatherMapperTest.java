package com.skyluma.weather.forecast.mapper;

import com.skyluma.weather.forecast.dto.WeatherResponse;
import com.skyluma.weather.openweather.dto.OpenWeatherCondition;
import com.skyluma.weather.openweather.dto.OpenWeatherCurrent;
import com.skyluma.weather.openweather.dto.OpenWeatherResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

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
                )
        );

        WeatherResponse response = mapper.toWeatherResponse(openWeatherResponse, 50.8798, 4.7005);

        assertThat(response.location().latitude()).isEqualTo(50.8798);
        assertThat(response.location().longitude()).isEqualTo(4.7005);
        assertThat(response.current().temperature()).isEqualTo(21.5);
        assertThat(response.current().feelsLike()).isEqualTo(20.8);
        assertThat(response.current().humidity()).isEqualTo(65);
        assertThat(response.current().description()).isEqualTo("clear sky");
        assertThat(response.current().icon()).isEqualTo("01d");
    }
}
