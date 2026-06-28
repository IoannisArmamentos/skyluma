package com.skyluma.weather.forecast.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class WeatherControllerValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void returnsBadRequestWhenLatitudeIsTooHigh() throws Exception {
        mockMvc.perform(get("/api/weather")
                        .param("latitude", "999")
                        .param("longitude", "4.7005"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.messages[0]", containsString("Latitude")));
    }

    @Test
    void returnsBadRequestWhenLongitudeIsTooHigh() throws Exception {
        mockMvc.perform(get("/api/weather")
                        .param("latitude", "50.8798")
                        .param("longitude", "999"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.messages[0]", containsString("Longitude")));
    }

    @Test
    void returnsBadRequestWhenLatitudeIsMissing() throws Exception {
        mockMvc.perform(get("/api/weather")
                        .param("longitude", "4.7005"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.messages[0]").value("Missing required request parameter: latitude"));
    }

    @Test
    void returnsBadRequestWhenLongitudeIsMissing() throws Exception {
        mockMvc.perform(get("/api/weather")
                        .param("latitude", "50.8798"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.messages[0]").value("Missing required request parameter: longitude"));
    }

    @Test
    void returnsBadRequestWhenLatitudeIsNotANumber() throws Exception {
        mockMvc.perform(get("/api/weather")
                        .param("latitude", "abc")
                        .param("longitude", "4.7005"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.messages[0]").value("Invalid value for request parameter: latitude"));
    }

    @Test
    void returnsBadRequestWhenLongitudeIsNotANumber() throws Exception {
        mockMvc.perform(get("/api/weather")
                        .param("latitude", "50.8798")
                        .param("longitude", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.messages[0]").value("Invalid value for request parameter: longitude"));
    }
}