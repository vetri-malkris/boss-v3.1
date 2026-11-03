import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map, tap } from 'rxjs';
import { APP_CONFIG } from '../config/app-config';
import { ApiResponse } from '../../shared/models/api-response.model';
import { CategoryCode } from '../../shared/models/access.model';
import { CategoryUploadResult } from '../../shared/models/category-upload.model';
import { saveBlob } from '../../shared/utils/file-download';

@Injectable({ providedIn: 'root' })
export class CategoryUploadService {
  private readonly http = inject(HttpClient);
  private readonly config = inject(APP_CONFIG);

  /** Downloads the multi-sheet Excel template for a category and saves it via the browser. */
  downloadTemplate(category: CategoryCode): Observable<void> {
    return this.http
      .get(`${this.config.apiBaseUrl}/data-entry/category/${category}/template`, { responseType: 'blob' })
      .pipe(
        tap((blob) => saveBlob(blob, `${category.toLowerCase()}-template.xlsx`)),
        map(() => void 0),
      );
  }

  bulkUpload(category: CategoryCode, file: File): Observable<CategoryUploadResult> {
    const formData = new FormData();
    formData.append('file', file);

    return this.http
      .post<ApiResponse<CategoryUploadResult>>(`${this.config.apiBaseUrl}/data-entry/category/${category}/bulk-upload`, formData)
      .pipe(map((response) => response.data));
  }
}
