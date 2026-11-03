import { Pipe, PipeTransform, LOCALE_ID, inject } from '@angular/core';
import { compactAmountParts } from '../utils/compact-amount';

/** Applies the exact same "Mn" compacting rule as the metric tiles, but inside a
 * backend-generated sentence (risk evidence, recommendation impact, action
 * descriptions) rather than a dedicated numeric field. Every standalone number in
 * the text is checked independently - anything below 1,000,000 (dates, percentages,
 * small counts) passes through untouched; only a genuine 7+ digit amount is
 * rewritten, with the "Mn" suffix wrapped for the shared 3/4-size style. Bind with
 * [innerHTML], not interpolation, since this returns markup. */
@Pipe({ name: 'compactNumbersInText' })
export class CompactNumbersInTextPipe implements PipeTransform {
  private readonly locale = inject(LOCALE_ID);
  private static readonly NUMBER = /-?\d[\d,]*(?:\.\d+)?/g;

  transform(text: string | null | undefined): string {
    if (!text) {
      return '';
    }
    return text.replace(CompactNumbersInTextPipe.NUMBER, (match) => {
      const value = Number(match.replace(/,/g, ''));
      if (!Number.isFinite(value)) {
        return match;
      }
      const { compact, text: formatted } = compactAmountParts(value, this.locale);
      return compact ? `${formatted}<span class="mn-suffix">&nbsp;Mn</span>` : match;
    });
  }
}
