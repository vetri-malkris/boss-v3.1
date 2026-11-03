export type ChangeDirection = 'INCREASE' | 'DECREASE' | 'STABLE';
export type TrendDirection = 'INCREASING' | 'DECREASING' | 'STABLE' | 'VOLATILE';

export interface Change {
  metric: string;
  currentValue: number;
  previousValue: number;
  change: number;
  changePercentage: number;
  direction: ChangeDirection;
}

export interface MonthlyComparison {
  currentPeriod: string;
  previousPeriod: string;
  changes: Change[];
}

export interface Trend {
  metric: string;
  direction: TrendDirection;
  monthsObserved: number;
}
