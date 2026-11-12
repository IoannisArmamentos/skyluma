import { DatePipe, DecimalPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import {
  AfterViewInit,
  Component,
  ElementRef,
  OnDestroy,
  OnInit,
  computed,
  inject,
  signal,
  viewChild,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';

import { LeafletMap, Marker, TileLayer, type LatLngExpression } from 'leaflet';
import { Observable, Subject, catchError, map, of, switchMap } from 'rxjs';

import { WeatherApi } from '../weather-api';
import { WeatherProvider, WeatherResponse } from '../weather.models';

interface ProviderOption {
  value: WeatherProvider | '';
  label: string;
}

interface WeatherRequest {
  latitude: number;
  longitude: number;
  provider?: WeatherProvider;
}

type WeatherResult =
  | { weather: WeatherResponse }
  | { error: unknown };

const DEFAULT_LATITUDE = 50.8798;
const DEFAULT_LONGITUDE = 4.7005;

// Browsers do not count the time a permission prompt stays open towards the geolocation
// timeout, and Firefox never answers when the prompt is dismissed. This limit covers both.
export const LOCATION_TIMEOUT_MS = 10000;

const GEOLOCATION_PERMISSION_DENIED = 1;
const GEOLOCATION_TIMEOUT = 3;

@Component({
  selector: 'app-weather-page',
  imports: [DatePipe, DecimalPipe, FormsModule],
  templateUrl: './weather-page.html',
  styleUrl: './weather-page.scss',
})
export class WeatherPage implements OnInit, AfterViewInit, OnDestroy {
  private readonly weatherApi = inject(WeatherApi);

  readonly latitude = signal<number | null>(DEFAULT_LATITUDE);
  readonly longitude = signal<number | null>(DEFAULT_LONGITUDE);
  readonly selectedProvider = signal<WeatherProvider | ''>('');

  readonly providerOptions: ProviderOption[] = [
    { value: '', label: 'Default' },
    { value: 'openmeteo', label: 'Open-Meteo' },
    { value: 'openweather', label: 'OpenWeather' },
  ];

  readonly weather = signal<WeatherResponse | null>(null);
  readonly loading = signal(false);
  readonly locating = signal(false);
  readonly errorMessage = signal('');
  readonly locationMessage = signal('');

  readonly statusMessage = computed(() => {
    if (this.locating()) {
      return 'Finding your location...';
    }

    if (this.loading()) {
      return 'Loading weather...';
    }

    return this.locationMessage();
  });

  readonly providerLabel = computed(() => {
    const weather = this.weather();
    return weather ? this.getProviderLabel(weather.provider) : '';
  });

  readonly alertsAvailable = computed(() => this.weather()?.provider === 'openweather');

  private readonly mapElement = viewChild.required<ElementRef<HTMLElement>>('mapElement');
  private map?: LeafletMap;
  private marker?: Marker;

  private readonly weatherRequests = new Subject<WeatherRequest>();
  private pendingRequestKey = '';

  private locationAttempt = 0;
  private locationTimer?: ReturnType<typeof setTimeout>;
  private locationPermissionDenied = false;

  constructor() {
    // switchMap drops the previous request when a new one starts, which cancels the HTTP call
    // and stops a slow, older response from replacing newer weather.
    this.weatherRequests
      .pipe(
        switchMap((request) => this.fetchWeather(request)),
        takeUntilDestroyed(),
      )
      .subscribe((result) => this.applyWeatherResult(result));
  }

  ngOnInit(): void {
    this.useCurrentLocation();
  }

  ngAfterViewInit(): void {
    this.initMap();
  }

  ngOnDestroy(): void {
    this.finishLocating();
    this.map?.remove();
  }

  /** Loads weather for the coordinates the user entered or picked on the map. */
  loadWeather(): void {
    this.finishLocating();
    this.locationMessage.set('');
    this.requestWeather();
  }

  useCurrentLocation(): void {
    if (this.locating()) {
      return;
    }

    if (!navigator.geolocation) {
      this.handleLocationFailure('Geolocation is not supported by this browser.');
      return;
    }

    // A denied prompt is not shown again in this session. The browser settings are the
    // place to change that decision, and asking again only adds dismissed prompts.
    if (this.locationPermissionDenied) {
      this.locationMessage.set(
        'Location access was not allowed. Allow it in your browser settings to use your current location.',
      );
      return;
    }

    const attempt = ++this.locationAttempt;

    this.locating.set(true);
    this.locationMessage.set('');
    this.errorMessage.set('');

    this.locationTimer = setTimeout(() => {
      if (attempt === this.locationAttempt) {
        this.handleLocationFailure('Finding your location took too long.');
      }
    }, LOCATION_TIMEOUT_MS);

    navigator.geolocation.getCurrentPosition(
      (position) => {
        if (attempt !== this.locationAttempt) {
          return;
        }

        this.finishLocating();
        this.latitude.set(Number(position.coords.latitude.toFixed(4)));
        this.longitude.set(Number(position.coords.longitude.toFixed(4)));
        this.requestWeather();
      },
      (error) => {
        if (attempt !== this.locationAttempt) {
          return;
        }

        if (error.code === GEOLOCATION_PERMISSION_DENIED) {
          this.locationPermissionDenied = true;
          this.handleLocationFailure('Location access was not allowed.');
        } else if (error.code === GEOLOCATION_TIMEOUT) {
          this.handleLocationFailure('Finding your location took too long.');
        } else {
          this.handleLocationFailure('Your location is not available.');
        }
      },
      {
        enableHighAccuracy: false,
        timeout: LOCATION_TIMEOUT_MS,
        maximumAge: 300000,
      },
    );
  }

  getProviderLabel(provider: WeatherProvider): string {
    return this.providerOptions.find((option) => option.value === provider)?.label ?? provider;
  }

  getWeatherIcon(icon: string | null | undefined): string {
    if (!icon) {
      return '🌤️';
    }

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

  private requestWeather(): void {
    const coordinates = this.getValidCoordinates();

    if (!coordinates) {
      this.errorMessage.set(
        'Latitude must be between -90 and 90, and longitude must be between -180 and 180.',
      );
      return;
    }

    const { latitude, longitude } = coordinates;
    const request: WeatherRequest = {
      latitude,
      longitude,
      provider: this.selectedProvider() || undefined,
    };
    const requestKey = `${request.latitude},${request.longitude},${request.provider ?? ''}`;

    // The same request is already on its way, so a second click or submit adds nothing.
    if (this.loading() && requestKey === this.pendingRequestKey) {
      return;
    }

    this.pendingRequestKey = requestKey;
    this.loading.set(true);
    this.errorMessage.set('');
    this.updateMapMarker(latitude, longitude);
    this.weatherRequests.next(request);
  }

  private fetchWeather(request: WeatherRequest): Observable<WeatherResult> {
    return this.weatherApi.getWeather(request.latitude, request.longitude, request.provider).pipe(
      map((weather): WeatherResult => ({ weather })),
      catchError((error: unknown) => of<WeatherResult>({ error })),
    );
  }

  private applyWeatherResult(result: WeatherResult): void {
    this.loading.set(false);
    this.pendingRequestKey = '';

    if ('weather' in result) {
      this.weather.set(result.weather);
      return;
    }

    // Weather for the previous location must not look like the answer to this request.
    this.weather.set(null);
    this.errorMessage.set(this.getErrorMessage(result.error));
  }

  private handleLocationFailure(reason: string): void {
    this.finishLocating();

    // Only fall back while nothing is shown yet, so a failed retry keeps the current location.
    if (this.weather() || this.loading()) {
      this.locationMessage.set(`${reason} You can enter coordinates or click on the map.`);
      return;
    }

    this.latitude.set(DEFAULT_LATITUDE);
    this.longitude.set(DEFAULT_LONGITUDE);
    this.locationMessage.set(`${reason} Showing weather for the default location.`);
    this.requestWeather();
  }

  /** Ends the current location attempt so its late callbacks and timer are ignored. */
  private finishLocating(): void {
    this.locationAttempt++;
    clearTimeout(this.locationTimer);
    this.locationTimer = undefined;
    this.locating.set(false);
  }

  private initMap(): void {
    const latitude = this.latitude() ?? DEFAULT_LATITUDE;
    const longitude = this.longitude() ?? DEFAULT_LONGITUDE;

    this.map = new LeafletMap(this.mapElement().nativeElement).setView([latitude, longitude], 10);

    new TileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
    }).addTo(this.map);

    this.marker = new Marker([latitude, longitude]).addTo(this.map);

    this.map.on('click', (event) => {
      this.latitude.set(Number(event.latlng.lat.toFixed(4)));
      this.longitude.set(Number(event.latlng.lng.toFixed(4)));
      this.loadWeather();
    });

    setTimeout(() => {
      this.map?.invalidateSize();
    });
  }

  private updateMapMarker(latitude: number, longitude: number): void {
    const coordinates: LatLngExpression = [latitude, longitude];

    this.marker?.setLatLng(coordinates);
    this.map?.setView(coordinates);
    this.map?.invalidateSize();
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

  private getValidCoordinates(): { latitude: number; longitude: number } | null {
    const latitude = this.latitude();
    const longitude = this.longitude();

    // Empty number inputs give null, and Number(null) would quietly become 0.
    const valid = typeof latitude === 'number' &&
      typeof longitude === 'number' &&
      Number.isFinite(latitude) &&
      Number.isFinite(longitude) &&
      latitude >= -90 &&
      latitude <= 90 &&
      longitude >= -180 &&
      longitude <= 180;

    return valid ? { latitude, longitude } : null;
  }

  private getErrorMessage(error: unknown): string {
    if (error instanceof HttpErrorResponse && error.error?.messages?.length > 0) {
      return error.error.messages.join(' ');
    }

    return 'Could not load weather data.';
  }
}
