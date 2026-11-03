import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { APP_CONFIG } from '../config/app-config';
import { ApiResponse } from '../../shared/models/api-response.model';
import { CreateUserRequest, UserSummary } from '../../shared/models/user.model';
import { SubcategoryCode } from '../../shared/models/access.model';

@Injectable({ providedIn: 'root' })
export class UserManagementService {
  private readonly http = inject(HttpClient);
  private readonly config = inject(APP_CONFIG);

  list(): Observable<UserSummary[]> {
    return this.http
      .get<ApiResponse<UserSummary[]>>(`${this.config.apiBaseUrl}/users`)
      .pipe(map((response) => response.data));
  }

  create(request: CreateUserRequest): Observable<UserSummary> {
    return this.http
      .post<ApiResponse<UserSummary>>(`${this.config.apiBaseUrl}/users`, request)
      .pipe(map((response) => response.data));
  }

  updateAccess(userId: string, enabledSubcategories: SubcategoryCode[]): Observable<UserSummary> {
    return this.http
      .patch<ApiResponse<UserSummary>>(`${this.config.apiBaseUrl}/users/${userId}/access`, { enabledSubcategories })
      .pipe(map((response) => response.data));
  }

  deactivate(userId: string): Observable<void> {
    return this.http
      .post<ApiResponse<void>>(`${this.config.apiBaseUrl}/users/${userId}/deactivate`, {})
      .pipe(map(() => undefined));
  }
}
