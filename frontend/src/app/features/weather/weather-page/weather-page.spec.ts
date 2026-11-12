import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Observable, Subject, of, throwError } from 'rxjs';

import { WeatherApi } from '../weather-api';
import { WeatherProvider, WeatherResponse } from '../weather.models';
import { LOCATION_TIMEOUT_MS, WeatherPage } from './weather-page';

import { LatLng, LeafletMap, Marker, TileLayer } from 'leaflet';

interface WeatherCall {
  latitude: number;
  longitude: number;
  provider?: WeatherProvider;
}

interface WeatherApiMock {
  calls: WeatherCall[];
  respond: (call: WeatherCall) => Observable<WeatherResponse>;
  getWeather(
    latitude: number,
    longitude: number,
    provider?: WeatherProvider,
  ): Observable<WeatherResponse>;
}

type GeolocationMode = 'allow' | 'deny' | 'unavailable' | 'timeout' | 'pending';

interface GeolocationFake {
  requests: number;
  mode: GeolocationMode;
  position: { latitude: number; longitude: number };
  pendingSuccess?: PositionCallback;
}

const DEFAULT_CALL: WeatherCall = { latitude: 50.8798, longitude: 4.7005, provider: undefined };
const BROWSER_POSITION = { latitude: 48.8566, longitude: 2.3522 };
const BROWSER_CALL: WeatherCall = { ...BROWSER_POSITION, provider: undefined };

describe('WeatherPage', () => {
  let fixture: ComponentFixture<WeatherPage>;
  let component: WeatherPage;
  let weatherApi: WeatherApiMock;
  let geolocation: GeolocationFake;

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
        date: '2026-06-28',
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
    vi.stubGlobal('ResizeObserver', class {
      observe(): void {}
      disconnect(): void {}
    });

    weatherApi = {
      calls: [],
      respond: () => of(weatherResponse),
      getWeather(latitude, longitude, provider) {
        const call = { latitude, longitude, provider };
        this.calls.push(call);
        return this.respond(call);
      },
    };

    installGeolocation('allow');

    await TestBed.configureTestingModule({
      imports: [WeatherPage],
      providers: [
        {
          provide: WeatherApi,
          useValue: weatherApi,
        },
      ],
    }).compileComponents();
  });

  afterEach(() => {
    vi.useRealTimers();
    vi.unstubAllGlobals();
  });

  describe('with location access allowed', () => {
    beforeEach(() => {
      createPage();
    });

    it('loads weather for the browser location when the page opens', () => {
      expect(geolocation.requests).toBe(1);
      expect(weatherApi.calls).toEqual([BROWSER_CALL]);
      expect(component.latitude()).toBe(48.8566);
      expect(component.longitude()).toBe(2.3522);
      expect(component.weather()).toEqual(weatherResponse);
      expect(component.locating()).toBe(false);
      expect(component.loading()).toBe(false);
    });

    it('renders current weather data', () => {
      const page = getPage();

      expect(page.textContent).toContain('Current weather');
      expect(page.textContent).toContain('21.5°C');
      expect(page.textContent).toContain('Partly cloudy');
      expect(page.textContent).toContain('Provider: Open-Meteo');
      expect(page.textContent).toContain('Humidity 65%');
    });

    it('renders daily forecast data', () => {
      const page = getPage();

      expect(page.textContent).toContain('Daily forecast');
      expect(page.textContent).toContain('Rain');
      expect(page.textContent).toContain('12.4°C / 24.8°C');
      expect(page.querySelector('.forecast-card h3')?.textContent).toBe('Sun, Jun 28');
    });

    it('renders calendar dates across a year boundary', () => {
      weatherApi.respond = () => of({
        ...weatherResponse,
        daily: [
          { ...weatherResponse.daily[0], date: '2025-12-31', dateTime: '2025-12-31T00:00:00Z' },
          { ...weatherResponse.daily[0], date: '2026-01-01', dateTime: '2026-01-01T00:00:00Z' },
        ],
      });

      component.loadWeather();
      fixture.detectChanges();

      const headings = Array.from(
        getPage().querySelectorAll('.forecast-card h3'),
        (heading) => heading.textContent,
      );
      expect(headings).toEqual(['Wed, Dec 31', 'Thu, Jan 1']);
    });

    it.each([
      ['America/Los_Angeles', '2026-01-01', '2026-01-01T00:00:00Z', 'Thu, Jan 1'],
      ['Pacific/Kiritimati', '2025-12-31', '2025-12-31T23:30:00Z', 'Wed, Dec 31'],
    ])('renders the forecast date in browser timezone %s', (timezone, date, dateTime, expected) => {
      vi.stubEnv('TZ', timezone);
      try {
        weatherApi.respond = () => of({
          ...weatherResponse,
          daily: [{ ...weatherResponse.daily[0], date, dateTime }],
        });

        component.loadWeather();
        fixture.detectChanges();

        const heading = getPage().querySelector('.forecast-card h3');
        expect(heading?.textContent).toBe(expected);
      } finally {
        vi.unstubAllEnvs();
      }
    });

    it('shows a message when alerts are not available from the provider', () => {
      expect(getPage().textContent).toContain('Weather alerts are not available from Open-Meteo yet.');
    });

    it('shows a message when the forecast has no days', () => {
      weatherApi.respond = () => of({ ...weatherResponse, daily: [] });

      component.loadWeather();
      fixture.detectChanges();

      expect(getPage().textContent).toContain('No daily forecast is available for this location.');
    });

    it('renders weather without icons or descriptions', () => {
      weatherApi.respond = () => of({
        ...weatherResponse,
        provider: 'openweather',
        current: { ...weatherResponse.current, icon: null, description: null },
        daily: [{ ...weatherResponse.daily[0], icon: null, description: null }],
        alerts: [{
          senderName: 'KMI',
          event: 'Wind',
          start: '2026-06-28T10:00:00Z',
          end: '2026-06-28T18:00:00Z',
          description: null,
        }],
      });

      component.loadWeather();
      fixture.detectChanges();

      const page = getPage();
      expect(component.errorMessage()).toBe('');
      expect(page.querySelector('.weather-icon')?.textContent).toBe('🌤️');
      expect(page.querySelector('.forecast-icon')?.textContent).toBe('🌤️');
      expect(page.textContent).toContain('21.5°C');
      expect(page.textContent).not.toContain('null');
      expect(page.querySelector('.alert-card h3')?.textContent).toBe('Wind');
    });

    it('loads weather when the Get weather button is clicked', () => {
      weatherApi.calls = [];

      getButtonByText('Get weather').click();
      fixture.detectChanges();

      expect(weatherApi.calls).toEqual([BROWSER_CALL]);
      expect(component.weather()).toEqual(weatherResponse);
    });

    it('submits the coordinate form with the Enter key', () => {
      weatherApi.calls = [];

      setInputValue('latitude', '40.7128');
      setInputValue('longitude', '-74.006');

      // Pressing Enter in a field submits the form through its submit button.
      const form = getPage().querySelector('form') as HTMLFormElement;
      const submitButton = form.querySelector('button[type="submit"]') as HTMLButtonElement;
      expect(submitButton.textContent?.trim()).toBe('Get weather');

      form.requestSubmit();
      fixture.detectChanges();

      expect(weatherApi.calls).toEqual([
        { latitude: 40.7128, longitude: -74.006, provider: undefined },
      ]);
    });

    it('sends the selected provider', () => {
      weatherApi.calls = [];

      component.selectedProvider.set('openweather');
      component.loadWeather();

      expect(weatherApi.calls).toEqual([{ ...BROWSER_CALL, provider: 'openweather' }]);
    });

    it('shows an error when coordinates are invalid', () => {
      weatherApi.calls = [];

      component.latitude.set(100);
      component.loadWeather();
      fixture.detectChanges();

      expect(weatherApi.calls).toEqual([]);
      expect(component.errorMessage()).toBe(
        'Latitude must be between -90 and 90, and longitude must be between -180 and 180.',
      );
    });

    it('treats an empty coordinate field as invalid instead of zero', () => {
      weatherApi.calls = [];

      setInputValue('latitude', '');
      (getPage().querySelector('form') as HTMLFormElement).requestSubmit();
      fixture.detectChanges();

      expect(component.latitude()).toBeNull();
      expect(weatherApi.calls).toEqual([]);
      expect(getPage().querySelector('[role="alert"]')?.textContent).toContain(
        'Latitude must be between -90 and 90',
      );
    });

    it('shows an error and removes earlier weather when loading fails', () => {
      weatherApi.respond = () => throwError(() => new Error('Request failed'));

      component.loadWeather();
      fixture.detectChanges();

      const page = getPage();
      expect(component.errorMessage()).toBe('Could not load weather data.');
      expect(page.querySelector('[role="alert"]')?.textContent).toBe('Could not load weather data.');
      expect(component.weather()).toBeNull();
      expect(page.textContent).not.toContain('Current weather');
      expect(component.loading()).toBe(false);
    });

    it('shows the messages from a backend error response', () => {
      weatherApi.respond = () => throwError(() => new HttpErrorResponse({
        status: 400,
        error: { messages: ['Latitude is invalid.', 'Longitude is invalid.'] },
      }));

      component.loadWeather();

      expect(component.errorMessage()).toBe('Latitude is invalid. Longitude is invalid.');
    });

    it('recovers after a failed request', () => {
      weatherApi.respond = () => throwError(() => new Error('Request failed'));
      component.loadWeather();

      weatherApi.respond = () => of(weatherResponse);
      component.loadWeather();
      fixture.detectChanges();

      expect(component.errorMessage()).toBe('');
      expect(component.weather()).toEqual(weatherResponse);
      expect(getPage().querySelector('[role="alert"]')).toBeNull();
    });

    it('tracks the loading state of a request', () => {
      const response = new Subject<WeatherResponse>();
      weatherApi.respond = () => response;

      component.loadWeather();
      fixture.detectChanges();

      expect(component.loading()).toBe(true);
      expect(getButtonByText('Loading...')).toBeDefined();
      expect(getStatus()).toBe('Loading weather...');
      expect(getPage().querySelector('.weather-results')?.getAttribute('aria-busy')).toBe('true');

      response.next({ ...weatherResponse, current: { ...weatherResponse.current, temperature: 3 } });
      fixture.detectChanges();

      expect(component.loading()).toBe(false);
      expect(getButtonByText('Get weather')).toBeDefined();
      expect(getStatus()).toBe('');
      expect(getPage().textContent).toContain('3°C');
    });

    it('ends the loading state when a request fails', () => {
      const response = new Subject<WeatherResponse>();
      weatherApi.respond = () => response;

      component.loadWeather();
      expect(component.loading()).toBe(true);

      response.error(new Error('Request failed'));
      fixture.detectChanges();

      expect(component.loading()).toBe(false);
      expect(getStatus()).toBe('');
    });

    it('keeps only the newest weather when locations change quickly', () => {
      const responses: Subject<WeatherResponse>[] = [];
      weatherApi.respond = () => {
        const response = new Subject<WeatherResponse>();
        responses.push(response);
        return response;
      };

      selectMapLocation(51.1, 4.1);
      selectMapLocation(52.2, 5.2);
      selectMapLocation(53.3, 6.3);

      expect(responses.map((response) => response.observed)).toEqual([false, false, true]);

      responses[2].next({ ...weatherResponse, location: { latitude: 53.3, longitude: 6.3 } });
      // A late answer for an earlier location has no subscriber and changes nothing.
      responses[0].next({ ...weatherResponse, location: { latitude: 51.1, longitude: 4.1 } });
      fixture.detectChanges();

      expect(component.weather()?.location).toEqual({ latitude: 53.3, longitude: 6.3 });
      expect(component.loading()).toBe(false);
    });

    it('ignores a failure from a request that was replaced', () => {
      const responses: Subject<WeatherResponse>[] = [];
      weatherApi.respond = () => {
        const response = new Subject<WeatherResponse>();
        responses.push(response);
        return response;
      };

      selectMapLocation(51.1, 4.1);
      selectMapLocation(52.2, 5.2);

      responses[0].error(new Error('Request failed'));
      responses[1].next(weatherResponse);
      fixture.detectChanges();

      expect(component.errorMessage()).toBe('');
      expect(component.weather()).toEqual(weatherResponse);
    });

    it('does not send the same request twice while it is loading', () => {
      const response = new Subject<WeatherResponse>();
      weatherApi.respond = () => response;
      weatherApi.calls = [];

      getButtonByText('Get weather').click();
      getButtonByText('Loading...').click();
      component.loadWeather();

      expect(weatherApi.calls).toEqual([BROWSER_CALL]);

      response.next(weatherResponse);
      weatherApi.respond = () => of(weatherResponse);

      // Once the answer is in, the same location can be refreshed.
      component.loadWeather();
      expect(weatherApi.calls.length).toBe(2);
    });

    it('loads weather when the map is clicked', async () => {
      weatherApi.calls = [];

      selectMapLocation(51.12346, 4.98766);
      // ngModel writes the new values to the inputs asynchronously.
      await fixture.whenStable();

      expect(component.latitude()).toBe(51.1235);
      expect(component.longitude()).toBe(4.9877);
      expect(weatherApi.calls).toEqual([
        { latitude: 51.1235, longitude: 4.9877, provider: undefined },
      ]);
      expect(getMarker().getLatLng()).toEqual(new LatLng(51.1235, 4.9877));

      const inputs = getPage().querySelectorAll<HTMLInputElement>('input[type="number"]');
      expect(Array.from(inputs, (input) => input.value)).toEqual(['51.1235', '4.9877']);
    });

    it('renders the location map', () => {
      const page = getPage();

      expect(page.textContent).toContain('Map');
      expect(page.textContent).toContain('Click on the map to select a location.');
      expect(page.querySelector('#weather-map')).not.toBeNull();
    });

    it('uses the OpenStreetMap tile server with linked attribution', () => {
      const tileLayers: TileLayer[] = [];

      getMap().eachLayer((layer) => {
        if (layer instanceof TileLayer) {
          tileLayers.push(layer);
        }
      });

      expect(tileLayers.length).toBe(1);
      expect((tileLayers[0] as unknown as { _url: string })._url).toBe(
        'https://tile.openstreetmap.org/{z}/{x}/{y}.png',
      );

      const attributionLink = getPage().querySelector<HTMLAnchorElement>(
        '.leaflet-control-attribution a[href="https://www.openstreetmap.org/copyright"]',
      );

      expect(attributionLink?.textContent).toBe('OpenStreetMap');
    });

    it('uses the built-in marker SVG without external image assets', () => {
      const page = getPage();
      const markerIcon = page.querySelector<HTMLImageElement>('.leaflet-marker-icon');
      expect(markerIcon?.getAttribute('src')).toMatch(/^data:image\/svg\+xml,/);
      expect(markerIcon?.getAttribute('alt')).toBe('Marker');
      expect(page.querySelector('.leaflet-marker-shadow')).toBeNull();
    });

    it('keeps the current location when a later location request fails', () => {
      selectMapLocation(51.5, 4.5);
      weatherApi.calls = [];
      geolocation.mode = 'unavailable';

      getButtonByText('Use current location').click();
      fixture.detectChanges();

      expect(weatherApi.calls).toEqual([]);
      expect(component.latitude()).toBe(51.5);
      expect(getStatus()).toBe(
        'Your location is not available. You can enter coordinates or click on the map.',
      );
    });

    it('removes the map when the page is destroyed', () => {
      const map = getMap();
      const removeSpy = vi.spyOn(map, 'remove');

      fixture.destroy();

      expect(removeSpy).toHaveBeenCalledOnce();
    });
  });

  describe('when the location is not available', () => {
    it.each([
      ['deny', 'Location access was not allowed.'],
      ['unavailable', 'Your location is not available.'],
      ['timeout', 'Finding your location took too long.'],
    ] as const)('loads the default location when geolocation answers %s', (mode, reason) => {
      installGeolocation(mode);
      createPage();

      expect(weatherApi.calls).toEqual([DEFAULT_CALL]);
      expect(component.weather()).toEqual(weatherResponse);
      expect(component.locating()).toBe(false);
      expect(component.errorMessage()).toBe('');
      expect(getStatus()).toBe(`${reason} Showing weather for the default location.`);
    });

    it('loads the default location when geolocation is not supported', () => {
      Object.defineProperty(navigator, 'geolocation', { configurable: true, value: undefined });
      createPage();

      expect(weatherApi.calls).toEqual([DEFAULT_CALL]);
      expect(component.locating()).toBe(false);
      expect(getStatus()).toBe(
        'Geolocation is not supported by this browser. Showing weather for the default location.',
      );
    });

    it('loads the default location when the browser never answers', () => {
      vi.useFakeTimers({ toFake: ['setTimeout', 'clearTimeout'] });
      installGeolocation('pending');
      createPage();

      expect(component.locating()).toBe(true);
      expect(getStatus()).toBe('Finding your location...');
      expect(weatherApi.calls).toEqual([]);

      vi.advanceTimersByTime(LOCATION_TIMEOUT_MS);
      fixture.detectChanges();

      expect(component.locating()).toBe(false);
      expect(weatherApi.calls).toEqual([DEFAULT_CALL]);
      expect(getStatus()).toBe(
        'Finding your location took too long. Showing weather for the default location.',
      );

      // A position that arrives after the fallback does not move the location again.
      answerPendingLocation();
      expect(weatherApi.calls).toEqual([DEFAULT_CALL]);
      expect(component.latitude()).toBe(DEFAULT_CALL.latitude);
    });

    it('does not ask for location access again after it was denied', () => {
      installGeolocation('deny');
      createPage();
      weatherApi.calls = [];

      getButtonByText('Use current location').click();
      fixture.detectChanges();

      expect(geolocation.requests).toBe(1);
      expect(weatherApi.calls).toEqual([]);
      expect(getStatus()).toBe(
        'Location access was not allowed. Allow it in your browser settings to use your current location.',
      );
    });

    it('asks for the location only once while a request is open', () => {
      installGeolocation('pending');
      createPage();

      getButtonByText('Locating...').click();
      component.useCurrentLocation();

      expect(geolocation.requests).toBe(1);
    });
  });

  describe('while the location is being found', () => {
    beforeEach(() => {
      installGeolocation('pending');
      createPage();
    });

    it('keeps coordinates the user submits', () => {
      setInputValue('latitude', '40.7128');
      setInputValue('longitude', '-74.006');
      getButtonByText('Get weather').click();

      answerPendingLocation();
      fixture.detectChanges();

      expect(component.locating()).toBe(false);
      expect(component.latitude()).toBe(40.7128);
      expect(weatherApi.calls).toEqual([
        { latitude: 40.7128, longitude: -74.006, provider: undefined },
      ]);
    });

    it('keeps a location picked on the map', () => {
      selectMapLocation(51.2, 4.4);

      answerPendingLocation();
      fixture.detectChanges();

      expect(component.latitude()).toBe(51.2);
      expect(weatherApi.calls).toEqual([{ latitude: 51.2, longitude: 4.4, provider: undefined }]);
    });

    it('uses the browser location when it arrives first', () => {
      answerPendingLocation();
      fixture.detectChanges();

      expect(component.locating()).toBe(false);
      expect(weatherApi.calls).toEqual([BROWSER_CALL]);
    });
  });

  function createPage(): void {
    fixture = TestBed.createComponent(WeatherPage);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  function getPage(): HTMLElement {
    return fixture.nativeElement as HTMLElement;
  }

  function getStatus(): string {
    return getPage().querySelector('[role="status"]')?.textContent?.trim() ?? '';
  }

  function getMap(): LeafletMap {
    return (component as unknown as { map: LeafletMap }).map;
  }

  function getMarker(): Marker {
    return (component as unknown as { marker: Marker }).marker;
  }

  function selectMapLocation(latitude: number, longitude: number): void {
    getMap().fire('click', { latlng: new LatLng(latitude, longitude) });
  }

  function setInputValue(name: string, value: string): void {
    const input = getPage().querySelector<HTMLInputElement>(`input[name="${name}"]`);

    if (!input) {
      throw new Error(`Input not found: ${name}`);
    }

    input.value = value;
    input.dispatchEvent(new Event('input'));
  }

  function getButtonByText(text: string): HTMLButtonElement {
    fixture.detectChanges();

    const buttons = Array.from(getPage().querySelectorAll('button'));
    const button = buttons.find((candidate) => candidate.textContent?.trim() === text);

    if (!button) {
      throw new Error(`Button not found: ${text}`);
    }

    return button;
  }

  function answerPendingLocation(): void {
    geolocation.pendingSuccess?.(createPosition());
  }

  function createPosition(): GeolocationPosition {
    return {
      coords: {
        ...geolocation.position,
        accuracy: 1,
        altitude: null,
        altitudeAccuracy: null,
        heading: null,
        speed: null,
      },
      timestamp: Date.now(),
    } as GeolocationPosition;
  }

  function installGeolocation(mode: GeolocationMode): void {
    const errorCodes = { deny: 1, unavailable: 2, timeout: 3 } as const;

    geolocation = { requests: 0, mode, position: BROWSER_POSITION };

    Object.defineProperty(navigator, 'geolocation', {
      configurable: true,
      value: {
        getCurrentPosition: (success: PositionCallback, error: PositionErrorCallback) => {
          geolocation.requests++;

          if (geolocation.mode === 'allow') {
            success(createPosition());
          } else if (geolocation.mode === 'pending') {
            geolocation.pendingSuccess = success;
          } else {
            error({
              code: errorCodes[geolocation.mode],
              message: geolocation.mode,
              PERMISSION_DENIED: 1,
              POSITION_UNAVAILABLE: 2,
              TIMEOUT: 3,
            } as GeolocationPositionError);
          }
        },
      },
    });
  }
});
