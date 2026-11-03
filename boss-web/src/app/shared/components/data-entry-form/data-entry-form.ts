import { Component, computed, effect, inject, input, output, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatSelectModule } from '@angular/material/select';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { DataEntryService } from '../../../core/services/data-entry.service';
import { ApiErrorResponse } from '../../models/api-response.model';
import { DataRecord, FieldDefinition, SubcategorySchema } from '../../models/data-entry.model';

/**
 * One data-entry form component reused by every subcategory that has a schema
 * (section: subcategory data entry), and reused again for editing an existing
 * record (pass {@link editingRecord} to switch modes) - the same field rendering
 * and validation apply to both, so a correction is held to the same rules as the
 * original entry. The FIELDS are real, distinct, per-subcategory domain metadata
 * fetched from the backend - only the rendering is shared, so this never becomes a
 * generic label/value form.
 */
@Component({
  selector: 'app-data-entry-form',
  imports: [FormsModule, MatButtonModule, MatSelectModule, MatProgressSpinnerModule, MatDatepickerModule],
  templateUrl: './data-entry-form.html',
  styleUrl: './data-entry-form.css',
})
export class DataEntryForm {
  private readonly dataEntryService = inject(DataEntryService);

  readonly schema = input.required<SubcategorySchema>();
  readonly editingRecord = input<DataRecord | null>(null);
  readonly recordCreated = output<void>();
  readonly recordUpdated = output<void>();

  readonly values = signal<Record<string, string>>({});
  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly justSaved = signal(false);

  /** `dateValueOf` is called straight from the template on every change-detection
   * pass. Without this cache it would build a brand new `Date` instance each time
   * even when the underlying ISO string hasn't changed, and a value that never
   * settles on a stable reference is exactly what makes MatDatepicker's own
   * internal selection tracking (CVA writeValue vs. the user's in-progress pick)
   * fight itself while the calendar overlay is open. */
  private readonly dateCache = new Map<string, { raw: string; date: Date | null }>();

  readonly isEditing = computed(() => this.editingRecord() !== null);

  readonly canSubmit = computed(() => {
    const values = this.values();
    return this.schema().fields.every((field) => !field.required || (values[field.name] ?? '').trim().length > 0);
  });

  constructor() {
    effect(() => {
      const record = this.editingRecord();
      if (record) {
        const initial: Record<string, string> = {};
        for (const [key, value] of Object.entries(record.fields)) {
          initial[key] = String(value);
        }
        this.values.set(initial);
      }
    });
  }

  setValue(field: FieldDefinition, raw: string | number): void {
    // Angular's NumberValueAccessor hands back an actual `number` for
    // type="number" inputs (not a string like every other input type) -
    // normalize here so `values` is always Record<string, string>.
    this.values.update((current) => ({ ...current, [field.name]: String(raw) }));
  }

  valueOf(field: FieldDefinition): string {
    return this.values()[field.name] ?? '';
  }

  /** DATE fields are stored in `values` as the same ISO (yyyy-MM-dd) string the
   * backend expects - only the datepicker's own binding needs a JS Date, converted
   * at this boundary so nothing else in the component has to know about it.
   * Parsed with an explicit local-midnight time (not a bare date string) so the
   * date never shifts by a day in timezones behind UTC. */
  dateValueOf(field: FieldDefinition): Date | null {
    const raw = this.values()[field.name] ?? '';
    const cached = this.dateCache.get(field.name);
    if (cached && cached.raw === raw) {
      return cached.date;
    }
    const parsed = raw ? new Date(`${raw}T00:00:00`) : null;
    const date = parsed && !isNaN(parsed.getTime()) ? parsed : null;
    this.dateCache.set(field.name, { raw, date });
    return date;
  }

  setDateValue(field: FieldDefinition, date: Date | null): void {
    if (!date) {
      this.values.update((current) => {
        const next = { ...current };
        delete next[field.name];
        return next;
      });
      return;
    }
    const iso = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
    this.values.update((current) => ({ ...current, [field.name]: iso }));
  }

  submit(): void {
    if (!this.canSubmit()) {
      return;
    }
    this.submitting.set(true);
    this.errorMessage.set(null);
    this.justSaved.set(false);

    const editing = this.editingRecord();
    const request$ = editing
      ? this.dataEntryService.updateRecord(this.schema().subcategory, editing.id, this.values())
      : this.dataEntryService.createRecord(this.schema().subcategory, this.values());

    request$.subscribe({
      next: () => {
        this.submitting.set(false);
        this.justSaved.set(true);
        if (editing) {
          this.recordUpdated.emit();
        } else {
          this.values.set({});
          this.recordCreated.emit();
          setTimeout(() => this.justSaved.set(false), 2000);
        }
      },
      error: (error: HttpErrorResponse) => {
        this.submitting.set(false);
        const body = error.error as ApiErrorResponse | undefined;
        this.errorMessage.set(body?.message ?? 'Could not save this record. Please try again.');
      },
    });
  }
}
