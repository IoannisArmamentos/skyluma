package com.skyluma.weather.forecast.provider;

import java.util.Arrays;

public enum WeatherProviderType {
    OPENMETEO("openmeteo"),
    OPENWEATHER("openweather");

    private final String value;

    WeatherProviderType(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static WeatherProviderType fromValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return Arrays.stream(values())
                .filter(provider -> provider.value.equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported weather provider: " + value));
    }
}