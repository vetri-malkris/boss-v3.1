import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { DataEntryService } from '../../../core/services/data-entry.service';
import { ApiErrorResponse } from '../../models/api-response.model';
import { SubcategoryCode } from '../../models/access.model';
import { DataRecord } from '../../models/data-entry.model';

export interface VoidRecordDialogData {
  subcategory: SubcategoryCode;
  record: DataRecord;
}

/**
 * Voiding is a deliberate, reasoned action, never a silent one-click delete -
 * deletion must be conservative (section: data integrity). A reason is required
 * and becomes part of the audit trail.
 */
@Component({
  selector: 'app-void-record-dialog',
  imports: [ReactiveFormsModule, MatButtonModule, MatDialogModule, MatFormFieldModule, MatInputModule, MatProgressSpinnerModule],
  templateUrl: './void-record-dialog.html',
  styleUrl: './void-record-dialog.css',
})
export class VoidRecordDialog {
  private readonly fb = inject(FormBuilder);
  private readonly dataEntryService = inject(DataEntryService);
  private readonly dialogRef = inject(MatDialogRef<VoidRecordDialog>);
  readonly data = inject<VoidRecordDialogData>(MAT_DIALOG_DATA);

  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    reason: ['', [Validators.required, Validators.maxLength(500)]],
  });

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting.set(true);
    this.errorMessage.set(null);

    this.dataEntryService.voidRecord(this.data.subcategory, this.data.record.id, this.form.getRawValue().reason).subscribe({
      next: () => {
        this.submitting.set(false);
        this.dialogRef.close(true);
      },
      error: (error: HttpErrorResponse) => {
        this.submitting.set(false);
        const body = error.error as ApiErrorResponse | undefined;
        this.errorMessage.set(body?.message ?? 'Could not void this record. Please try again.');
      },
    });
  }

  close(): void {
    this.dialogRef.close(false);
  }
}
