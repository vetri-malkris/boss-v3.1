import { Component, computed, inject, input, output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatToolbarModule } from '@angular/material/toolbar';
import { Router } from '@angular/router';
import { AuthService } from '../../../../core/auth/auth.service';
import { CategoryService } from '../../../../core/services/category.service';
import { CategoryCode } from '../../../../shared/models/access.model';

@Component({
  selector: 'app-dashboard-header',
  imports: [MatButtonModule, MatIconModule, MatMenuModule, MatTooltipModule, MatToolbarModule],
  templateUrl: './dashboard-header.html',
  styleUrl: './dashboard-header.css',
})
export class DashboardHeader {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly categoryService = inject(CategoryService);

  readonly tenantName = input<string | null>(null);
  readonly uploadData = output<CategoryCode>();
  readonly manageUsers = output<void>();

  readonly currentUser = this.authService.currentUser;
  readonly canManageUsers = computed(() => this.authService.hasAnyRole(['TENANT_ADMIN', 'CEO']));
  readonly activeCategory = this.categoryService.activeCategory;

  emitUploadData(): void {
    const category = this.activeCategory();
    if (category) {
      this.uploadData.emit(category);
    }
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
