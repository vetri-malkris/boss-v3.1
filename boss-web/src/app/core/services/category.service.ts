import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, map, shareReplay, tap } from 'rxjs';
import { APP_CONFIG } from '../config/app-config';
import { ApiResponse } from '../../shared/models/api-response.model';
import { CategoryCode, CategoryTaxonomy, SubcategoryCode } from '../../shared/models/access.model';
import { AuthService } from '../auth/auth.service';
import { CategoryViewService } from './category-view.service';

/**
 * The fixed sidebar taxonomy, fetched once from the backend (the single source of
 * truth for labels/structure) and cached for the session - it never changes at runtime.
 * Also derives the slice of it the *current* user is actually allowed to see (dynamic
 * per-user menu access), so the sidebar and the dashboard's "Overview" both filter the
 * same way instead of duplicating the rule.
 */
@Injectable({ providedIn: 'root' })
export class CategoryService {
  private readonly http = inject(HttpClient);
  private readonly config = inject(APP_CONFIG);
  private readonly authService = inject(AuthService);
  private readonly categoryViewService = inject(CategoryViewService);

  private readonly _taxonomy = signal<CategoryTaxonomy[]>([]);
  readonly taxonomy = this._taxonomy.asReadonly();

  readonly visibleCategories = computed<CategoryTaxonomy[]>(() => {
    const taxonomy = this._taxonomy();
    const user = this.authService.currentUser();
    if (!user) {
      return [];
    }
    if (user.roles.includes('TENANT_ADMIN')) {
      return taxonomy;
    }
    const enabled = new Set(user.enabledSubcategories);
    return taxonomy
      .map((category) => ({
        ...category,
        subcategories: category.subcategories.filter((sub) => enabled.has(sub.code)),
      }))
      .filter((category) => category.subcategories.length > 0);
  });

  readonly visibleSubcategoryCodes = computed<Set<SubcategoryCode>>(() => {
    const codes = new Set<SubcategoryCode>();
    for (const category of this.visibleCategories()) {
      for (const sub of category.subcategories) {
        codes.add(sub.code);
      }
    }
    return codes;
  });

  /**
   * Which category the currently-selected subcategory belongs to - null on
   * Overview. Derived purely from `CategoryViewService.selected()` (no new state:
   * the sidebar always selects a subcategory as soon as a category is expanded),
   * so this is what drives whether the upload button is enabled and which
   * category's template it should offer.
   */
  readonly activeCategory = computed<CategoryCode | null>(() => {
    const selected = this.categoryViewService.selected();
    if (!selected) {
      return null;
    }
    const match = this._taxonomy().find((category) =>
      category.subcategories.some((sub) => sub.code === selected),
    );
    return match?.code ?? null;
  });

  private cached$: Observable<CategoryTaxonomy[]> | null = null;

  load(): Observable<CategoryTaxonomy[]> {
    if (!this.cached$) {
      this.cached$ = this.http
        .get<ApiResponse<CategoryTaxonomy[]>>(`${this.config.apiBaseUrl}/categories`)
        .pipe(
          map((response) => response.data),
          tap((taxonomy) => this._taxonomy.set(taxonomy)),
          shareReplay(1),
        );
    }
    return this.cached$;
  }
}
