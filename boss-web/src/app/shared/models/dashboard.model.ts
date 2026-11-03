import { MonthlyComparison, Trend } from './analytics.model';
import { Forecast } from './forecast.model';
import { MonthlyBreakdown } from './ingestion.model';
import { Risk } from './risk.model';

export interface Metrics {
  period: string | null;
  revenue: number | null;
  expenses: number | null;
  profit: number | null;
  margin: number | null;
  cashBalance: number | null;
  runwayMonths: number | null;
  receivables: number | null;
  payables: number | null;
  netCashFlow: number | null;
  gstLiability: number | null;
  outputGst: number | null;
  inputGst: number | null;
  monthlyPeopleCost: number | null;
}

export interface CashFlowPoint {
  month: string;
  cashBalance: number | null;
  netChange: number | null;
}

/**
 * KPIs derived from the manually-entered Finance forms (Position, Profitability,
 * Budget & Forecast, Capital) - a separate authoritative source from `Metrics`
 * above (which comes from the transaction upload), never merged into it. Every
 * field is null when its source subcategory has no record at all yet.
 */
export interface FinanceKpis {
  cashPosition: number | null;
  workingCapital: number | null;
  currentRatio: number | null;
  grossProfit: number | null;
  grossMargin: number | null;
  operatingProfit: number | null;
  netProfit: number | null;
  netMargin: number | null;
  fundingRequirement: number | null;
  debtExposure: number | null;
}

export interface DashboardState {
  tenantName: string;
  hasData: boolean;
  metrics: Metrics;
  monthlyComparison: MonthlyComparison | null;
  trends: Trend[];
  chartSeries: MonthlyBreakdown[];
  risks: Risk[];
  forecast: Forecast | null;
  cashFlowSeries: CashFlowPoint[];
  financeKpis: FinanceKpis;
}
