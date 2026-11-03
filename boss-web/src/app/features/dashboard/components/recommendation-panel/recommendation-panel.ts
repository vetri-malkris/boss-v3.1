import { DecimalPipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialog } from '@angular/material/dialog';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { RecommendationService } from '../../../../core/services/recommendation.service';
import { ApiErrorResponse } from '../../../../shared/models/api-response.model';
import { Recommendation } from '../../../../shared/models/recommendation.model';
import { ProposeActionDialog, ProposeActionDialogData } from '../propose-action-dialog/propose-action-dialog';
import { CompactNumbersInTextPipe } from '../../../../shared/pipes/compact-numbers-in-text.pipe';

/**
 * Persisted, evidence-grounded recommendations (RecommendationEngine, section 7/15) -
 * generated deterministically from the risk engine, not the LLM. "Generate" writes new
 * rows (it's an explicit action, not run on every dashboard load) so history accumulates
 * rather than silently regenerating on each page visit.
 */
@Component({
  selector: 'app-recommendation-panel',
  imports: [DecimalPipe, MatButtonModule, MatCardModule, MatExpansionModule, MatIconModule, MatProgressSpinnerModule,
    CompactNumbersInTextPipe],
  templateUrl: './recommendation-panel.html',
  styleUrl: './recommendation-panel.css',
})
export class RecommendationPanel {
  private readonly recommendationService = inject(RecommendationService);
  private readonly dialog = inject(MatDialog);

  readonly loading = signal(true);
  readonly generating = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly recommendations = signal<Recommendation[]>([]);

  constructor() {
    this.recommendationService.list().subscribe({
      next: (list) => {
        this.recommendations.set(list);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  generate(): void {
    this.generating.set(true);
    this.errorMessage.set(null);

    this.recommendationService.generate().subscribe({
      next: (created) => {
        this.generating.set(false);
        this.recommendations.set([...created, ...this.recommendations()]);
      },
      error: (error: HttpErrorResponse) => {
        this.generating.set(false);
        const body = error.error as ApiErrorResponse | undefined;
        this.errorMessage.set(body?.message ?? 'Could not generate recommendations. Please try again.');
      },
    });
  }

  proposeAction(rec: Recommendation): void {
    const data: ProposeActionDialogData = { recommendationId: rec.id, suggestedDescription: rec.recommendedAction };
    this.dialog.open(ProposeActionDialog, { width: '480px', data });
  }
}
