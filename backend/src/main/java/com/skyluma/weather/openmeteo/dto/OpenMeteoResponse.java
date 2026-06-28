package com.skyluma.weather.openmeteo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record OpenMeteoResponse(
        double latitude,
        double longitude,
        OpenMeteoCurrent current,
        OpenMeteoDaily daily
) {
}