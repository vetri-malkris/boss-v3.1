import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { APP_CONFIG } from '../config/app-config';
import { ApiResponse } from '../../shared/models/api-response.model';
import { ChatResponse } from '../../shared/models/ai.model';

@Injectable({ providedIn: 'root' })
export class AiService {
  private readonly http = inject(HttpClient);
  private readonly config = inject(APP_CONFIG);

  ask(message: string): Observable<ChatResponse> {
    return this.http
      .post<ApiResponse<ChatResponse>>(`${this.config.apiBaseUrl}/ai/chat`, { message })
      .pipe(map((response) => response.data));
  }
}
