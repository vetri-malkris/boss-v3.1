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

export interface ProposeActionDialogData {
  recommendationId: string;
  suggestedDescription: string;
}

@Component({
  selector: 'app-propose-action-dialog',
  imports: [ReactiveFormsModule, MatButtonModule, MatDialogModule, MatFormFieldModule, MatInputModule, MatProgressSpinnerModule],
  templateUrl: './propose-action-dialog.html',
  styleUrl: './propose-action-dialog.css',
})
export class ProposeActionDialog {
  private readonly fb = inject(FormBuilder);
  private readonly actionService = inject(ActionService);
  private readonly dialogRef = inject(MatDialogRef<ProposeActionDialog>);
  private readonly data = inject<ProposeActionDialogData>(MAT_DIALOG_DATA);

  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    description: [this.data.suggestedDescription, [Validators.required, Validators.maxLength(1000)]],
    expectedOutcomeMetric: ['Monthly profit', [Validators.required, Validators.maxLength(255)]],
    expectedOutcomeValue: [0, [Validators.required]],
  });

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting.set(true);
    this.errorMessage.set(null);

    const value = this.form.getRawValue();
    this.actionService
      .propose({
        recommendationId: this.data.recommendationId,
        description: value.description,
        expectedOutcomeMetric: value.expectedOutcomeMetric,
        expectedOutcomeValue: value.expectedOutcomeValue,
      })
      .subscribe({
        next: () => {
          this.submitting.set(false);
          this.dialogRef.close(true);
        },
        error: (error: HttpErrorResponse) => {
          this.submitting.set(false);
          const body = error.error as ApiErrorResponse | undefined;
          this.errorMessage.set(body?.message ?? 'Could not propose this action. Please try again.');
        },
      });
  }

  close(): void {
    this.dialogRef.close(false);
  }
}
