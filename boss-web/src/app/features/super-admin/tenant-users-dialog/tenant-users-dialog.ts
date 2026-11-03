import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { CategoryService } from '../../../core/services/category.service';
import { SuperAdminUserService } from '../../../core/services/super-admin-user.service';
import { ApiErrorResponse } from '../../../shared/models/api-response.model';
import { RoleName } from '../../../shared/models/role.model';
import { UserSummary } from '../../../shared/models/user.model';
import { SubcategoryCode } from '../../../shared/models/access.model';
import { SubcategoryPicker } from '../../../shared/components/subcategory-picker/subcategory-picker';

const ASSIGNABLE_ROLES: RoleName[] = ['TENANT_ADMIN', 'CEO', 'CFO', 'FINANCE_MANAGER', 'MANAGER', 'EMPLOYEE', 'VIEWER'];

export interface TenantUsersDialogData {
  tenantId: string;
  tenantName: string;
}

type Mode = 'list' | 'create' | 'access';

/**
 * SUPER_ADMIN's equivalent of ManageUsersDialog - same create/edit-access flow, but
 * against an arbitrary tenant (passed in as dialog data) via SuperAdminUserService
 * instead of the caller's own tenant.
 */
@Component({
  selector: 'app-tenant-users-dialog',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatChipsModule,
    MatDialogModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatSelectModule,
    SubcategoryPicker,
  ],
  templateUrl: './tenant-users-dialog.html',
  styleUrl: './tenant-users-dialog.css',
})
export class TenantUsersDialog {
  private readonly fb = inject(FormBuilder);
  private readonly superAdminUserService = inject(SuperAdminUserService);
  private readonly categoryService = inject(CategoryService);
  readonly data = inject<TenantUsersDialogData>(MAT_DIALOG_DATA);

  readonly assignableRoles = ASSIGNABLE_ROLES;
  readonly taxonomy = this.categoryService.taxonomy;

  readonly mode = signal<Mode>('list');
  readonly loading = signal(true);
  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly users = signal<UserSummary[]>([]);
  readonly selectedUser = signal<UserSummary | null>(null);
  readonly createSelection = signal<SubcategoryCode[]>([]);
  readonly createSelectionValid = signal(false);
  readonly accessSelection = signal<SubcategoryCode[]>([]);
  readonly accessSelectionValid = signal(false);

  readonly createForm = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8)]],
    fullName: ['', [Validators.required, Validators.maxLength(255)]],
    roles: this.fb.nonNullable.control<RoleName[]>([], [Validators.required]),
  });

  constructor() {
    this.categoryService.load().subscribe();
    this.refresh();
  }

  refresh(): void {
    this.loading.set(true);
    this.superAdminUserService.list(this.data.tenantId).subscribe({
      next: (users) => {
        this.users.set(users);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  showCreate(): void {
    this.errorMessage.set(null);
    this.createForm.reset({ email: '', password: '', fullName: '', roles: [] });
    this.createSelection.set([]);
    this.createSelectionValid.set(false);
    this.mode.set('create');
  }

  showAccess(user: UserSummary): void {
    this.errorMessage.set(null);
    this.selectedUser.set(user);
    this.accessSelection.set(user.enabledSubcategories);
    this.accessSelectionValid.set(true); // starts from the user's existing (valid) access
    this.mode.set('access');
  }

  showList(): void {
    this.mode.set('list');
  }

  submitCreate(): void {
    if (this.createForm.invalid) {
      this.createForm.markAllAsTouched();
      return;
    }
    this.submitting.set(true);
    this.errorMessage.set(null);

    const value = this.createForm.getRawValue();
    this.superAdminUserService
      .create(this.data.tenantId, { ...value, enabledSubcategories: this.createSelection() })
      .subscribe({
        next: (created) => {
          this.submitting.set(false);
          this.users.update((list) => [created, ...list]);
          this.mode.set('list');
        },
        error: (error: HttpErrorResponse) => {
          this.submitting.set(false);
          const body = error.error as ApiErrorResponse | undefined;
          this.errorMessage.set(body?.message ?? 'Could not create this user. Please try again.');
        },
      });
  }

  submitAccess(): void {
    const user = this.selectedUser();
    if (!user) {
      return;
    }
    this.submitting.set(true);
    this.errorMessage.set(null);

    this.superAdminUserService.updateAccess(this.data.tenantId, user.id, this.accessSelection()).subscribe({
      next: (updated) => {
        this.submitting.set(false);
        this.users.update((list) => list.map((u) => (u.id === updated.id ? updated : u)));
        this.mode.set('list');
      },
      error: (error: HttpErrorResponse) => {
        this.submitting.set(false);
        const body = error.error as ApiErrorResponse | undefined;
        this.errorMessage.set(body?.message ?? 'Could not update access. Please try again.');
      },
    });
  }
}
