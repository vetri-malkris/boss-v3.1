import { Component, inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { DataEntryForm } from '../data-entry-form/data-entry-form';
import { DataRecord, SubcategorySchema } from '../../models/data-entry.model';

export interface EditRecordDialogData {
  schema: SubcategorySchema;
  record: DataRecord;
}

/** Thin wrapper around DataEntryForm in edit mode - closes itself once the correction saves. */
@Component({
  selector: 'app-edit-record-dialog',
  imports: [MatDialogModule, DataEntryForm],
  templateUrl: './edit-record-dialog.html',
  styleUrl: './edit-record-dialog.css',
})
export class EditRecordDialog {
  private readonly dialogRef = inject(MatDialogRef<EditRecordDialog>);
  readonly data = inject<EditRecordDialogData>(MAT_DIALOG_DATA);

  onSaved(): void {
    this.dialogRef.close(true);
  }

  close(): void {
    this.dialogRef.close(false);
  }
}
