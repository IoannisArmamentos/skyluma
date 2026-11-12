export type WeatherProvider = 'openmeteo' | 'openweather';

export interface WeatherResponse {
  provider: WeatherProvider;
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
  date: string;
  /** Provider timestamp kept for API compatibility; display `date` for the location's forecast day. */
  dateTime: string;
  minTemperature: number;
  maxTemperature: number;
  humidity: number | null;
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
