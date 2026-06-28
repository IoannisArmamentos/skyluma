import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';

import { Observable } from 'rxjs';

import { WeatherResponse } from './weather.models';

@Injectable({
  providedIn: 'root',
})
export class WeatherApi {
  private readonly apiUrl = '/api/weather';

  constructor(private readonly http: HttpClient) {}

  getWeather(latitude: number, longitude: number): Observable<WeatherResponse> {
    const params = new HttpParams()
      .set('latitude', latitude)
      .set('longitude', longitude);

    return this.http.get<WeatherResponse>(this.apiUrl, { params });
  }
}
