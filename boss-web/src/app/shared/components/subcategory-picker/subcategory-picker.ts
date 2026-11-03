import { Component, computed, effect, input, output, signal } from '@angular/core';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { CategoryCode, CategoryTaxonomy, SubcategoryCode } from '../../models/access.model';

/**
 * The category/subcategory access picker used both when creating a user and when
 * editing an existing one's access. A category's toggle is independent UI state, not
 * derived: switching it on reveals its subcategories and requires picking at least one
 * before the selection is valid; switching it off clears them. This is where "category
 * enabled needs >= 1 subcategory, or you can't save" is enforced - the parent dialog
 * reads {@link valid} to gate its Save button.
 */
@Component({
  selector: 'app-subcategory-picker',
  imports: [MatCheckboxModule, MatSlideToggleModule],
  templateUrl: './subcategory-picker.html',
  styleUrl: './subcategory-picker.css',
})
export class SubcategoryPicker {
  readonly taxonomy = input.required<CategoryTaxonomy[]>();
  readonly initialSelection = input<SubcategoryCode[]>([]);
  readonly selectionChange = output<SubcategoryCode[]>();
  readonly validChange = output<boolean>();

  readonly categoryEnabled = signal<Record<string, boolean>>({});
  readonly selected = signal<Set<SubcategoryCode>>(new Set());

  readonly selection = computed(() => Array.from(this.selected()));
  readonly valid = computed(() => {
    const enabled = this.categoryEnabled();
    const chosen = this.selected();
    return this.taxonomy().every((category) => {
      if (!enabled[category.code]) {
        return true;
      }
      return category.subcategories.some((sub) => chosen.has(sub.code));
    });
  });

  constructor() {
    effect(() => {
      const taxonomy = this.taxonomy();
      const initial = new Set(this.initialSelection());
      if (taxonomy.length === 0) {
        return;
      }
      const enabled: Record<string, boolean> = {};
      for (const category of taxonomy) {
        enabled[category.code] = category.subcategories.some((sub) => initial.has(sub.code));
      }
      this.categoryEnabled.set(enabled);
      this.selected.set(initial);
    });

    effect(() => this.selectionChange.emit(this.selection()));
    effect(() => this.validChange.emit(this.valid()));
  }

  toggleCategory(category: CategoryTaxonomy, enabled: boolean): void {
    this.categoryEnabled.update((state) => ({ ...state, [category.code]: enabled }));
    if (!enabled) {
      this.selected.update((current) => {
        const next = new Set(current);
        for (const sub of category.subcategories) {
          next.delete(sub.code);
        }
        return next;
      });
    }
  }

  isCategoryEnabled(code: CategoryCode): boolean {
    return this.categoryEnabled()[code] ?? false;
  }

  isCategoryMissingASubcategory(category: CategoryTaxonomy): boolean {
    if (!this.isCategoryEnabled(category.code)) {
      return false;
    }
    return !category.subcategories.some((sub) => this.selected().has(sub.code));
  }

  isSelected(code: SubcategoryCode): boolean {
    return this.selected().has(code);
  }

  toggleSubcategory(code: SubcategoryCode, checked: boolean): void {
    this.selected.update((current) => {
      const next = new Set(current);
      if (checked) {
        next.add(code);
      } else {
        next.delete(code);
      }
      return next;
    });
  }
}
