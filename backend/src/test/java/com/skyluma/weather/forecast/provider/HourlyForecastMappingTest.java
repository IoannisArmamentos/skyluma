package com.skyluma.weather.forecast.provider;

import com.skyluma.weather.forecast.dto.WeatherResponse;
import com.skyluma.weather.openmeteo.dto.OpenMeteoHourly;
import com.skyluma.weather.openmeteo.dto.OpenMeteoResponse;
import com.skyluma.weather.openmeteo.mapper.OpenMeteoMapper;
import com.skyluma.weather.openweather.dto.OpenWeatherHourly;
import com.skyluma.weather.openweather.dto.OpenWeatherResponse;
import com.skyluma.weather.openweather.mapper.OpenWeatherMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class HourlyForecastMappingTest {

    private final OpenMeteoMapper openMeteoMapper = new OpenMeteoMapper();
    private final OpenWeatherMapper openWeatherMapper = new OpenWeatherMapper();

    @Autowired
    private JsonMapper jsonMapper;

    @Test
    void mapsOpenMeteoHourlyDataAndSpringDstBoundary() throws IOException {
        OpenMeteoResponse provider = fixture("openmeteo-hourly.json", OpenMeteoResponse.class);
        WeatherResponse response = openMeteoMapper.toWeatherResponse(provider, 50.88, 4.7);

        assertThat(response.timezone()).isEqualTo("Europe/Brussels");
        assertThat(response.utcOffsetSeconds()).isEqualTo(7200);
        assertThat(response.units().temperature()).isEqualTo("°C");
        assertThat(response.units().windSpeed()).isEqualTo("m/s");
        assertThat(response.units().precipitationAmount()).isEqualTo("mm");
        assertThat(response.capabilities().supportsHourlyForecast()).isTrue();
        assertThat(response.capabilities().supportsAlerts()).isFalse();
        assertThat(response.hourly()).hasSize(3);
        assertThat(response.hourly()).extracting(hour -> hour.time())
                .containsExactly(Instant.parse("2026-03-29T00:00:00Z"),
                        Instant.parse("2026-03-29T01:00:00Z"), Instant.parse("2026-03-29T02:00:00Z"));
        assertThat(response.hourly()).extracting(hour -> hour.time().atZone(ZoneId.of(response.timezone())).getHour())
                .containsExactly(1, 3, 4);
        assertThat(response.hourly().get(1).temperature()).isEqualTo(12.4);
        assertThat(response.hourly().get(1).precipitationProbability()).isEqualTo(30);
        assertThat(response.hourly().get(1).precipitationAmount()).isEqualTo(0.4);
        assertThat(response.hourly().get(1).windGusts()).isEqualTo(7.1);
        assertThat(response.hourly().get(1).pressure()).isEqualTo(1004.3);
        assertThat(response.hourly().get(2).feelsLike()).isNull();
        assertThat(response.hourly().get(2).precipitationAmount()).isNull();
        assertThat(response.daily()).extracting(day -> day.date())
                .containsExactly(LocalDate.of(2026, 3, 29), LocalDate.of(2026, 3, 30));
        assertThat(response.daily().getFirst().dateTime()).isEqualTo(Instant.parse("2026-03-29T00:00:00Z"));
        assertThat(response.daily().getFirst().sunrise()).isEqualTo(Instant.ofEpochSecond(1774759080));
        assertThat(response.current().sunrise()).isEqualTo(response.daily().getFirst().sunrise());
        assertThat(response.alerts()).isEmpty();
    }

    @Test
    void mapsOpenWeatherHoursAcrossAutumnDstAndKeepsMissingMeasurementsNull() throws IOException {
        OpenWeatherResponse provider = fixture("openweather-hourly.json", OpenWeatherResponse.class);
        WeatherResponse response = openWeatherMapper.toWeatherResponse(provider, 34.05, -118.24);

        assertThat(response.timezone()).isEqualTo("America/Los_Angeles");
        assertThat(response.utcOffsetSeconds()).isEqualTo(-28800);
        assertThat(response.units().pressure()).isEqualTo("hPa");
        assertThat(response.capabilities().supportsAlerts()).isTrue();
        assertThat(response.hourly()).hasSize(3);
        assertThat(response.hourly().get(0).time()).isEqualTo(Instant.parse("2026-11-01T08:30:00Z"));
        assertThat(response.hourly().get(1).time()).isEqualTo(Instant.parse("2026-11-01T09:30:00Z"));
        assertThat(response.hourly()).extracting(hour -> hour.time().atZone(ZoneId.of(response.timezone())).getHour())
                .containsExactly(1, 1, 2);
        assertThat(response.hourly().get(0).precipitationProbability()).isEqualTo(25);
        assertThat(response.hourly().get(1).precipitationProbability()).isEqualTo(65);
        assertThat(response.hourly().get(1).precipitationAmount()).isEqualTo(0.8);
        assertThat(response.hourly().get(1).windGusts()).isNull();
        assertThat(response.hourly().get(2).precipitationProbability()).isZero();
        assertThat(response.hourly().get(2).precipitationAmount()).isNull();
        assertThat(response.daily().getFirst().date()).isEqualTo(LocalDate.of(2026, 11, 1));
        assertThat(response.current().sunrise()).isEqualTo(Instant.ofEpochSecond(1793520000));
        assertThat(response.daily().getFirst().sunset()).isEqualTo(Instant.ofEpochSecond(1793563200));
        assertThat(jsonMapper.writeValueAsString(response)).contains("\"hourly\"", "\"utcOffsetSeconds\"", "\"units\"");
    }

    @Test
    void toleratesMissingAndIncompleteOpenMeteoHourlyArrays() throws IOException {
        OpenMeteoResponse provider = fixture("openmeteo-hourly.json", OpenMeteoResponse.class);
        OpenMeteoHourly original = provider.hourly();
        OpenMeteoHourly incomplete = new OpenMeteoHourly(
                Arrays.asList(original.time().get(2), null, original.time().get(0)),
                List.of(11.0), null, null, null, null, null, null, null, null, null);
        OpenMeteoResponse changed = new OpenMeteoResponse(provider.latitude(), provider.longitude(),
                provider.current(), provider.daily(), provider.timezone(), provider.utcOffsetSeconds(), incomplete);

        WeatherResponse response = openMeteoMapper.toWeatherResponse(changed, 50.88, 4.7);
        assertThat(response.hourly()).hasSize(2);
        assertThat(response.hourly().getFirst().time()).isEqualTo(Instant.ofEpochSecond(original.time().get(0)));
        assertThat(response.hourly().getFirst().temperature()).isNull();
        assertThat(response.hourly().getLast().temperature()).isEqualTo(11.0);
        assertThat(response.hourly().getLast().precipitationAmount()).isNull();
        assertThat(openMeteoMapper.toWeatherResponse(new OpenMeteoResponse(provider.latitude(),
                provider.longitude(), provider.current(), provider.daily(), provider.timezone(),
                provider.utcOffsetSeconds(), null), 50.88, 4.7).hourly()).isEmpty();
    }

    @Test
    void sortsOpenWeatherHoursAndCapsCoverageAt48() throws IOException {
        OpenWeatherResponse provider = fixture("openweather-hourly.json", OpenWeatherResponse.class);
        OpenWeatherHourly sample = provider.hourly().getFirst();
        List<OpenWeatherHourly> hours = new ArrayList<>(IntStream.range(0, 52)
                .mapToObj(index -> new OpenWeatherHourly(sample.dt() + index * 3600L, sample.temp(),
                        sample.feelsLike(), sample.pop(), sample.rain(), sample.snow(), sample.windSpeed(),
                        sample.windGusts(), sample.windDirection(), sample.humidity(), sample.pressure(),
                        sample.weather()))
                .toList());
        java.util.Collections.reverse(hours);
        hours.add(null);
        OpenWeatherResponse changed = new OpenWeatherResponse(provider.lat(), provider.lon(), provider.timezone(),
                provider.timezoneOffset(), provider.current(), provider.daily(), provider.alerts(), hours);

        WeatherResponse response = openWeatherMapper.toWeatherResponse(changed, 34.05, -118.24);
        assertThat(response.hourly()).hasSize(48);
        assertThat(response.hourly().getFirst().time()).isEqualTo(Instant.ofEpochSecond(sample.dt()));
        assertThat(response.hourly().getLast().time()).isEqualTo(Instant.ofEpochSecond(sample.dt() + 47 * 3600L));
    }

    private <T> T fixture(String name, Class<T> type) throws IOException {
        try (var stream = getClass().getResourceAsStream("/fixtures/" + name)) {
            assertThat(stream).isNotNull();
            return jsonMapper.readValue(new String(stream.readAllBytes(), StandardCharsets.UTF_8), type);
        }
    }
}
