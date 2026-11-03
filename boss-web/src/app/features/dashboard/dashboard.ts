import { Component, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AuthService } from '../../core/auth/auth.service';
import { DashboardService } from '../../core/services/dashboard.service';
import { CategoryService } from '../../core/services/category.service';
import { CategoryViewService } from '../../core/services/category-view.service';
import { DashboardState } from '../../shared/models/dashboard.model';
import { CategoryCode } from '../../shared/models/access.model';
import { DashboardLayout } from '../../layout/dashboard-layout/dashboard-layout';
import { AiChat } from './components/ai-chat/ai-chat';
import { CategoryContent } from './components/category-content/category-content';
import { UploadDialog, UploadDialogData } from './components/upload-dialog/upload-dialog';
import { ManageUsersDialog } from './components/manage-users-dialog/manage-users-dialog';
import { SuperAdminPanel } from '../super-admin/super-admin-panel/super-admin-panel';

/**
 * The single screen a signed-in user ever sees (section 2/31): every BOSS capability
 * is a card/panel/dialog composed here, never a separate route. Which panels show is
 * driven by the sidebar's selected subcategory (section: menu access) via CategoryContent -
 * except for SUPER_ADMIN, who has no tenant of their own and sees SuperAdminPanel
 * instead (still this same route, just different content for that one role).
 */
@Component({
  selector: 'app-dashboard',
  imports: [
    MatButtonModule,
    MatCardModule,
    MatIconModule,
    MatProgressSpinnerModule,
    DashboardLayout,
    AiChat,
    CategoryContent,
    SuperAdminPanel,
  ],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css',
})
export class Dashboard {
  private readonly authService = inject(AuthService);
  private readonly dashboardService = inject(DashboardService);
  private readonly dialog = inject(MatDialog);
  private readonly categoryViewService = inject(CategoryViewService);
  private readonly categoryService = inject(CategoryService);

  readonly isSuperAdmin = computed(() => this.authService.hasRole('SUPER_ADMIN'));
  readonly loading = signal(true);
  readonly dashboard = signal<DashboardState | null>(null);
  readonly selected = this.categoryViewService.selected;

  constructor() {
    if (!this.isSuperAdmin()) {
      this.reload();
    }
  }

  reload(): void {
    this.loading.set(true);
    this.dashboardService.load().subscribe({
      next: (state) => {
        this.dashboard.set(state);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  openUploadDialog(category: CategoryCode): void {
    const categoryLabel = this.categoryService.taxonomy().find((c) => c.code === category)?.label ?? category;
    const data: UploadDialogData = { category, categoryLabel };

    this.dialog
      .open(UploadDialog, { width: '560px', data })
      .afterClosed()
      .subscribe((refreshed) => {
        if (refreshed) {
          this.reload();
        }
      });
  }

  openManageUsers(): void {
    this.dialog.open(ManageUsersDialog, { width: '640px', maxHeight: '85vh' });
  }
}
