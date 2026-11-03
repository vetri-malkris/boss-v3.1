import { DecimalPipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AiService } from '../../../../core/services/ai.service';
import { ApiErrorResponse } from '../../../../shared/models/api-response.model';
import { ChatMessage } from '../../../../shared/models/ai.model';

/**
 * A real chat UI backed by the AI orchestrator + MockAiClient (section 13) - every
 * number it shows is grounded in the tenant's actual dashboard data, gathered via
 * real tool calls server-side. No LLM provider is configured by default, so answers
 * are template-based over real evidence rather than free-form generation; wiring a
 * real provider (AI_PROVIDER env var) upgrades this without any frontend change.
 */
@Component({
  selector: 'app-ai-chat',
  imports: [DecimalPipe, FormsModule, MatButtonModule, MatCardModule, MatChipsModule, MatFormFieldModule, MatIconModule, MatInputModule, MatProgressSpinnerModule],
  templateUrl: './ai-chat.html',
  styleUrl: './ai-chat.css',
})
export class AiChat {
  private readonly aiService = inject(AiService);

  readonly messages = signal<ChatMessage[]>([]);
  readonly draft = signal('');
  readonly sending = signal(false);

  readonly suggestions = ['Why did profit decrease?', 'What is our runway?', 'Are there any risks?'];

  ask(text?: string): void {
    const message = (text ?? this.draft()).trim();
    if (!message || this.sending()) {
      return;
    }

    this.messages.update((m) => [...m, { role: 'user', text: message }]);
    this.draft.set('');
    this.sending.set(true);

    this.aiService.ask(message).subscribe({
      next: (response) => {
        this.sending.set(false);
        this.messages.update((m) => [...m, { role: 'assistant', text: response.summary, response }]);
      },
      error: (error: HttpErrorResponse) => {
        this.sending.set(false);
        const body = error.error as ApiErrorResponse | undefined;
        this.messages.update((m) => [
          ...m,
          { role: 'assistant', text: body?.message ?? 'Something went wrong answering that. Please try again.' },
        ]);
      },
    });
  }
}
