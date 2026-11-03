import { Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { CategoryViewService } from '../../../core/services/category-view.service';
import { MetricTraceability } from '../../models/metric-traceability';

/**
 * Shows where one Overview number actually comes from - category, subcategory,
 * the exact field(s), and the formula - then offers to jump straight to that
 * subcategory's own page via the same selection mechanism the sidebar uses
 * (CategoryViewService.select), not a new navigation path.
 */
@Component({
  selector: 'app-metric-traceability-dialog',
  imports: [MatButtonModule, MatDialogModule],
  templateUrl: './metric-traceability-dialog.html',
  styleUrl: './metric-traceability-dialog.css',
})
export class MetricTraceabilityDialog {
  private readonly categoryViewService = inject(CategoryViewService);
  private readonly dialogRef = inject(MatDialogRef<MetricTraceabilityDialog>);
  readonly data = inject<MetricTraceability>(MAT_DIALOG_DATA);

  goToSubcategory(): void {
    const code = this.data.subcategory;
    if (code) {
      this.categoryViewService.select(code);
    }
    this.dialogRef.close();
  }

  close(): void {
    this.dialogRef.close();
  }
}
