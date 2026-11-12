package com.skyluma.weather.forecast.dto;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class DailyForecastResponseSerializationTest {

    @Autowired
    private JsonMapper jsonMapper;

    @Test
    void serializesDateWithoutReplacingLegacyTimestamp() {
        DailyForecastResponse day = new DailyForecastResponse(
                Instant.parse("2026-01-01T00:00:00Z"), 1, 5, null, "Clear sky", "0",
                LocalDate.of(2026, 1, 1));

        String json = jsonMapper.writeValueAsString(day);

        assertThat(json).contains("\"date\":\"2026-01-01\"");
        assertThat(json).contains("\"dateTime\":\"2026-01-01T00:00:00Z\"");
    }
}
