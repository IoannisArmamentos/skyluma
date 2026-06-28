export interface WeatherResponse {
  location: LocationResponse;
  current: CurrentWeatherResponse;
  daily: DailyForecastResponse[];
  alerts: WeatherAlertResponse[];
}

export interface LocationResponse {
  latitude: number;
  longitude: number;
}

export interface CurrentWeatherResponse {
  temperature: number;
  feelsLike: number;
  humidity: number;
  description: string;
  icon: string;
}

export interface DailyForecastResponse {
  dateTime: string;
  minTemperature: number;
  maxTemperature: number;
  humidity: number;
  description: string;
  icon: string;
}

export interface WeatherAlertResponse {
  senderName: string;
  event: string;
  start: string;
  end: string;
  description: string;
}
