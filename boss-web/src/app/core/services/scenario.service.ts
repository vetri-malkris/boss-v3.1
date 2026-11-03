import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { APP_CONFIG } from '../config/app-config';
import { ApiResponse } from '../../shared/models/api-response.model';
import { ScenarioRequest, ScenarioResult } from '../../shared/models/scenario.model';

@Injectable({ providedIn: 'root' })
export class ScenarioService {
  private readonly http = inject(HttpClient);
  private readonly config = inject(APP_CONFIG);

  simulate(request: ScenarioRequest): Observable<ScenarioResult> {
    return this.http
      .post<ApiResponse<ScenarioResult>>(`${this.config.apiBaseUrl}/scenarios/simulate`, request)
      .pipe(map((response) => response.data));
  }
}
