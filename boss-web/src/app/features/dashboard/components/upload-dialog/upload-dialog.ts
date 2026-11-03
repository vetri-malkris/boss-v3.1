import { Component, ElementRef, ViewChild, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { CategoryUploadService } from '../../../../core/services/category-upload.service';
import { ApiErrorResponse } from '../../../../shared/models/api-response.model';
import { CategoryCode } from '../../../../shared/models/access.model';
import { CategoryUploadResult } from '../../../../shared/models/category-upload.model';

const ACCEPTED_EXTENSIONS = ['.xlsx'];

export interface UploadDialogData {
  category: CategoryCode;
  categoryLabel: string;
}

/**
 * The only way data enters BOSS: a modal, not a separate page (section 3). Category-
 * aware - offers that category's multi-sheet template (one sheet per subcategory) and
 * uploads it back atomically: any missing mandatory field anywhere rejects the whole
 * file, nothing partial is ever written. Closes with `true` once an upload succeeds so
 * the dashboard knows to reload its state.
 */
@Component({
  selector: 'app-upload-dialog',
  imports: [MatButtonModule, MatCardModule, MatDialogModule, MatIconModule, MatProgressSpinnerModule],
  templateUrl: './upload-dialog.html',
  styleUrl: './upload-dialog.css',
})
export class UploadDialog {
  private readonly categoryUploadService = inject(CategoryUploadService);
  private readonly dialogRef = inject(MatDialogRef<UploadDialog>);
  readonly data = inject<UploadDialogData>(MAT_DIALOG_DATA);

  @ViewChild('fileInput') fileInputRef!: ElementRef<HTMLInputElement>;

  readonly selectedFile = signal<File | null>(null);
  readonly dragOver = signal(false);
  readonly downloadingTemplate = signal(false);
  readonly uploading = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly result = signal<CategoryUploadResult | null>(null);

  downloadTemplate(): void {
    this.downloadingTemplate.set(true);
    this.errorMessage.set(null);
    this.categoryUploadService.downloadTemplate(this.data.category).subscribe({
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

    this.categoryUploadService.bulkUpload(this.data.category, file).subscribe({
      next: (result) => {
        this.uploading.set(false);
        this.result.set(result);
        setTimeout(() => this.dialogRef.close(true), 900);
      },
      error: (error: HttpErrorResponse) => {
        this.uploading.set(false);
        const body = error.error as ApiErrorResponse | undefined;
        this.errorMessage.set(body?.message ?? 'Could not upload this file. Please try again.');
      },
    });
  }

  close(): void {
    this.dialogRef.close(false);
  }

  private setFile(file: File | null): void {
    if (!file) {
      return;
    }
    const isAccepted = ACCEPTED_EXTENSIONS.some((ext) => file.name.toLowerCase().endsWith(ext));
    if (!isAccepted) {
      this.errorMessage.set('Please choose the .xlsx template file.');
      return;
    }
    this.errorMessage.set(null);
    this.result.set(null);
    this.selectedFile.set(file);
  }
}
