import { DecimalPipe } from '@angular/common';
import { Component, computed, input } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { Forecast } from '../../../../shared/models/forecast.model';

/**
 * Deterministic linear-trend forecast over the tenant's snapshot history
 * (ForecastService, section 11) - real numbers, not a placeholder. Shows an honest
 * empty state until there are at least 3 months of data to fit a trend on.
 */
@Component({
  selector: 'app-forecast-panel',
  imports: [DecimalPipe, MatCardModule, MatIconModule],
  templateUrl: './forecast-panel.html',
  styleUrl: './forecast-panel.css',
})
export class ForecastPanel {
  readonly forecast = input<Forecast | null>(null);

  readonly nextRevenue = computed(() => this.forecast()?.revenue?.points?.[0] ?? null);
  readonly nextExpenses = computed(() => this.forecast()?.expenses?.points?.[0] ?? null);
}
