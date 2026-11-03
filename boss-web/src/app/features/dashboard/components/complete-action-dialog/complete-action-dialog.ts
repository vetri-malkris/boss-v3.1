import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ActionService } from '../../../../core/services/action.service';
import { ApiErrorResponse } from '../../../../shared/models/api-response.model';

export interface CompleteActionDialogData {
  actionId: string;
  expectedOutcomeMetric: string;
  expectedOutcomeValue: number;
}

@Component({
  selector: 'app-complete-action-dialog',
  imports: [ReactiveFormsModule, MatButtonModule, MatDialogModule, MatFormFieldModule, MatInputModule, MatProgressSpinnerModule],
  templateUrl: './complete-action-dialog.html',
  styleUrl: './complete-action-dialog.css',
})
export class CompleteActionDialog {
  private readonly fb = inject(FormBuilder);
  private readonly actionService = inject(ActionService);
  private readonly dialogRef = inject(MatDialogRef<CompleteActionDialog>);
  readonly data = inject<CompleteActionDialogData>(MAT_DIALOG_DATA);

  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    actualOutcomeValue: [this.data.expectedOutcomeValue, [Validators.required]],
    outcomeReason: ['', [Validators.maxLength(1000)]],
  });

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting.set(true);
    this.errorMessage.set(null);

    const value = this.form.getRawValue();
    this.actionService.complete(this.data.actionId, value).subscribe({
      next: () => {
        this.submitting.set(false);
        this.dialogRef.close(true);
      },
      error: (error: HttpErrorResponse) => {
        this.submitting.set(false);
        const body = error.error as ApiErrorResponse | undefined;
        this.errorMessage.set(body?.message ?? 'Could not record the outcome. Please try again.');
      },
    });
  }

  close(): void {
    this.dialogRef.close(false);
  }
}
