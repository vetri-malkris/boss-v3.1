import { DecimalPipe } from '@angular/common';
import { Component, inject, input } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatDialog } from '@angular/material/dialog';
import { Metrics } from '../../../../shared/models/dashboard.model';
import { CompactAmountDisplay } from '../../../../shared/components/compact-amount-display/compact-amount-display';
import { METRIC_TRACEABILITY } from '../../../../shared/models/metric-traceability';
import { MetricTraceabilityDialog } from '../../../../shared/components/metric-traceability-dialog/metric-traceability-dialog';

@Component({
  selector: 'app-metric-cards',
  imports: [DecimalPipe, MatCardModule, CompactAmountDisplay],
  templateUrl: './metric-cards.html',
  styleUrl: './metric-cards.css',
})
export class MetricCards {
  private readonly dialog = inject(MatDialog);

  readonly metrics = input.required<Metrics>();

  showTraceability(key: keyof Metrics): void {
    const traceability = METRIC_TRACEABILITY[key];
    if (traceability) {
      this.dialog.open(MetricTraceabilityDialog, { width: '420px', data: traceability });
    }
  }
}
