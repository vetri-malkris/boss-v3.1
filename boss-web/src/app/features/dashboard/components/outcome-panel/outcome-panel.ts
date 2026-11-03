import { DecimalPipe } from '@angular/common';
import { Component, computed, inject } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { ActionService } from '../../../../core/services/action.service';

/**
 * Expected vs. actual outcome tracking (section 18) - a read-only view over completed
 * actions from the same shared ActionService state ActionPanel writes to. Variance was
 * already computed server-side when the action was completed; this never recalculates it.
 */
@Component({
  selector: 'app-outcome-panel',
  imports: [DecimalPipe, MatCardModule, MatIconModule],
  templateUrl: './outcome-panel.html',
  styleUrl: './outcome-panel.css',
})
export class OutcomePanel {
  private readonly actionService = inject(ActionService);

  readonly completed = computed(() => this.actionService.actions().filter((a) => a.status === 'COMPLETED'));
}
