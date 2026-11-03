import { Component, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { Observable } from 'rxjs';
import { AuthService } from '../../../../core/auth/auth.service';
import { ActionService } from '../../../../core/services/action.service';
import { ActionItem } from '../../../../shared/models/action.model';
import { ApiErrorResponse } from '../../../../shared/models/api-response.model';
import { CompleteActionDialog, CompleteActionDialogData } from '../complete-action-dialog/complete-action-dialog';
import { CompactNumbersInTextPipe } from '../../../../shared/pipes/compact-numbers-in-text.pipe';
import { CompactAmountDisplay } from '../../../../shared/components/compact-amount-display/compact-amount-display';

const APPROVER_ROLES = ['TENANT_ADMIN', 'CFO', 'CEO'] as const;

/**
 * The human-approval gate (section 16): pending actions sit here until an approver
 * (TENANT_ADMIN/CFO/CEO) approves or rejects them - nothing here executes itself.
 * Reads/writes the shared signal state in ActionService, so a "Propose action" done
 * elsewhere (RecommendationPanel) shows up here without a manual refresh.
 */
@Component({
  selector: 'app-action-panel',
  imports: [MatButtonModule, MatCardModule, MatChipsModule, MatIconModule, CompactNumbersInTextPipe, CompactAmountDisplay],
  templateUrl: './action-panel.html',
  styleUrl: './action-panel.css',
})
export class ActionPanel {
  private readonly actionService = inject(ActionService);
  private readonly authService = inject(AuthService);
  private readonly dialog = inject(MatDialog);

  readonly actions = this.actionService.actions;
  readonly errorMessage = signal<string | null>(null);
  readonly canApprove = this.authService.hasAnyRole([...APPROVER_ROLES]);

  constructor() {
    this.actionService.refresh().subscribe();
  }

  approve(action: ActionItem): void {
    this.run(this.actionService.approve(action.id));
  }

  reject(action: ActionItem): void {
    this.run(this.actionService.reject(action.id));
  }

  start(action: ActionItem): void {
    this.run(this.actionService.start(action.id));
  }

  cancel(action: ActionItem): void {
    this.run(this.actionService.cancel(action.id));
  }

  complete(action: ActionItem): void {
    const data: CompleteActionDialogData = {
      actionId: action.id,
      expectedOutcomeMetric: action.expectedOutcomeMetric,
      expectedOutcomeValue: action.expectedOutcomeValue,
    };
    this.dialog.open(CompleteActionDialog, { width: '440px', data });
  }

  private run(request: Observable<ActionItem>): void {
    this.errorMessage.set(null);
    request.subscribe({
      error: (error: HttpErrorResponse) => {
        const body = error.error as ApiErrorResponse | undefined;
        this.errorMessage.set(body?.message ?? 'Could not update this action. Please try again.');
      },
    });
  }
}
