package com.boss.common.access;

/**
 * The fixed sidebar subcategories, each belonging to exactly one {@link Category}.
 * A user's menu access is stored as a set of these (see {@code User.enabledSubcategories}) -
 * a category has no separate "enabled" flag of its own, so "category on with zero
 * subcategories" is not a representable state: the UI derives category visibility from
 * "does this user have at least one of its subcategories".
 */
public enum Subcategory {
    // HUMAN_RESOURCE
    WORKFORCE(Category.HUMAN_RESOURCE),
    PEOPLE_COST(Category.HUMAN_RESOURCE),
    PRODUCTIVITY(Category.HUMAN_RESOURCE),
    HIRING(Category.HUMAN_RESOURCE),
    ATTRITION(Category.HUMAN_RESOURCE),
    CAPABILITY(Category.HUMAN_RESOURCE),

    // FINANCE
    POSITION(Category.FINANCE),
    LIQUIDITY(Category.FINANCE),
    PROFITABILITY(Category.FINANCE),
    FORECAST(Category.FINANCE),
    COST(Category.FINANCE),
    CAPITAL(Category.FINANCE),

    // ACCOUNTING
    REPORTING(Category.ACCOUNTING),
    WORKING_CAPITAL(Category.ACCOUNTING),
    ASSETS_LIABILITIES(Category.ACCOUNTING),
    RECONCILIATION(Category.ACCOUNTING),
    ACCOUNTING_CONTROLS(Category.ACCOUNTING),

    // AUDITING
    AUDIT_STATUS(Category.AUDITING),
    FINDINGS(Category.AUDITING),
    AUDIT_CONTROLS(Category.AUDITING),
    AUDIT_EXCEPTIONS(Category.AUDITING),
    AUDIT_ACTIONS(Category.AUDITING),

    // COMPLIANCE
    COMPLIANCE_STATUS(Category.COMPLIANCE),
    TAX_GST(Category.COMPLIANCE),
    REGULATORY(Category.COMPLIANCE),
    FILINGS(Category.COMPLIANCE),
    COMPLIANCE_EXCEPTIONS(Category.COMPLIANCE),

    // RISK
    RISK_EXPOSURE(Category.RISK),
    RISK_FINANCIAL(Category.RISK),
    RISK_OPERATIONAL(Category.RISK),
    RISK_COMPLIANCE(Category.RISK),
    RISK_TECHNOLOGY(Category.RISK),
    RISK_MITIGATION(Category.RISK);

    private final Category category;

    Subcategory(Category category) {
        this.category = category;
    }

    public Category category() {
        return category;
    }

    public String label() {
        return switch (this) {
            case WORKFORCE -> "Workforce";
            case PEOPLE_COST -> "People Cost";
            case PRODUCTIVITY -> "Productivity";
            case HIRING -> "Hiring";
            case ATTRITION -> "Attrition";
            case CAPABILITY -> "Capability";

            case POSITION -> "Position";
            case LIQUIDITY -> "Liquidity";
            case PROFITABILITY -> "Profitability";
            case FORECAST -> "Budget & Forecast";
            case COST -> "Cost";
            case CAPITAL -> "Capital";

            case REPORTING -> "Reporting";
            case WORKING_CAPITAL -> "Working Capital";
            case ASSETS_LIABILITIES -> "Assets/Liabilities";
            case RECONCILIATION -> "Reconciliation";
            case ACCOUNTING_CONTROLS -> "Controls";

            case AUDIT_STATUS -> "Audit Status";
            case FINDINGS -> "Findings";
            case AUDIT_CONTROLS -> "Controls";
            case AUDIT_EXCEPTIONS -> "Exceptions";
            case AUDIT_ACTIONS -> "Actions";

            case COMPLIANCE_STATUS -> "Compliance Status";
            case TAX_GST -> "Tax/GST";
            case REGULATORY -> "Regulatory";
            case FILINGS -> "Filings";
            case COMPLIANCE_EXCEPTIONS -> "Exceptions";

            case RISK_EXPOSURE -> "Exposure";
            case RISK_FINANCIAL -> "Financial";
            case RISK_OPERATIONAL -> "Operational";
            case RISK_COMPLIANCE -> "Compliance";
            case RISK_TECHNOLOGY -> "Technology";
            case RISK_MITIGATION -> "Mitigation";
        };
    }
}
