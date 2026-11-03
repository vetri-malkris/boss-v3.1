/** Mirrors com.boss.common.access.Subcategory exactly - the backend is the source of truth. */
export type SubcategoryCode =
  | 'WORKFORCE' | 'PEOPLE_COST' | 'PRODUCTIVITY' | 'HIRING' | 'ATTRITION' | 'CAPABILITY'
  | 'POSITION' | 'LIQUIDITY' | 'PROFITABILITY' | 'FORECAST' | 'COST' | 'CAPITAL'
  | 'REPORTING' | 'WORKING_CAPITAL' | 'ASSETS_LIABILITIES' | 'RECONCILIATION' | 'ACCOUNTING_CONTROLS'
  | 'AUDIT_STATUS' | 'FINDINGS' | 'AUDIT_CONTROLS' | 'AUDIT_EXCEPTIONS' | 'AUDIT_ACTIONS'
  | 'COMPLIANCE_STATUS' | 'TAX_GST' | 'REGULATORY' | 'FILINGS' | 'COMPLIANCE_EXCEPTIONS'
  | 'RISK_EXPOSURE' | 'RISK_FINANCIAL' | 'RISK_OPERATIONAL' | 'RISK_COMPLIANCE' | 'RISK_TECHNOLOGY' | 'RISK_MITIGATION';

export type CategoryCode = 'HUMAN_RESOURCE' | 'FINANCE' | 'ACCOUNTING' | 'AUDITING' | 'COMPLIANCE' | 'RISK';

export interface SubcategoryTaxonomy {
  code: SubcategoryCode;
  label: string;
}

export interface CategoryTaxonomy {
  code: CategoryCode;
  label: string;
  subcategories: SubcategoryTaxonomy[];
}
