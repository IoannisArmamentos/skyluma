package com.skyluma.weather.openmeteo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record OpenMeteoResponse(
        double latitude,
        double longitude,
        OpenMeteoCurrent current,
        OpenMeteoDaily daily,
        String timezone,
        @JsonProperty("utc_offset_seconds") Integer utcOffsetSeconds,
        OpenMeteoHourly hourly
) {
    public OpenMeteoResponse(double latitude, double longitude, OpenMeteoCurrent current, OpenMeteoDaily daily) {
        this(latitude, longitude, current, daily, null, null, null);
    }
}
