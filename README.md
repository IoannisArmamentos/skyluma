# SkyLuma

SkyLuma is a weather forecast app built with Spring Boot and Angular.

The project is based on an older weather app, but it is not a direct migration. The goal is to build a cleaner and more modern version with a simple backend, a REST API, and a separate Angular frontend.

## Project structure

```text
SkyLuma/
  backend/   Spring Boot REST API
  frontend/  Angular app
```

## Tech stack

### Backend

1. Java 21
2. Spring Boot
3. Maven
4. Lombok
5. GitHub Actions CI

### Frontend

1. Angular
2. TypeScript
3. SCSS
4. Angular dev proxy
5. GitHub Actions CI

## Features

1. Current weather by latitude and longitude
2. Daily forecast
3. Weather provider selection
4. Default weather provider from backend config
5. Provider override from the frontend
6. Current browser location
7. Coordinate validation
8. Weather alert message based on the selected provider

## Weather providers

SkyLuma currently supports:

1. Open Meteo
2. OpenWeather

Open Meteo is the default provider because it works without an API key for basic forecast data.

OpenWeather is optional and requires an API key.

## Weather alerts

Weather alert support depends on the selected provider.

The current Open Meteo implementation gives forecast data, but it does not give official weather alerts.

OpenWeather can return weather alerts when the API returns active alerts for the selected location.

The frontend shows a clear message when weather alerts are not available from the selected provider. This avoids saying that there are no alerts when the provider simply does not support them.

## Running locally

Run the backend and frontend in separate terminals.

### Backend

From the project root:

```bash
cd backend
./mvnw spring-boot:run
```

The backend starts on:

```text
http://localhost:8080
```

Health check:

```bash
curl http://localhost:8080/api/health
```

Weather endpoint with the default provider:

```bash
curl "http://localhost:8080/api/weather?latitude=50.8798&longitude=4.7005"
```

Weather endpoint with a selected provider:

```bash
curl "http://localhost:8080/api/weather?latitude=50.8798&longitude=4.7005&provider=openmeteo"
```

```bash
curl "http://localhost:8080/api/weather?latitude=50.8798&longitude=4.7005&provider=openweather"
```

### Frontend

From the project root:

```bash
cd frontend
npm install
npm start
```

The frontend starts on:

```text
http://localhost:4200
```

During local development, Angular sends `/api` requests to the backend.

## Configuration

Open Meteo is the default provider.

Example backend config:

```yaml
skyluma:
  weather:
    provider: openmeteo
```

Supported provider values:

```text
openmeteo
openweather
```

### OpenWeather API key

OpenWeather requires an API key.

Set this environment variable before running the backend if you want to use OpenWeather:

```bash
export OPENWEATHER_API_KEY=your_openweather_api_key
```

The backend reads OpenWeather settings from `application.yaml`:

```yaml
openweather:
  base-url: https://api.openweathermap.org/data/3.0
  api-key: ${OPENWEATHER_API_KEY:}
  units: metric
  language: en
```

Do not commit API keys or secrets.

## API endpoints

### Health

```http
GET /api/health
```

Example response:

```json
{
  "status": "UP"
}
```

### Weather

```http
GET /api/weather?latitude=50.8798&longitude=4.7005
```

With provider:

```http
GET /api/weather?latitude=50.8798&longitude=4.7005&provider=openmeteo
```

```http
GET /api/weather?latitude=50.8798&longitude=4.7005&provider=openweather
```

The weather response includes:

1. selected provider
2. location
3. current weather
4. daily forecast
5. weather alerts when the selected provider gives them

## Tests and checks

### Backend tests

From the project root:

```bash
cd backend
./mvnw test
```

### Frontend build

From the project root:

```bash
cd frontend
npm run build
```

## Development notes

1. Keep pull requests small and focused.
2. Keep backend provider values stable.
3. Do not make the frontend depend on backend error message text.
4. Add stable error codes later if the frontend needs to react to specific backend errors.
5. Keep frontend UI text in the frontend.
6. Add frontend translations later if we need more languages.
7. Do not commit API keys, secrets, tokens, or local environment files.
