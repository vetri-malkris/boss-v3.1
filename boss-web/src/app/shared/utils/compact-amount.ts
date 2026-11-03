import { formatNumber } from '@angular/common';

/** Shared threshold/rounding rule behind every "Mn" display on Overview - the one
 * place this logic lives, so the tile component and the free-text pipe can never
 * drift apart. Values at 1,000,000 or above compact to millions (2 decimals max,
 * trailing zeros stripped); everything else renders exactly as it always has. */
export function compactAmountParts(value: number, locale: string): { compact: boolean; text: string } {
  if (Math.abs(value) < 1_000_000) {
    return { compact: false, text: formatNumber(value, locale, '1.2-2') };
  }
  const millions = value / 1_000_000;
  return { compact: true, text: formatNumber(millions, locale, '1.0-2') };
}
