import { DatePipe, DecimalPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectorRef, Component } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { finalize } from 'rxjs';

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

  constructor(
    private readonly weatherApi: WeatherApi,
    private readonly changeDetectorRef: ChangeDetectorRef,
  ) {}

  loadWeather(): void {
    if (!this.areCoordinatesValid()) {
      this.errorMessage = 'Latitude must be between -90 and 90, and longitude must be between -180 and 180.';
      this.changeDetectorRef.markForCheck();
      return;
    }

    this.loading = true;
    this.errorMessage = '';
    this.changeDetectorRef.markForCheck();

    const provider = this.selectedProvider || undefined;

    this.weatherApi.getWeather(Number(this.latitude), Number(this.longitude), provider)
      .pipe(
        finalize(() => {
          this.loading = false;
          this.changeDetectorRef.markForCheck();
        }),
      )
      .subscribe({
        next: (weather: WeatherResponse) => {
          this.weather = weather;
          this.changeDetectorRef.markForCheck();
        },
        error: (error: unknown) => {
          this.errorMessage = this.getErrorMessage(error);
          this.changeDetectorRef.markForCheck();
        },
      });
  }

  useCurrentLocation(): void {
    if (!navigator.geolocation) {
      this.errorMessage = 'Geolocation is not supported by this browser.';
      this.changeDetectorRef.markForCheck();
      return;
    }

    this.locating = true;
    this.errorMessage = '';
    this.changeDetectorRef.markForCheck();

    navigator.geolocation.getCurrentPosition(
      (position) => {
        this.latitude = Number(position.coords.latitude.toFixed(4));
        this.longitude = Number(position.coords.longitude.toFixed(4));
        this.locating = false;
        this.changeDetectorRef.markForCheck();
        this.loadWeather();
      },
      () => {
        this.errorMessage = 'Could not get your current location.';
        this.locating = false;
        this.changeDetectorRef.markForCheck();
      },
      {
        enableHighAccuracy: false,
        timeout: 10000,
        maximumAge: 300000,
      },
    );
  }

  getProviderLabel(provider: WeatherProvider): string {
    return this.providerOptions.find((option) => option.value === provider)?.label ?? provider;
  }

  areAlertsAvailableForProvider(provider: WeatherProvider): boolean {
    return provider === 'openweather';
  }

  getWeatherIcon(icon: string): string {
    if (this.isOpenWeatherClear(icon)) {
      return '☀️';
    }

    if (this.isOpenWeatherCloudy(icon)) {
      return '☁️';
    }

    if (this.isOpenWeatherRain(icon)) {
      return '🌧️';
    }

    if (this.isOpenWeatherSnow(icon)) {
      return '❄️';
    }

    if (this.isOpenWeatherThunderstorm(icon)) {
      return '⛈️';
    }

    if (this.isOpenMeteoClear(icon)) {
      return '☀️';
    }

    if (this.isOpenMeteoPartlyCloudy(icon)) {
      return '⛅';
    }

    if (this.isOpenMeteoFog(icon)) {
      return '🌫️';
    }

    if (this.isOpenMeteoRain(icon)) {
      return '🌧️';
    }

    if (this.isOpenMeteoSnow(icon)) {
      return '❄️';
    }

    if (this.isOpenMeteoThunderstorm(icon)) {
      return '⛈️';
    }

    return '🌤️';
  }

  private isOpenWeatherClear(icon: string): boolean {
    return icon.startsWith('01');
  }

  private isOpenWeatherCloudy(icon: string): boolean {
    return ['02', '03', '04', '50'].some((code) => icon.startsWith(code));
  }

  private isOpenWeatherRain(icon: string): boolean {
    return ['09', '10'].some((code) => icon.startsWith(code));
  }

  private isOpenWeatherSnow(icon: string): boolean {
    return icon.startsWith('13');
  }

  private isOpenWeatherThunderstorm(icon: string): boolean {
    return icon.startsWith('11');
  }

  private isOpenMeteoClear(icon: string): boolean {
    return icon === '0';
  }

  private isOpenMeteoPartlyCloudy(icon: string): boolean {
    return ['1', '2', '3'].includes(icon);
  }

  private isOpenMeteoFog(icon: string): boolean {
    return ['45', '48'].includes(icon);
  }

  private isOpenMeteoRain(icon: string): boolean {
    return [
      '51',
      '53',
      '55',
      '56',
      '57',
      '61',
      '63',
      '65',
      '66',
      '67',
      '80',
      '81',
      '82',
    ].includes(icon);
  }

  private isOpenMeteoSnow(icon: string): boolean {
    return [
      '71',
      '73',
      '75',
      '77',
      '85',
      '86',
    ].includes(icon);
  }

  private isOpenMeteoThunderstorm(icon: string): boolean {
    return ['95', '96', '99'].includes(icon);
  }

  private areCoordinatesValid(): boolean {
    const latitude = Number(this.latitude);
    const longitude = Number(this.longitude);

    return Number.isFinite(latitude) &&
      Number.isFinite(longitude) &&
      latitude >= -90 &&
      latitude <= 90 &&
      longitude >= -180 &&
      longitude <= 180;
  }

  private getErrorMessage(error: unknown): string {
    if (error instanceof HttpErrorResponse && error.error?.messages?.length > 0) {
      return error.error.messages.join(' ');
    }

    return 'Could not load weather data.';
  }
}
