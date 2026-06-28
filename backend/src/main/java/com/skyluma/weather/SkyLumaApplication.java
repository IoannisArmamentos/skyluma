package com.skyluma.weather;

import com.skyluma.weather.config.CorsProperties;
import com.skyluma.weather.config.OpenMeteoProperties;
import com.skyluma.weather.config.OpenWeatherProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({
        OpenWeatherProperties.class,
        CorsProperties.class,
        OpenMeteoProperties.class
})
public class SkyLumaApplication {

    public static void main(String[] args) {
        SpringApplication.run(SkyLumaApplication.class, args);
    }
}