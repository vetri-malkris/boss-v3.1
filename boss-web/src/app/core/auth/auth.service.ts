import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, map, tap } from 'rxjs';
import { APP_CONFIG } from '../config/app-config';
import { ApiResponse } from '../../shared/models/api-response.model';
import { LoginRequest, LoginResponse, RegisterTenantRequest } from '../../shared/models/auth.model';
import { RoleName } from '../../shared/models/role.model';
import { UserSummary } from '../../shared/models/user.model';

const TOKEN_STORAGE_KEY = 'boss.accessToken';
const USER_STORAGE_KEY = 'boss.currentUser';

/**
 * Owns the client-side session: the access token, the current user, and role checks
 * used by guards/components. The token never touches an AI provider or any third
 * party - it is only ever attached to requests to this app's own backend
 * (see authInterceptor) (section 27).
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly config = inject(APP_CONFIG);

  private readonly tokenSignal = signal<string | null>(this.readStoredToken());
  private readonly userSignal = signal<UserSummary | null>(this.readStoredUser());

  readonly currentUser = this.userSignal.asReadonly();
  readonly isAuthenticated = computed(() => this.tokenSignal() !== null);

  login(request: LoginRequest): Observable<LoginResponse> {
    return this.http
      .post<ApiResponse<LoginResponse>>(`${this.config.apiBaseUrl}/auth/login`, request)
      .pipe(
        map((response) => response.data),
        tap((loginResponse) => this.applySession(loginResponse)),
      );
  }

  /** SUPER_ADMIN onboarding a client: creates the tenant and its first CEO user.
   * Deliberately does NOT switch the session to the new CEO - the caller stays
   * logged in as themselves (there is no public self-signup anymore). */
  onboardTenant(request: RegisterTenantRequest): Observable<LoginResponse> {
    return this.http
      .post<ApiResponse<LoginResponse>>(`${this.config.apiBaseUrl}/auth/register`, request)
      .pipe(map((response) => response.data));
  }

  logout(): void {
    this.tokenSignal.set(null);
    this.userSignal.set(null);
    localStorage.removeItem(TOKEN_STORAGE_KEY);
    localStorage.removeItem(USER_STORAGE_KEY);
  }

  getToken(): string | null {
    return this.tokenSignal();
  }

  hasRole(role: RoleName): boolean {
    return this.userSignal()?.roles.includes(role) ?? false;
  }

  hasAnyRole(roles: RoleName[]): boolean {
    return roles.some((role) => this.hasRole(role));
  }

  private applySession(response: LoginResponse): void {
    this.tokenSignal.set(response.accessToken);
    this.userSignal.set(response.user);
    localStorage.setItem(TOKEN_STORAGE_KEY, response.accessToken);
    localStorage.setItem(USER_STORAGE_KEY, JSON.stringify(response.user));
  }

  private readStoredToken(): string | null {
    return localStorage.getItem(TOKEN_STORAGE_KEY);
  }

  private readStoredUser(): UserSummary | null {
    const raw = localStorage.getItem(USER_STORAGE_KEY);
    return raw ? (JSON.parse(raw) as UserSummary) : null;
  }
}
