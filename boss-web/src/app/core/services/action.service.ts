import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, map, tap } from 'rxjs';
import { APP_CONFIG } from '../config/app-config';
import { ApiResponse } from '../../shared/models/api-response.model';
import { ActionItem, CompleteActionRequest, ProposeActionRequest } from '../../shared/models/action.model';

/**
 * Holds the tenant's actions as shared signal state so the recommendation panel
 * (which proposes actions), the action panel (which approves/rejects/completes them)
 * and the outcome panel (which reads completed ones) all stay in sync without each
 * component re-fetching or passing data through the parent dashboard component.
 */
@Injectable({ providedIn: 'root' })
export class ActionService {
  private readonly http = inject(HttpClient);
  private readonly config = inject(APP_CONFIG);

  private readonly _actions = signal<ActionItem[]>([]);
  readonly actions = this._actions.asReadonly();

  refresh(): Observable<ActionItem[]> {
    return this.http
      .get<ApiResponse<ActionItem[]>>(`${this.config.apiBaseUrl}/actions`)
      .pipe(
        map((response) => response.data),
        tap((actions) => this._actions.set(actions)),
      );
  }

  propose(request: ProposeActionRequest): Observable<ActionItem> {
    return this.http
      .post<ApiResponse<ActionItem>>(`${this.config.apiBaseUrl}/actions`, request)
      .pipe(
        map((response) => response.data),
        tap((created) => this._actions.update((list) => [created, ...list])),
      );
  }

  approve(id: string): Observable<ActionItem> {
    return this.post(id, 'approve');
  }

  reject(id: string): Observable<ActionItem> {
    return this.post(id, 'reject');
  }

  start(id: string): Observable<ActionItem> {
    return this.post(id, 'start');
  }

  cancel(id: string): Observable<ActionItem> {
    return this.post(id, 'cancel');
  }

  complete(id: string, request: CompleteActionRequest): Observable<ActionItem> {
    return this.http
      .post<ApiResponse<ActionItem>>(`${this.config.apiBaseUrl}/actions/${id}/complete`, request)
      .pipe(
        map((response) => response.data),
        tap((updated) => this.replace(updated)),
      );
  }

  private post(id: string, verb: string): Observable<ActionItem> {
    return this.http
      .post<ApiResponse<ActionItem>>(`${this.config.apiBaseUrl}/actions/${id}/${verb}`, {})
      .pipe(
        map((response) => response.data),
        tap((updated) => this.replace(updated)),
      );
  }

  private replace(updated: ActionItem): void {
    this._actions.update((list) => list.map((a) => (a.id === updated.id ? updated : a)));
  }
}
