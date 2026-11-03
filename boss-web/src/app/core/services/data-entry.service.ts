import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, map, shareReplay, tap } from 'rxjs';
import { APP_CONFIG } from '../config/app-config';
import { ApiResponse } from '../../shared/models/api-response.model';
import { SubcategoryCode } from '../../shared/models/access.model';
import { BulkUploadResult, DataRecord, SubcategorySchema } from '../../shared/models/data-entry.model';
import { saveBlob } from '../../shared/utils/file-download';

/**
 * Which subcategories have a real data-entry form (and what fields each one has) is
 * fetched once from the backend - the single source of truth - and cached for the
 * session, mirroring CategoryService's taxonomy caching.
 */
@Injectable({ providedIn: 'root' })
export class DataEntryService {
  private readonly http = inject(HttpClient);
  private readonly config = inject(APP_CONFIG);

  private readonly _schemas = signal<SubcategorySchema[]>([]);
  readonly schemas = this._schemas.asReadonly();

  readonly schemasBySubcategory = computed<Map<SubcategoryCode, SubcategorySchema>>(
    () => new Map(this._schemas().map((schema) => [schema.subcategory, schema])),
  );

  private cached$: Observable<SubcategorySchema[]> | null = null;

  loadSchemas(): Observable<SubcategorySchema[]> {
    if (!this.cached$) {
      this.cached$ = this.http
        .get<ApiResponse<SubcategorySchema[]>>(`${this.config.apiBaseUrl}/data-entry/schemas`)
        .pipe(
          map((response) => response.data),
          tap((schemas) => this._schemas.set(schemas)),
          shareReplay(1),
        );
    }
    return this.cached$;
  }

  listRecords(subcategory: SubcategoryCode): Observable<DataRecord[]> {
    return this.http
      .get<ApiResponse<DataRecord[]>>(`${this.config.apiBaseUrl}/data-entry/${subcategory}/records`)
      .pipe(map((response) => response.data));
  }

  createRecord(subcategory: SubcategoryCode, values: Record<string, string>): Observable<DataRecord> {
    return this.http
      .post<ApiResponse<DataRecord>>(`${this.config.apiBaseUrl}/data-entry/${subcategory}/records`, { values })
      .pipe(map((response) => response.data));
  }

  updateRecord(subcategory: SubcategoryCode, id: string, values: Record<string, string>): Observable<DataRecord> {
    return this.http
      .patch<ApiResponse<DataRecord>>(`${this.config.apiBaseUrl}/data-entry/${subcategory}/records/${id}`, { values })
      .pipe(map((response) => response.data));
  }

  voidRecord(subcategory: SubcategoryCode, id: string, reason: string): Observable<void> {
    return this.http
      .post<ApiResponse<void>>(`${this.config.apiBaseUrl}/data-entry/${subcategory}/records/${id}/void`, { reason })
      .pipe(map(() => undefined));
  }

  /** Downloads the single-sheet Excel template for one subcategory and saves it via the browser. */
  downloadTemplate(subcategory: SubcategoryCode): Observable<void> {
    return this.http
      .get(`${this.config.apiBaseUrl}/data-entry/${subcategory}/template`, { responseType: 'blob' })
      .pipe(
        tap((blob) => saveBlob(blob, `${subcategory.toLowerCase()}-template.xlsx`)),
        map(() => void 0),
      );
  }

  bulkUpload(subcategory: SubcategoryCode, file: File): Observable<BulkUploadResult> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http
      .post<ApiResponse<BulkUploadResult>>(`${this.config.apiBaseUrl}/data-entry/${subcategory}/bulk-upload`, formData)
      .pipe(map((response) => response.data));
  }
}
