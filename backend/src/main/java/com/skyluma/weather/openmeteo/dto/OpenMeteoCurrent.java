package com.skyluma.weather.openmeteo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record OpenMeteoCurrent(
        String time,

        @JsonProperty("temperature_2m")
        double temperature,

        @JsonProperty("apparent_temperature")
        double apparentTemperature,

        @JsonProperty("relative_humidity_2m")
        int relativeHumidity,

        @JsonProperty("weather_code")
        int weatherCode
) {
}
