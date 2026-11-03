import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';

/**
 * Exactly two screens, per the product requirement: /login and /dashboard.
 * Everything else is a card, panel, or dialog inside the dashboard - never a route.
 */
export const routes: Routes = [
  { path: 'login', loadComponent: () => import('./features/login/login').then((m) => m.Login) },
  {
    path: 'dashboard',
    canActivate: [authGuard],
    loadComponent: () => import('./features/dashboard/dashboard').then((m) => m.Dashboard),
  },
  { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
  { path: '**', redirectTo: 'dashboard' },
];
