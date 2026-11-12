package com.skyluma.weather.openmeteo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record OpenMeteoCurrent(
        String time,

        @JsonProperty("temperature_2m")
        Double temperature,

        @JsonProperty("apparent_temperature")
        Double apparentTemperature,

        @JsonProperty("relative_humidity_2m")
        Integer relativeHumidity,

        @JsonProperty("weather_code")
        Integer weatherCode
) {
}
