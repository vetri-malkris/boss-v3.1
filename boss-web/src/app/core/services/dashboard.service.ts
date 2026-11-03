import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { APP_CONFIG } from '../config/app-config';
import { ApiResponse } from '../../shared/models/api-response.model';
import { DashboardState } from '../../shared/models/dashboard.model';

@Injectable({ providedIn: 'root' })
export class DashboardService {
  private readonly http = inject(HttpClient);
  private readonly config = inject(APP_CONFIG);

  load(): Observable<DashboardState> {
    return this.http
      .get<ApiResponse<DashboardState>>(`${this.config.apiBaseUrl}/dashboard`)
      .pipe(map((response) => response.data));
  }
}
