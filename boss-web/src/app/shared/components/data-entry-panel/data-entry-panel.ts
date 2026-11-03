import { Component, inject, input, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { DataEntryBulkUploadDialog } from '../data-entry-bulk-upload-dialog/data-entry-bulk-upload-dialog';
import { DataEntryForm } from '../data-entry-form/data-entry-form';
import { DataEntryRecords } from '../data-entry-records/data-entry-records';
import { SubcategorySchema } from '../../models/data-entry.model';

/**
 * The data-entry surface for one subcategory: its own form, its own bulk upload, and
 * a table of what's been entered so far - composed here so CategoryContent only has
 * to pass in the schema.
 */
@Component({
  selector: 'app-data-entry-panel',
  imports: [MatButtonModule, MatCardModule, MatIconModule, DataEntryForm, DataEntryRecords],
  templateUrl: './data-entry-panel.html',
  styleUrl: './data-entry-panel.css',
})
export class DataEntryPanel {
  private readonly dialog = inject(MatDialog);

  readonly schema = input.required<SubcategorySchema>();
  readonly refreshToken = signal(0);

  onRecordCreated(): void {
    this.refreshToken.update((value) => value + 1);
  }

  openBulkUpload(): void {
    this.dialog
      .open(DataEntryBulkUploadDialog, { width: '520px', data: { schema: this.schema() } })
      .afterClosed()
      .subscribe((created) => {
        if (created) {
          this.refreshToken.update((value) => value + 1);
        }
      });
  }
}
