# SkyLuma

SkyLuma is a weather forecast application built with Spring Boot and Angular.

The project is a modern rewrite of an older weather forecast application, with a cleaner backend architecture, REST API design, safe configuration handling, and a separate frontend application.

## Project structure

SkyLuma/
  backend/   Spring Boot REST API
  frontend/  Angular application

## Backend

The backend is built with:

- Java 21
- Spring Boot
- Maven
- GitHub Actions CI

## Run tests

From the project root:

    cd backend
    ./mvnw test

## Run the application

From the project root:

    cd backend
    ./mvnw spring-boot:run

The backend starts on:

    http://localhost:8080

## Configuration

The backend requires an OpenWeather API key.

Set the following environment variable before running the application:

    export OPENWEATHER_API_KEY=your_openweather_api_key

Then start the backend:

    cd backend
    ./mvnw spring-boot:run

The backend reads OpenWeather settings from `application.yaml`:

    openweather:
      base-url: https://api.openweathermap.org/data/3.0
      api-key: ${OPENWEATHER_API_KEY:}
      units: metric
      language: en

The API key must not be committed to source control.

## Health endpoint

    GET /api/health

Response:

    {
      "status": "UP"
    }

## Planned features

- Weather forecast endpoint
- OpenWeather integration
- Angular frontend
- Map-based location selection
- Improved weather UI
- Home Assistant integration example
