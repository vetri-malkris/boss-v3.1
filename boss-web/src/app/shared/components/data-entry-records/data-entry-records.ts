import { Component, computed, effect, inject, input, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { DataEntryService } from '../../../core/services/data-entry.service';
import { DataRecord, SubcategorySchema } from '../../models/data-entry.model';
import { EditRecordDialog } from '../edit-record-dialog/edit-record-dialog';
import { VoidRecordDialog } from '../void-record-dialog/void-record-dialog';

/**
 * A table of the records already entered for a subcategory - real persisted data,
 * most recent first. Every row can be corrected (Edit) or voided, so a mistaken
 * entry is never a dead end - "buried" or "no way to fix it" would make this not
 * actually usable (section: corrections must remain traceable).
 */
@Component({
  selector: 'app-data-entry-records',
  imports: [MatButtonModule, MatIconModule, MatTooltipModule],
  templateUrl: './data-entry-records.html',
  styleUrl: './data-entry-records.css',
})
export class DataEntryRecords {
  private readonly dataEntryService = inject(DataEntryService);
  private readonly dialog = inject(MatDialog);

  readonly schema = input.required<SubcategorySchema>();
  readonly refreshToken = input<number>(0);

  readonly records = signal<DataRecord[]>([]);
  readonly loading = signal(true);

  readonly columns = computed(() => this.schema().fields.map((field) => field.name));

  constructor() {
    effect(() => {
      this.refreshToken();
      this.refresh();
    });
  }

  refresh(): void {
    this.loading.set(true);
    this.dataEntryService.listRecords(this.schema().subcategory).subscribe({
      next: (records) => {
        this.records.set(records);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  cell(record: DataRecord, column: string): string {
    const value = record.fields[column];
    if (value === undefined || value === null) {
      return '';
    }
    const field = this.schema().fields.find((f) => f.name === column);
    return field?.type === 'DATE' ? this.isoToDisplay(String(value)) : String(value);
  }

  /** Stored dates are always ISO (yyyy-MM-dd, see SchemaValidator) - displayed as
   * DD-MM-YYYY to match the datepicker's input format. Falls back to the raw value
   * unchanged if it isn't in the expected shape, rather than showing something wrong. */
  private isoToDisplay(iso: string): string {
    const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(iso);
    return match ? `${match[3]}-${match[2]}-${match[1]}` : iso;
  }

  edit(record: DataRecord): void {
    this.dialog
      .open(EditRecordDialog, { width: '640px', data: { schema: this.schema(), record } })
      .afterClosed()
      .subscribe((saved) => {
        if (saved) {
          this.refresh();
        }
      });
  }

  voidRecord(record: DataRecord): void {
    this.dialog
      .open(VoidRecordDialog, { width: '480px', data: { subcategory: this.schema().subcategory, record } })
      .afterClosed()
      .subscribe((voided) => {
        if (voided) {
          this.refresh();
        }
      });
  }
}
