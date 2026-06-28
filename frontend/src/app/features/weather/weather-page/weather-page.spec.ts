import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Observable, of, throwError } from 'rxjs';

import { WeatherApi } from '../weather-api';
import { WeatherProvider, WeatherResponse } from '../weather.models';
import { WeatherPage } from './weather-page';

import * as L from 'leaflet';

interface WeatherApiMock {
  calls: Array<{
    latitude: number;
    longitude: number;
    provider?: WeatherProvider;
  }>;
  response: Observable<WeatherResponse>;
  getWeather(
    latitude: number,
    longitude: number,
    provider?: WeatherProvider,
  ): Observable<WeatherResponse>;
}

describe('WeatherPage', () => {
  let fixture: ComponentFixture<WeatherPage>;
  let component: WeatherPage;
  let weatherApi: WeatherApiMock;

  const weatherResponse: WeatherResponse = {
    provider: 'openmeteo',
    location: {
      latitude: 50.8798,
      longitude: 4.7005,
    },
    current: {
      temperature: 21.5,
      feelsLike: 20.8,
      humidity: 65,
      description: 'Partly cloudy',
      icon: '1',
    },
    daily: [
      {
        dateTime: '2026-06-28T00:00:00Z',
        minTemperature: 12.4,
        maxTemperature: 24.8,
        humidity: null,
        description: 'Rain',
        icon: '61',
      },
    ],
    alerts: [],
  };

  beforeEach(async () => {
    weatherApi = {
      calls: [],
      response: of(weatherResponse),
      getWeather(
        latitude: number,
        longitude: number,
        provider?: WeatherProvider,
      ): Observable<WeatherResponse> {
        this.calls.push({ latitude, longitude, provider });
        return this.response;
      },
    };

    mockSuccessfulGeolocation();

    await TestBed.configureTestingModule({
      imports: [WeatherPage],
      providers: [
        {
          provide: WeatherApi,
          useValue: weatherApi,
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(WeatherPage);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('loads weather for the current location when the page opens', () => {
    expect(weatherApi.calls).toEqual([
      {
        latitude: 50.8798,
        longitude: 4.7005,
        provider: undefined,
      },
    ]);

    expect(component.weather).toEqual(weatherResponse);
  });

  it('renders current weather data', () => {
    const page = fixture.nativeElement as HTMLElement;

    expect(page.textContent).toContain('Current weather');
    expect(page.textContent).toContain('21.5°C');
    expect(page.textContent).toContain('Partly cloudy');
    expect(page.textContent).toContain('Provider: Open-Meteo');
    expect(page.textContent).toContain('Humidity 65%');
  });

  it('renders daily forecast data', () => {
    const page = fixture.nativeElement as HTMLElement;

    expect(page.textContent).toContain('Daily forecast');
    expect(page.textContent).toContain('Rain');
    expect(page.textContent).toContain('12.4°C / 24.8°C');
  });

  it('shows a message when alerts are not available from the provider', () => {
    const page = fixture.nativeElement as HTMLElement;

    expect(page.textContent).toContain('Weather alerts are not available from Open-Meteo yet.');
  });

  it('loads weather when the Get weather button is clicked', () => {
    weatherApi.calls = [];

    const button = getButtonByText('Get weather');

    button.click();
    fixture.detectChanges();

    expect(weatherApi.calls).toEqual([
      {
        latitude: 50.8798,
        longitude: 4.7005,
        provider: undefined,
      },
    ]);

    expect(component.weather).toEqual(weatherResponse);
  });

  it('shows an error when coordinates are invalid', () => {
    weatherApi.calls = [];

    component.latitude = 100;
    component.longitude = 4.7005;

    component.loadWeather();
    fixture.detectChanges();

    expect(weatherApi.calls).toEqual([]);
    expect(component.errorMessage).toBe(
      'Latitude must be between -90 and 90, and longitude must be between -180 and 180.',
    );
  });

  it('shows an error when weather loading fails', () => {
    weatherApi.calls = [];
    weatherApi.response = throwError(() => new Error('Request failed'));

    component.loadWeather();
    fixture.detectChanges();

    expect(component.errorMessage).toBe('Could not load weather data.');
  });

  it('uses browser coordinates when current location is available', () => {
    expect(weatherApi.calls).toEqual([
      {
        latitude: 50.8798,
        longitude: 4.7005,
        provider: undefined,
      },
    ]);
  });

  it('shows an error when current location fails', async () => {
    mockFailedGeolocation();

    fixture = TestBed.createComponent(WeatherPage);
    component = fixture.componentInstance;
    fixture.detectChanges();

    expect(component.errorMessage).toBe(
      'Location access was not allowed. You can enter coordinates manually.',
    );
    expect(component.locating).toBe(false);
  });

  it('shows an error when geolocation is not supported', () => {
    mockMissingGeolocation();

    fixture = TestBed.createComponent(WeatherPage);
    component = fixture.componentInstance;
    fixture.detectChanges();

    expect(component.errorMessage).toBe('Geolocation is not supported by this browser.');
    expect(component.locating).toBe(false);
  });

  it('renders the location map', () => {
    const page = fixture.nativeElement as HTMLElement;

    expect(page.textContent).toContain('Map');
    expect(page.textContent).toContain('Click on the map to select a location.');
    expect(page.querySelector('#weather-map')).not.toBeNull();
  });

  it('loads weather when the map is clicked', () => {
    weatherApi.calls = [];

    const map = (component as unknown as { map: L.Map }).map;

    map.fire('click', {
      latlng: L.latLng(51.12346, 4.98766),
    });

    fixture.detectChanges();

    expect(component.latitude).toBe(51.1235);
    expect(component.longitude).toBe(4.9877);
    expect(weatherApi.calls).toEqual([
      {
        latitude: 51.1235,
        longitude: 4.9877,
        provider: undefined,
      },
    ]);
  });

  function getButtonByText(text: string): HTMLButtonElement {
    const buttons = Array.from(
      fixture.nativeElement.querySelectorAll('button'),
    ) as HTMLButtonElement[];

    const button = buttons.find((candidate) => candidate.textContent?.trim() === text);

    if (!button) {
      throw new Error(`Button not found: ${text}`);
    }

    return button;
  }

  function mockSuccessfulGeolocation(latitude = 50.8798, longitude = 4.7005): void {
    Object.defineProperty(navigator, 'geolocation', {
      configurable: true,
      value: {
        getCurrentPosition: (
          success: PositionCallback,
        ) => success({
          coords: {
            latitude,
            longitude,
            accuracy: 1,
            altitude: null,
            altitudeAccuracy: null,
            heading: null,
            speed: null,
          },
          timestamp: Date.now(),
        } as GeolocationPosition),
      },
    });
  }

  function mockFailedGeolocation(): void {
    Object.defineProperty(navigator, 'geolocation', {
      configurable: true,
      value: {
        getCurrentPosition: (
          _success: PositionCallback,
          error: PositionErrorCallback,
        ) => error({
          code: 1,
          message: 'Permission denied',
          PERMISSION_DENIED: 1,
          POSITION_UNAVAILABLE: 2,
          TIMEOUT: 3,
        } as GeolocationPositionError),
      },
    });
  }

  function mockMissingGeolocation(): void {
    Object.defineProperty(navigator, 'geolocation', {
      configurable: true,
      value: undefined,
    });
  }
});
