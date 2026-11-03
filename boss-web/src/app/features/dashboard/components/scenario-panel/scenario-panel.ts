import { DecimalPipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ScenarioService } from '../../../../core/services/scenario.service';
import { ApiErrorResponse } from '../../../../shared/models/api-response.model';
import { ScenarioResult } from '../../../../shared/models/scenario.model';

/**
 * Runs a real scenario against the tenant's forecast trend (ScenarioEngine, section
 * 12): clones nothing destructively - it's a pure read+compute against the existing
 * forecast, so running it can never affect the real dashboard data.
 */
@Component({
  selector: 'app-scenario-panel',
  imports: [DecimalPipe, FormsModule, MatButtonModule, MatCardModule, MatIconModule, MatProgressSpinnerModule],
  templateUrl: './scenario-panel.html',
  styleUrl: './scenario-panel.css',
})
export class ScenarioPanel {
  private readonly scenarioService = inject(ScenarioService);

  readonly revenueChangePercent = signal(-10);
  readonly expenseChangePercent = signal(5);
  readonly additionalHires = signal(0);
  readonly avgMonthlyCostPerHire = signal(5000);

  readonly running = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly result = signal<ScenarioResult | null>(null);

  runScenario(): void {
    this.running.set(true);
    this.errorMessage.set(null);
    this.result.set(null);

    this.scenarioService
      .simulate({
        revenueChangePercent: this.revenueChangePercent(),
        expenseChangePercent: this.expenseChangePercent(),
        additionalHires: this.additionalHires(),
        avgMonthlyCostPerHire: this.avgMonthlyCostPerHire(),
      })
      .subscribe({
        next: (result) => {
          this.running.set(false);
          this.result.set(result);
        },
        error: (error: HttpErrorResponse) => {
          this.running.set(false);
          const body = error.error as ApiErrorResponse | undefined;
          this.errorMessage.set(body?.message ?? 'Could not run the scenario. Please try again.');
        },
      });
  }
}
