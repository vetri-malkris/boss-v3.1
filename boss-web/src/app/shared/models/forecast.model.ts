export interface ForecastPoint {
  period: string;
  value: number;
}

export interface MetricForecast {
  metric: string;
  points: ForecastPoint[];
}

// The backend omits null fields from JSON (default-property-inclusion: non_null), so
// these come through as `undefined`, not `null`, when absent - always check with
// `!= null` (loose), never `!== null`.
export interface BreakEvenForecast {
  alreadyBreakEven: boolean;
  monthsFromNow?: number | null;
  period?: string | null;
}

export interface RunwayForecast {
  available: boolean;
  monthsUntilExhausted?: number | null;
  period?: string | null;
}

export interface Forecast {
  horizonMonths: number;
  hasEnoughData: boolean;
  revenue: MetricForecast | null;
  expenses: MetricForecast | null;
  cash?: MetricForecast | null;
  runway: RunwayForecast | null;
  breakEven: BreakEvenForecast | null;
}
