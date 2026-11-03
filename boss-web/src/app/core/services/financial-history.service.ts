import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { APP_CONFIG } from '../config/app-config';
import { ApiResponse } from '../../shared/models/api-response.model';
import { SnapshotRevision } from '../../shared/models/snapshot-revision.model';

@Injectable({ providedIn: 'root' })
export class FinancialHistoryService {
  private readonly http = inject(HttpClient);
  private readonly config = inject(APP_CONFIG);

  revisions(): Observable<SnapshotRevision[]> {
    return this.http
      .get<ApiResponse<SnapshotRevision[]>>(`${this.config.apiBaseUrl}/financial/revisions`)
      .pipe(map((response) => response.data));
  }
}
