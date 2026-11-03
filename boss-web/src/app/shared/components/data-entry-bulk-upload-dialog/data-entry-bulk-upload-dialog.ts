import { Component, ElementRef, ViewChild, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { DataEntryService } from '../../../core/services/data-entry.service';
import { ApiErrorResponse } from '../../models/api-response.model';
import { SubcategorySchema } from '../../models/data-entry.model';

const ACCEPTED_EXTENSIONS = ['.csv', '.xlsx', '.xls'];

export interface DataEntryBulkUploadDialogData {
  schema: SubcategorySchema;
}

/** Bulk-upload a CSV/Excel of records for one subcategory - same dropzone pattern as the main data upload dialog. */
@Component({
  selector: 'app-data-entry-bulk-upload-dialog',
  imports: [MatButtonModule, MatCardModule, MatDialogModule, MatIconModule, MatProgressSpinnerModule],
  templateUrl: './data-entry-bulk-upload-dialog.html',
  styleUrl: './data-entry-bulk-upload-dialog.css',
})
export class DataEntryBulkUploadDialog {
  private readonly dataEntryService = inject(DataEntryService);
  private readonly dialogRef = inject(MatDialogRef<DataEntryBulkUploadDialog>);
  readonly data = inject<DataEntryBulkUploadDialogData>(MAT_DIALOG_DATA);

  @ViewChild('fileInput') fileInputRef!: ElementRef<HTMLInputElement>;

  readonly selectedFile = signal<File | null>(null);
  readonly dragOver = signal(false);
  readonly downloadingTemplate = signal(false);
  readonly uploading = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly rowsCreated = signal<number | null>(null);
  readonly warnings = signal<string[]>([]);

  downloadTemplate(): void {
    this.downloadingTemplate.set(true);
    this.errorMessage.set(null);
    this.dataEntryService.downloadTemplate(this.data.schema.subcategory).subscribe({
      next: () => this.downloadingTemplate.set(false),
      error: () => {
        this.downloadingTemplate.set(false);
        this.errorMessage.set('Could not download the template. Please try again.');
      },
    });
  }

  openFilePicker(): void {
    this.fileInputRef.nativeElement.click();
  }

  onFileInputChange(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.setFile(input.files?.[0] ?? null);
    input.value = '';
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    this.dragOver.set(false);
    this.setFile(event.dataTransfer?.files?.[0] ?? null);
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    this.dragOver.set(true);
  }

  onDragLeave(): void {
    this.dragOver.set(false);
  }

  upload(): void {
    const file = this.selectedFile();
    if (!file) {
      return;
    }
    this.uploading.set(true);
    this.errorMessage.set(null);

    this.dataEntryService.bulkUpload(this.data.schema.subcategory, file).subscribe({
      next: (result) => {
        this.uploading.set(false);
        this.rowsCreated.set(result.rowsCreated);
        this.warnings.set(result.warnings);
        if (result.warnings.length === 0) {
          setTimeout(() => this.dialogRef.close(true), 900);
        }
      },
      error: (error: HttpErrorResponse) => {
        this.uploading.set(false);
        const body = error.error as ApiErrorResponse | undefined;
        this.errorMessage.set(body?.message ?? 'Could not upload this file. Please try again.');
      },
    });
  }

  close(): void {
    this.dialogRef.close(this.rowsCreated() !== null && this.rowsCreated()! > 0);
  }

  private setFile(file: File | null): void {
    if (!file) {
      return;
    }
    const isAccepted = ACCEPTED_EXTENSIONS.some((ext) => file.name.toLowerCase().endsWith(ext));
    if (!isAccepted) {
      this.errorMessage.set('Please choose a .csv or .xlsx file.');
      return;
    }
    this.errorMessage.set(null);
    this.rowsCreated.set(null);
    this.warnings.set([]);
    this.selectedFile.set(file);
  }
}
