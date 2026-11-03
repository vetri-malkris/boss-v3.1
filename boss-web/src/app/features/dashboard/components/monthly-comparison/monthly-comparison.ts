import { DecimalPipe, TitleCasePipe } from '@angular/common';
import { Component, input } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MonthlyComparison } from '../../../../shared/models/analytics.model';

@Component({
  selector: 'app-monthly-comparison',
  imports: [DecimalPipe, TitleCasePipe, MatCardModule, MatIconModule],
  templateUrl: './monthly-comparison.html',
  styleUrl: './monthly-comparison.css',
})
export class MonthlyComparisonPanel {
  readonly comparison = input<MonthlyComparison | null>(null);
}
