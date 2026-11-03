import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { APP_CONFIG } from '../config/app-config';
import { ApiResponse } from '../../shared/models/api-response.model';
import { Recommendation } from '../../shared/models/recommendation.model';

@Injectable({ providedIn: 'root' })
export class RecommendationService {
  private readonly http = inject(HttpClient);
  private readonly config = inject(APP_CONFIG);

  list(): Observable<Recommendation[]> {
    return this.http
      .get<ApiResponse<Recommendation[]>>(`${this.config.apiBaseUrl}/recommendations`)
      .pipe(map((response) => response.data));
  }

  generate(): Observable<Recommendation[]> {
    return this.http
      .post<ApiResponse<Recommendation[]>>(`${this.config.apiBaseUrl}/recommendations/generate`, {})
      .pipe(map((response) => response.data));
  }
}
