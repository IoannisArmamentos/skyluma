import { DatePipe, DecimalPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { WeatherApi } from '../weather-api';
import { WeatherProvider, WeatherResponse } from '../weather.models';

interface ProviderOption {
  value: WeatherProvider | '';
  label: string;
}

@Component({
  selector: 'app-weather-page',
  imports: [DatePipe, DecimalPipe, FormsModule],
  templateUrl: './weather-page.html',
  styleUrl: './weather-page.scss',
})
export class WeatherPage {
  latitude = 50.8798;
  longitude = 4.7005;
  selectedProvider: WeatherProvider | '' = '';

  readonly providerOptions: ProviderOption[] = [
    { value: '', label: 'Default' },
    { value: 'openmeteo', label: 'Open-Meteo' },
    { value: 'openweather', label: 'OpenWeather' },
  ];

  weather?: WeatherResponse;
  loading = false;
  locating = false;
  errorMessage = '';

  constructor(private readonly weatherApi: WeatherApi) {}

  loadWeather(): void {
    this.loading = true;
    this.errorMessage = '';

    const provider = this.selectedProvider || undefined;

    this.weatherApi.getWeather(this.latitude, this.longitude, provider).subscribe({
      next: (weather) => {
        this.weather = weather;
        this.loading = false;
      },
      error: (error: unknown) => {
        this.errorMessage = this.getErrorMessage(error);
        this.loading = false;
      },
    });
  }

  useCurrentLocation(): void {
    if (!navigator.geolocation) {
      this.errorMessage = 'Geolocation is not supported by this browser.';
      return;
    }

    this.locating = true;
    this.errorMessage = '';

    navigator.geolocation.getCurrentPosition(
      (position) => {
        this.latitude = Number(position.coords.latitude.toFixed(4));
        this.longitude = Number(position.coords.longitude.toFixed(4));
        this.locating = false;
        this.loadWeather();
      },
      () => {
        this.errorMessage = 'Could not get your current location.';
        this.locating = false;
      },
    );
  }

  getProviderLabel(provider: WeatherProvider): string {
    return this.providerOptions.find((option) => option.value === provider)?.label ?? provider;
  }

  private getErrorMessage(error: unknown): string {
    if (error instanceof HttpErrorResponse && error.error?.messages?.length > 0) {
      return error.error.messages.join(' ');
    }

    return 'Could not load weather data.';
  }
}
