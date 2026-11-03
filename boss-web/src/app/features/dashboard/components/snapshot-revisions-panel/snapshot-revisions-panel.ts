import { Component, inject, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { FinancialHistoryService } from '../../../../core/services/financial-history.service';
import { SnapshotRevision } from '../../../../shared/models/snapshot-revision.model';

/**
 * Historical financial facts are never silently overwritten (section: data
 * integrity) - if a re-upload changes a month that was already recorded, the prior
 * numbers are preserved and shown here. Renders nothing when there are no
 * corrections, so it never clutters a tenant that's never had one.
 */
@Component({
  selector: 'app-snapshot-revisions-panel',
  imports: [MatCardModule, MatIconModule, DatePipe, DecimalPipe],
  templateUrl: './snapshot-revisions-panel.html',
  styleUrl: './snapshot-revisions-panel.css',
})
export class SnapshotRevisionsPanel {
  private readonly financialHistoryService = inject(FinancialHistoryService);

  readonly revisions = signal<SnapshotRevision[]>([]);
  readonly loaded = signal(false);

  constructor() {
    this.financialHistoryService.revisions().subscribe({
      next: (revisions) => {
        this.revisions.set(revisions);
        this.loaded.set(true);
      },
      error: () => this.loaded.set(true),
    });
  }
}
