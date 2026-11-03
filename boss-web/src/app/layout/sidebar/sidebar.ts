import { Component, inject, signal } from '@angular/core';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { CategoryService } from '../../core/services/category.service';
import { CategoryViewService } from '../../core/services/category-view.service';
import { CategoryCode, CategoryTaxonomy, SubcategoryCode } from '../../shared/models/access.model';

const CATEGORY_ICONS: Record<CategoryCode, string> = {
  HUMAN_RESOURCE: 'groups',
  FINANCE: 'payments',
  ACCOUNTING: 'receipt_long',
  AUDITING: 'fact_check',
  COMPLIANCE: 'gavel',
  RISK: 'warning',
};

/**
 * Dynamic per-user sidebar (section: menu access): a TENANT_ADMIN sees every
 * category; anyone else sees only categories that have at least one of their
 * enabledSubcategories, and only those specific subcategories within it - never a
 * category or item the current user wasn't granted. "Overview" (selected = null)
 * always sits above the taxonomy and shows every panel the user has access to at
 * once; picking a subcategory narrows the dashboard down to just that.
 *
 * Only one category is ever expanded at a time (openCategory) - they all start
 * shrunk, and opening one closes whichever was open. Clicking a category's icon
 * while the sidebar itself is collapsed to its icon rail expands the sidebar,
 * opens straight to that category, and pre-selects its first subcategory so the
 * dashboard has something to show immediately instead of an empty expanded panel.
 */
@Component({
  selector: 'app-sidebar',
  imports: [MatExpansionModule, MatIconModule, MatTooltipModule],
  templateUrl: './sidebar.html',
  styleUrl: './sidebar.css',
})
export class Sidebar {
  private readonly categoryService = inject(CategoryService);
  private readonly categoryViewService = inject(CategoryViewService);

  readonly categoryIcons = CATEGORY_ICONS;
  readonly selected = this.categoryViewService.selected;
  readonly visibleCategories = this.categoryService.visibleCategories;
  readonly collapsed = signal(false);
  readonly openCategory = signal<CategoryCode | null>(null);

  constructor() {
    this.categoryService.load().subscribe();
  }

  toggleCollapsed(): void {
    this.collapsed.update((value) => !value);
  }

  selectOverview(): void {
    this.categoryViewService.select(null);
    this.openCategory.set(null);
  }

  select(code: SubcategoryCode): void {
    this.categoryViewService.select(code);
  }

  onCategoryClosed(code: CategoryCode): void {
    this.openCategory.update((current) => (current === code ? null : current));
  }

  /** A collapsed-rail icon click: expand the sidebar straight into that category with its first subcategory selected. */
  expandTo(category: CategoryTaxonomy): void {
    this.collapsed.set(false);
    this.openCategory.set(category.code);
    const first = category.subcategories[0];
    if (first) {
      this.categoryViewService.select(first.code);
    }
  }
}
