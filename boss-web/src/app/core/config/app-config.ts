import { InjectionToken } from '@angular/core';
import { environment } from '../../../environments/environment';

export interface AppConfig {
  apiBaseUrl: string;
}

/**
 * DI token wrapping environment.ts so services depend on an injectable config
 * rather than importing the environment file directly - keeps them testable
 * and keeps the runtime API host swappable per section 28/33.
 */
export const APP_CONFIG = new InjectionToken<AppConfig>('APP_CONFIG', {
  factory: () => ({ apiBaseUrl: environment.apiBaseUrl }),
});
