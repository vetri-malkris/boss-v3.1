import { Component, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatToolbarModule } from '@angular/material/toolbar';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/auth/auth.service';
import { TenantService } from '../../../core/services/tenant.service';
import { Tenant } from '../../../shared/models/tenant.model';
import { OnboardTenantDialog } from '../onboard-tenant-dialog/onboard-tenant-dialog';
import { TenantUsersDialog, TenantUsersDialogData } from '../tenant-users-dialog/tenant-users-dialog';

/**
 * What a SUPER_ADMIN sees instead of the normal tenant dashboard (still the same
 * single /dashboard route - section 2/31 - just different content for this role,
 * since a SUPER_ADMIN has no tenant of its own to show metrics for). Lists every
 * client on the platform, is the only place a new one gets onboarded, and is also
 * where a SUPER_ADMIN manages any tenant's users and their menu access.
 */
@Component({
  selector: 'app-super-admin-panel',
  imports: [
    DatePipe,
    FormsModule,
    MatButtonModule,
    MatCardModule,
    MatIconModule,
    MatMenuModule,
    MatProgressSpinnerModule,
    MatToolbarModule,
  ],
  templateUrl: './super-admin-panel.html',
  styleUrl: './super-admin-panel.css',
})
export class SuperAdminPanel {
  private readonly authService = inject(AuthService);
  private readonly tenantService = inject(TenantService);
  private readonly dialog = inject(MatDialog);
  private readonly router = inject(Router);

  readonly currentUser = this.authService.currentUser;
  readonly loading = signal(true);
  readonly tenants = signal<Tenant[]>([]);
  readonly search = signal('');

  readonly activeCount = computed(() => this.tenants().filter((t) => t.status === 'ACTIVE').length);

  readonly filteredTenants = computed(() => {
    const term = this.search().trim().toLowerCase();
    const all = this.tenants();
    if (!term) {
      return all;
    }
    return all.filter((t) => t.name.toLowerCase().includes(term) || t.slug.toLowerCase().includes(term));
  });

  constructor() {
    this.refresh();
  }

  refresh(): void {
    this.loading.set(true);
    this.tenantService.list().subscribe({
      next: (tenants) => {
        this.tenants.set(tenants);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  initialOf(name: string): string {
    return name.trim().charAt(0).toUpperCase() || '?';
  }

  openOnboardDialog(): void {
    this.dialog
      .open(OnboardTenantDialog, { width: '480px' })
      .afterClosed()
      .subscribe((created) => {
        if (created) {
          this.refresh();
        }
      });
  }

  openUsersDialog(tenant: Tenant): void {
    const data: TenantUsersDialogData = { tenantId: tenant.id, tenantName: tenant.name };
    this.dialog.open(TenantUsersDialog, { width: '640px', maxHeight: '85vh', data });
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
