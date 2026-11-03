import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AuthService } from '../../../core/auth/auth.service';
import { ApiErrorResponse } from '../../../shared/models/api-response.model';

const SLUG_PATTERN = /^[a-z0-9][a-z0-9-]*[a-z0-9]$/;

/**
 * SUPER_ADMIN onboarding a new client: creates the tenant and its first CEO login in
 * one step. Closing with `true` tells SuperAdminPanel to refresh its tenant list.
 */
@Component({
  selector: 'app-onboard-tenant-dialog',
  imports: [ReactiveFormsModule, MatButtonModule, MatDialogModule, MatProgressSpinnerModule],
  templateUrl: './onboard-tenant-dialog.html',
  styleUrl: './onboard-tenant-dialog.css',
})
export class OnboardTenantDialog {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly dialogRef = inject(MatDialogRef<OnboardTenantDialog>);

  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    tenantName: ['', [Validators.required, Validators.maxLength(255)]],
    tenantSlug: ['', [Validators.required, Validators.pattern(SLUG_PATTERN), Validators.maxLength(100)]],
    adminFullName: ['', [Validators.required, Validators.maxLength(255)]],
    adminEmail: ['', [Validators.required, Validators.email]],
    adminPassword: ['', [Validators.required, Validators.minLength(8)]],
  });

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting.set(true);
    this.errorMessage.set(null);

    this.authService.onboardTenant(this.form.getRawValue()).subscribe({
      next: () => {
        this.submitting.set(false);
        this.dialogRef.close(true);
      },
      error: (error: HttpErrorResponse) => {
        this.submitting.set(false);
        const body = error.error as ApiErrorResponse | undefined;
        this.errorMessage.set(body?.message ?? 'Could not create this client. Please try again.');
      },
    });
  }

  cancel(): void {
    this.dialogRef.close(false);
  }
}
