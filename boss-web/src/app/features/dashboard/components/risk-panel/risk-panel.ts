import { Component, input } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatIconModule } from '@angular/material/icon';
import { Risk } from '../../../../shared/models/risk.model';
import { CompactNumbersInTextPipe } from '../../../../shared/pipes/compact-numbers-in-text.pipe';

@Component({
  selector: 'app-risk-panel',
  imports: [MatCardModule, MatExpansionModule, MatIconModule, CompactNumbersInTextPipe],
  templateUrl: './risk-panel.html',
  styleUrl: './risk-panel.css',
})
export class RiskPanel {
  readonly risks = input.required<Risk[]>();

  severityIcon(severity: Risk['severity']): string {
    return severity === 'HIGH' ? 'error' : severity === 'MEDIUM' ? 'warning' : 'info';
  }
}
