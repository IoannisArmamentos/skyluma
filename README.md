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

## Health endpoint

    GET /api/health

Response:

    {
      "status": "UP"
    }

## Planned features

- Weather forecast endpoint
- OpenWeather integration
- Safe API key configuration
- Angular frontend
- Map-based location selection
- Current weather, daily forecast, and weather alerts
