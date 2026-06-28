import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';

import { Observable } from 'rxjs';

import { WeatherProvider, WeatherResponse } from './weather.models';

@Injectable({
  providedIn: 'root',
})
export class WeatherApi {
  private readonly apiUrl = '/api/weather';

  constructor(private readonly http: HttpClient) {}

  getWeather(
    latitude: number,
    longitude: number,
    provider?: WeatherProvider,
  ): Observable<WeatherResponse> {
    let params = new HttpParams()
      .set('latitude', latitude)
      .set('longitude', longitude);

    if (provider) {
      params = params.set('provider', provider);
    }

    return this.http.get<WeatherResponse>(this.apiUrl, { params });
  }
}
