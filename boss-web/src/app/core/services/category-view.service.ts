import { Injectable, signal } from '@angular/core';
import { SubcategoryCode } from '../../shared/models/access.model';

/**
 * Which sidebar subcategory is currently selected - null means "Overview" (every
 * panel the current user has access to, at once). The sidebar writes to this; the
 * dashboard's content area reads it to decide what to render. Kept as its own service
 * (rather than component state) so the sidebar and the main content - siblings in
 * DashboardLayout - can share it without threading it through inputs/outputs.
 */
@Injectable({ providedIn: 'root' })
export class CategoryViewService {
  private readonly _selected = signal<SubcategoryCode | null>(null);
  readonly selected = this._selected.asReadonly();

  select(code: SubcategoryCode | null): void {
    this._selected.set(code);
  }
}
