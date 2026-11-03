import { BreakEvenForecast, MetricForecast, RunwayForecast } from './forecast.model';

export interface ScenarioRequest {
  revenueChangePercent: number;
  expenseChangePercent: number;
  additionalHires: number;
  avgMonthlyCostPerHire: number;
}

export interface ScenarioSide {
  nextRevenue: number;
  nextExpenses: number;
  nextProfit: number;
  nextMargin: number;
  cash: MetricForecast | null;
  runway: RunwayForecast;
  breakEven: BreakEvenForecast;
}

export interface ScenarioResult {
  horizonMonths: number;
  hasEnoughData: boolean;
  base: ScenarioSide | null;
  scenario: ScenarioSide | null;
}
