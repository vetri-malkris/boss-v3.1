import { Component, input, output } from '@angular/core';
import { DashboardHeader } from '../../features/dashboard/components/dashboard-header/dashboard-header';
import { Sidebar } from '../sidebar/sidebar';
import { CategoryCode } from '../../shared/models/access.model';

/**
 * The layout wrapper: a header bar plus a dynamic per-user sidebar and a content
 * area. Still exactly one route (/dashboard) - the sidebar filters what's shown
 * within this same screen, it doesn't navigate anywhere (section 2/3).
 */
@Component({
  selector: 'app-dashboard-layout',
  imports: [DashboardHeader, Sidebar],
  templateUrl: './dashboard-layout.html',
  styleUrl: './dashboard-layout.css',
})
export class DashboardLayout {
  readonly tenantName = input<string | null>(null);
  readonly uploadData = output<CategoryCode>();
  readonly manageUsers = output<void>();
}
