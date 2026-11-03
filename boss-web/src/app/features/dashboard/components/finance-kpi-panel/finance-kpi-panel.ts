import { DecimalPipe } from '@angular/common';
import { Component, inject, input } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatDialog } from '@angular/material/dialog';
import { FinanceKpis } from '../../../../shared/models/dashboard.model';
import { CompactAmountDisplay } from '../../../../shared/components/compact-amount-display/compact-amount-display';
import { METRIC_TRACEABILITY } from '../../../../shared/models/metric-traceability';
import { MetricTraceabilityDialog } from '../../../../shared/components/metric-traceability-dialog/metric-traceability-dialog';

/**
 * KPIs derived from the manually-entered Finance forms (Position, Profitability,
 * Budget & Forecast, Capital) - shown on Overview only, alongside (not instead
 * of) those forms' own pages, which show just the form. A separate authoritative
 * source from `MetricCards`' transaction-upload-driven numbers, never merged.
 */
@Component({
  selector: 'app-finance-kpi-panel',
  imports: [DecimalPipe, MatCardModule, CompactAmountDisplay],
  templateUrl: './finance-kpi-panel.html',
  styleUrl: './finance-kpi-panel.css',
})
export class FinanceKpiPanel {
  private readonly dialog = inject(MatDialog);

  readonly kpis = input.required<FinanceKpis>();

  showTraceability(key: keyof FinanceKpis): void {
    const traceability = METRIC_TRACEABILITY[key];
    if (traceability) {
      this.dialog.open(MetricTraceabilityDialog, { width: '420px', data: traceability });
    }
  }
}
