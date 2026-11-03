import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { APP_CONFIG } from '../config/app-config';
import { ApiResponse } from '../../shared/models/api-response.model';
import { Tenant } from '../../shared/models/tenant.model';

/** SUPER_ADMIN only - platform-level tenant listing (see TenantController). */
@Injectable({ providedIn: 'root' })
export class TenantService {
  private readonly http = inject(HttpClient);
  private readonly config = inject(APP_CONFIG);

  list(): Observable<Tenant[]> {
    return this.http
      .get<ApiResponse<Tenant[]>>(`${this.config.apiBaseUrl}/tenants`)
      .pipe(map((response) => response.data));
  }
}
