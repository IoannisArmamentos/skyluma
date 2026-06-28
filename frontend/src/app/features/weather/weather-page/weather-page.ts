import { DatePipe, DecimalPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { WeatherApi } from '../weather-api';
import { WeatherResponse } from '../weather.models';

@Component({
  selector: 'app-weather-page',
  imports: [DatePipe, DecimalPipe, FormsModule],
  templateUrl: './weather-page.html',
  styleUrl: './weather-page.scss',
})
export class WeatherPage {
  latitude = 50.8798;
  longitude = 4.7005;

  weather?: WeatherResponse;
  loading = false;
  errorMessage = '';

  constructor(private readonly weatherApi: WeatherApi) {}

  loadWeather(): void {
    this.loading = true;
    this.errorMessage = '';

    this.weatherApi.getWeather(this.latitude, this.longitude).subscribe({
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

  private getErrorMessage(error: unknown): string {
    if (error instanceof HttpErrorResponse && error.error?.messages?.length > 0) {
      return error.error.messages.join(' ');
    }

    return 'Could not load weather data.';
  }
}
