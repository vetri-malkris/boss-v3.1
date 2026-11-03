import { Component, LOCALE_ID, computed, inject, input } from '@angular/core';
import { compactAmountParts } from '../../utils/compact-amount';

/** Renders one amount on a metric tile - below 1,000,000 exactly as always
 * ("1,234.00"); at 1,000,000 or above, as "2.27" + a smaller "Mn" suffix (the
 * suffix is rendered at 3/4 the size of the number, via the shared `.mn-suffix`
 * class in styles.css - a plain string couldn't mix two font sizes). */
@Component({
  selector: 'app-compact-amount-display',
  templateUrl: './compact-amount-display.html',
})
export class CompactAmountDisplay {
  private readonly locale = inject(LOCALE_ID);

  readonly value = input.required<number>();

  private readonly parts = computed(() => compactAmountParts(this.value(), this.locale));

  readonly isCompact = computed(() => this.parts().compact);
  readonly text = computed(() => this.parts().text);
}
