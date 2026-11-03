import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { APP_CONFIG } from '../config/app-config';
import { ApiResponse } from '../../shared/models/api-response.model';
import { CreateUserRequest, UserSummary } from '../../shared/models/user.model';
import { SubcategoryCode } from '../../shared/models/access.model';

/** SUPER_ADMIN only - the same operations as UserManagementService, but scoped to an
 * explicit tenantId instead of the caller's own tenant (see TenantUserController). */
@Injectable({ providedIn: 'root' })
export class SuperAdminUserService {
  private readonly http = inject(HttpClient);
  private readonly config = inject(APP_CONFIG);

  list(tenantId: string): Observable<UserSummary[]> {
    return this.http
      .get<ApiResponse<UserSummary[]>>(`${this.config.apiBaseUrl}/tenants/${tenantId}/users`)
      .pipe(map((response) => response.data));
  }

  create(tenantId: string, request: CreateUserRequest): Observable<UserSummary> {
    return this.http
      .post<ApiResponse<UserSummary>>(`${this.config.apiBaseUrl}/tenants/${tenantId}/users`, request)
      .pipe(map((response) => response.data));
  }

  updateAccess(tenantId: string, userId: string, enabledSubcategories: SubcategoryCode[]): Observable<UserSummary> {
    return this.http
      .patch<ApiResponse<UserSummary>>(`${this.config.apiBaseUrl}/tenants/${tenantId}/users/${userId}/access`, { enabledSubcategories })
      .pipe(map((response) => response.data));
  }

  deactivate(tenantId: string, userId: string): Observable<void> {
    return this.http
      .post<ApiResponse<void>>(`${this.config.apiBaseUrl}/tenants/${tenantId}/users/${userId}/deactivate`, {})
      .pipe(map(() => undefined));
  }
}
