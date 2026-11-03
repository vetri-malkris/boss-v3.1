import { Component, computed, inject, input } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { CategoryService } from '../../../../core/services/category.service';
import { DataEntryService } from '../../../../core/services/data-entry.service';
import { DashboardState } from '../../../../shared/models/dashboard.model';
import { SubcategoryCode } from '../../../../shared/models/access.model';
import { MetricCards } from '../metric-cards/metric-cards';
import { FinanceKpiPanel } from '../finance-kpi-panel/finance-kpi-panel';
import { FinancialChart } from '../financial-chart/financial-chart';
import { CashFlowChart } from '../cash-flow-chart/cash-flow-chart';
import { MonthlyComparisonPanel } from '../monthly-comparison/monthly-comparison';
import { RiskPanel } from '../risk-panel/risk-panel';
import { ForecastPanel } from '../forecast-panel/forecast-panel';
import { ScenarioPanel } from '../scenario-panel/scenario-panel';
import { RecommendationPanel } from '../recommendation-panel/recommendation-panel';
import { ActionPanel } from '../action-panel/action-panel';
import { OutcomePanel } from '../outcome-panel/outcome-panel';
import { DataEntryPanel } from '../../../../shared/components/data-entry-panel/data-entry-panel';
import { SnapshotRevisionsPanel } from '../snapshot-revisions-panel/snapshot-revisions-panel';

type PanelKey =
  | 'metrics' | 'chart' | 'comparison' | 'risk' | 'forecast' | 'scenario'
  | 'recommendation' | 'action' | 'outcome' | 'financeKpis';

/**
 * What each subcategory's OWN page shows when it's the one directly selected.
 * Finance's Position/Liquidity/Profitability/Budget & Forecast/Cost each now have
 * their own real manual data-entry form (rendered separately via dataEntrySchema
 * below) instead of a computed panel here - only Risk's manual-register
 * subcategories still show a computed panel *alongside* their form, which is
 * intentional (the register and the real auto-detected risk panel are meant to sit
 * together). See OVERVIEW_CONTRIBUTIONS for what those computed panels still
 * contribute to Overview's "everything this user can see" union.
 */
const PANELS_BY_SUBCATEGORY: Partial<Record<SubcategoryCode, PanelKey[]>> = {
  RISK_EXPOSURE: ['risk'],
  RISK_FINANCIAL: ['risk'],
  RISK_MITIGATION: ['risk', 'recommendation', 'action', 'outcome'],
};

/**
 * Which computed panels each subcategory implies access to, for Overview's union
 * only. Finance's subcategories contribute their computed panels here even though
 * their own page no longer shows them directly (see PANELS_BY_SUBCATEGORY) - a
 * user who only has Position access should still see real business status on
 * Overview, just not duplicated onto Position's own now-form-only page.
 */
const OVERVIEW_CONTRIBUTIONS: Partial<Record<SubcategoryCode, PanelKey[]>> = {
  POSITION: ['metrics', 'chart', 'comparison', 'financeKpis', 'risk'],
  LIQUIDITY: ['metrics', 'forecast'],
  PROFITABILITY: ['metrics', 'chart', 'financeKpis', 'risk'],
  FORECAST: ['forecast', 'scenario', 'financeKpis'],
  COST: ['metrics', 'chart'],
  CAPITAL: ['financeKpis'],
  ...PANELS_BY_SUBCATEGORY,
};

/** Rendering order for "Overview" (no subcategory picked) - a fixed order, not
 * whatever order a Set iterates the user's granted subcategories in. */
const PANEL_ORDER: PanelKey[] = [
  'metrics', 'chart', 'comparison', 'financeKpis', 'risk', 'forecast', 'scenario', 'recommendation', 'action', 'outcome',
];

@Component({
  selector: 'app-category-content',
  imports: [
    MatButtonModule, MatCardModule, MatIconModule, MetricCards, FinanceKpiPanel, FinancialChart,
    CashFlowChart, MonthlyComparisonPanel, RiskPanel, ForecastPanel, ScenarioPanel, RecommendationPanel, ActionPanel,
    OutcomePanel, DataEntryPanel, SnapshotRevisionsPanel,
  ],
  templateUrl: './category-content.html',
  styleUrl: './category-content.css',
})
export class CategoryContent {
  private readonly categoryService = inject(CategoryService);
  private readonly dataEntryService = inject(DataEntryService);

  readonly subcategory = input.required<SubcategoryCode | null>();
  readonly dashboard = input.required<DashboardState>();

  constructor() {
    this.dataEntryService.loadSchemas().subscribe();
  }

  /** Overview never shows a data-entry form - only a specific subcategory does. */
  readonly dataEntrySchema = computed(() => {
    const code = this.subcategory();
    return code ? this.dataEntryService.schemasBySubcategory().get(code) ?? null : null;
  });

  /**
   * null = "Overview": every panel behind every subcategory this user actually has
   * access to, unioned together (never more than that - a limited user's Overview
   * only shows what their granted subcategories would individually show).
   */
  readonly panels = computed<PanelKey[]>(() => {
    const code = this.subcategory();
    if (code === null) {
      const granted = new Set<PanelKey>();
      for (const sub of this.categoryService.visibleSubcategoryCodes()) {
        for (const panel of OVERVIEW_CONTRIBUTIONS[sub] ?? []) {
          granted.add(panel);
        }
      }
      return PANEL_ORDER.filter((panel) => granted.has(panel));
    }
    return PANELS_BY_SUBCATEGORY[code] ?? [];
  });

  readonly subcategoryLabel = computed(() => {
    const code = this.subcategory();
    if (!code) {
      return 'Overview';
    }
    for (const category of this.categoryService.taxonomy()) {
      const match = category.subcategories.find((sub) => sub.code === code);
      if (match) {
        return `${category.label} → ${match.label}`;
      }
    }
    return code;
  });

  has(key: PanelKey): boolean {
    return this.panels().includes(key);
  }
}
