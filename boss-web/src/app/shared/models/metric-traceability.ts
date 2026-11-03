import { SubcategoryCode } from './access.model';

/** One Overview metric's provenance - transcribed directly from this session's
 * verified traceability (every source/formula here was confirmed against the live
 * backend, not guessed). `subcategory` is the one place to jump to on "Go to" -
 * omitted for a field with no single source (e.g. Runway draws on three). */
export interface MetricTraceability {
  label: string;
  source: string;
  fields: string;
  formula: string;
  subcategory?: SubcategoryCode;
  subcategoryLabel?: string;
}

export const METRIC_TRACEABILITY: Record<string, MetricTraceability> = {
  revenue: {
    label: 'Revenue',
    source: 'Finance → Profitability',
    fields: 'revenue',
    formula: 'Latest period’s value, direct.',
    subcategory: 'PROFITABILITY',
    subcategoryLabel: 'Profitability',
  },
  expenses: {
    label: 'Expense',
    source: 'Finance → Profitability',
    fields: 'cogs, operatingExpenses, financeCost, tax',
    formula: 'Sum of all four fields, latest period.',
    subcategory: 'PROFITABILITY',
    subcategoryLabel: 'Profitability',
  },
  profit: {
    label: 'Profit / Loss',
    source: 'Derived from Finance → Profitability',
    fields: 'revenue, cogs, operatingExpenses, financeCost, tax',
    formula: 'Revenue − Expenses.',
    subcategory: 'PROFITABILITY',
    subcategoryLabel: 'Profitability',
  },
  margin: {
    label: 'Margin',
    source: 'Derived from Finance → Profitability',
    fields: 'revenue, cogs, operatingExpenses, financeCost, tax',
    formula: 'Profit ÷ Revenue × 100. Shown as 0, not an error, when revenue is 0.',
    subcategory: 'PROFITABILITY',
    subcategoryLabel: 'Profitability',
  },
  cashBalance: {
    label: 'Cash',
    source: 'Finance → Position',
    fields: 'cashAndBankBalance',
    formula: 'Latest record, by its as-of date’s month.',
    subcategory: 'POSITION',
    subcategoryLabel: 'Position',
  },
  netCashFlow: {
    label: 'Net cash flow',
    source: 'Finance → Position',
    fields: 'cashAndBankBalance',
    formula: 'This period’s cash − the previous period’s cash. Needs two consecutive periods with cash entered.',
    subcategory: 'POSITION',
    subcategoryLabel: 'Position',
  },
  runwayMonths: {
    label: 'Runway',
    source: 'Finance → Position + Profitability; HR → People Cost',
    fields: 'cashAndBankBalance; cogs+operatingExpenses+financeCost+tax − revenue; People Cost total',
    formula: 'Cash ÷ [(Expenses − Revenue) + People Cost]. Null (never shown as NaN or Infinity) when there’s no cash yet or burn is zero/negative. Recomputed whenever any of the three inputs changes, in any order.',
    subcategory: 'POSITION',
    subcategoryLabel: 'Position',
  },
  receivables: {
    label: 'Receivables',
    source: 'Finance → Position',
    fields: 'accountsReceivable',
    formula: 'Latest record, direct — the sole AR source.',
    subcategory: 'POSITION',
    subcategoryLabel: 'Position',
  },
  payables: {
    label: 'Payables',
    source: 'Finance → Position',
    fields: 'accountsPayable',
    formula: 'Latest record, direct — the sole AP source.',
    subcategory: 'POSITION',
    subcategoryLabel: 'Position',
  },
  outputGst: {
    label: 'Output GST',
    source: 'Compliance → Tax-GST',
    fields: 'outputGst',
    formula: 'Latest record by filing period, direct.',
    subcategory: 'TAX_GST',
    subcategoryLabel: 'Tax-GST',
  },
  inputGst: {
    label: 'Input GST (ITC)',
    source: 'Compliance → Tax-GST',
    fields: 'inputGst',
    formula: 'Latest record by filing period, direct.',
    subcategory: 'TAX_GST',
    subcategoryLabel: 'Tax-GST',
  },
  gstLiability: {
    label: 'GST liability',
    source: 'Compliance → Tax-GST',
    fields: 'outputGst, inputGst',
    formula: 'max(0, Output − Input). A credit position (Input > Output) floors at 0, never shown as a negative liability.',
    subcategory: 'TAX_GST',
    subcategoryLabel: 'Tax-GST',
  },
  monthlyPeopleCost: {
    label: 'People cost',
    source: 'HR → People Cost',
    fields: 'grossSalaryCost, benefitsCost, payrollTaxCost',
    formula: 'Sum of all three fields, across every record sharing the latest period (covers multiple departments).',
    subcategory: 'PEOPLE_COST',
    subcategoryLabel: 'People Cost',
  },
  cashPosition: {
    label: 'Cash position',
    source: 'Finance → Position',
    fields: 'cashAndBankBalance',
    formula: 'Latest record, direct.',
    subcategory: 'POSITION',
    subcategoryLabel: 'Position',
  },
  workingCapital: {
    label: 'Working capital',
    source: 'Finance → Position',
    fields: 'cashAndBankBalance, accountsReceivable, inventoryValue, accountsPayable, otherLiabilities',
    formula: '(Cash + AR + Inventory) − (AP + Other liabilities).',
    subcategory: 'POSITION',
    subcategoryLabel: 'Position',
  },
  currentRatio: {
    label: 'Current ratio',
    source: 'Finance → Position',
    fields: 'cashAndBankBalance, accountsReceivable, inventoryValue, accountsPayable, otherLiabilities',
    formula: '(Cash + AR + Inventory) ÷ (AP + Other liabilities). Null, not a divide-by-zero, when liabilities are 0.',
    subcategory: 'POSITION',
    subcategoryLabel: 'Position',
  },
  grossProfit: {
    label: 'Gross profit',
    source: 'Finance → Profitability',
    fields: 'revenue, cogs',
    formula: 'Revenue − COGS.',
    subcategory: 'PROFITABILITY',
    subcategoryLabel: 'Profitability',
  },
  grossMargin: {
    label: 'Gross margin',
    source: 'Finance → Profitability',
    fields: 'revenue, cogs',
    formula: 'Gross profit ÷ Revenue × 100.',
    subcategory: 'PROFITABILITY',
    subcategoryLabel: 'Profitability',
  },
  operatingProfit: {
    label: 'Operating profit',
    source: 'Finance → Profitability',
    fields: 'revenue, cogs, operatingExpenses',
    formula: 'Gross profit − Operating expenses.',
    subcategory: 'PROFITABILITY',
    subcategoryLabel: 'Profitability',
  },
  netProfit: {
    label: 'Net profit',
    source: 'Finance → Profitability',
    fields: 'revenue, cogs, operatingExpenses, financeCost, tax',
    formula: 'Operating profit − Finance cost − Tax.',
    subcategory: 'PROFITABILITY',
    subcategoryLabel: 'Profitability',
  },
  netMargin: {
    label: 'Net margin',
    source: 'Finance → Profitability',
    fields: 'revenue, cogs, operatingExpenses, financeCost, tax',
    formula: 'Net profit ÷ Revenue × 100.',
    subcategory: 'PROFITABILITY',
    subcategoryLabel: 'Profitability',
  },
  fundingRequirement: {
    label: 'Funding requirement',
    source: 'Finance → Budget & Forecast',
    fields: 'fundingRequirement',
    formula: 'Latest record, direct.',
    subcategory: 'FORECAST',
    subcategoryLabel: 'Budget & Forecast',
  },
  debtExposure: {
    label: 'Debt exposure',
    source: 'Finance → Capital',
    fields: 'outstandingDebt',
    formula: 'Sum of outstandingDebt across ALL Capital records — not just the latest.',
    subcategory: 'CAPITAL',
    subcategoryLabel: 'Capital',
  },
};
