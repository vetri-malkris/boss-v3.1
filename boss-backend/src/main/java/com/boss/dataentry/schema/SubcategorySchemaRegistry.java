package com.boss.dataentry.schema;

import com.boss.common.access.Subcategory;
import com.boss.dataentry.domain.FieldDefinition;
import com.boss.dataentry.domain.SubcategorySchema;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The single source of truth for which subcategories have a real data-entry form and
 * what fields each one captures - genuinely distinct per subcategory (a Hiring record
 * has real hiring fields, a Tax/GST record has real tax fields), not a generic
 * label/value blob. New subcategories are added here as their forms are built; a
 * subcategory with no schema here still shows "not built yet" in the UI.
 */
@Component
public class SubcategorySchemaRegistry {

    private final Map<Subcategory, SubcategorySchema> schemas = new EnumMap<>(Subcategory.class);

    public SubcategorySchemaRegistry() {
        register(Subcategory.WORKFORCE, List.of(
                FieldDefinition.date("recordDate", "Record date", true),
                FieldDefinition.text("department", "Department", true),
                FieldDefinition.text("location", "Location", false),
                FieldDefinition.number("activeHeadcount", "Active headcount", true),
                FieldDefinition.number("fullTimeCount", "Full-time count", false),
                FieldDefinition.number("partTimeCount", "Part-time count", false),
                FieldDefinition.number("contractorCount", "Contractor count", false)));

        register(Subcategory.PEOPLE_COST, List.of(
                FieldDefinition.text("period", "Period (e.g. 2026-03)", true),
                FieldDefinition.text("department", "Department", true),
                FieldDefinition.number("grossSalaryCost", "Gross salary cost", true),
                FieldDefinition.number("benefitsCost", "Benefits cost", false),
                FieldDefinition.number("payrollTaxCost", "Payroll tax cost", false)));

        register(Subcategory.PRODUCTIVITY, List.of(
                FieldDefinition.text("period", "Period (e.g. 2026-03)", true),
                FieldDefinition.text("department", "Department", true),
                FieldDefinition.text("outputMetricName", "Output metric name", true),
                FieldDefinition.number("outputValue", "Output value", true),
                FieldDefinition.number("fteCount", "FTE count", true)));

        register(Subcategory.HIRING, List.of(
                FieldDefinition.text("requisitionId", "Requisition ID", true),
                FieldDefinition.text("positionTitle", "Position title", true),
                FieldDefinition.text("department", "Department", true),
                FieldDefinition.enumField("status", "Status", true, "OPEN", "INTERVIEWING", "OFFER", "FILLED", "CANCELLED"),
                FieldDefinition.date("openedDate", "Opened date", true),
                FieldDefinition.date("targetFillDate", "Target fill date", false),
                FieldDefinition.date("filledDate", "Filled date", false)));

        register(Subcategory.ATTRITION, List.of(
                FieldDefinition.text("employeeName", "Employee name/ID", true),
                FieldDefinition.text("department", "Department", true),
                FieldDefinition.date("separationDate", "Separation date", true),
                FieldDefinition.enumField("separationType", "Separation type", true, "VOLUNTARY", "INVOLUNTARY", "RETIREMENT"),
                FieldDefinition.text("reason", "Reason", false),
                FieldDefinition.number("tenureMonths", "Tenure (months)", false)));

        register(Subcategory.CAPABILITY, List.of(
                FieldDefinition.text("employeeName", "Employee name/ID", true),
                FieldDefinition.text("department", "Department", true),
                FieldDefinition.text("skillName", "Skill / certification name", true),
                FieldDefinition.enumField("status", "Status", true, "NOT_STARTED", "IN_PROGRESS", "COMPLETED"),
                FieldDefinition.date("completionDate", "Completion date", false),
                FieldDefinition.number("trainingCost", "Training cost", false)));

        // FINANCE - a manual entry per subcategory, shown alongside (not instead of)
        // the real transaction-derived metrics/chart/forecast panels those subcategories
        // already have, the same way Risk's manual register sits alongside its real
        // auto-detected risk panel.
        register(Subcategory.POSITION, List.of(
                FieldDefinition.date("asOfDate", "As-of date", true),
                FieldDefinition.number("cashAndBankBalance", "Cash & bank balance", true),
                FieldDefinition.number("accountsReceivable", "Accounts receivable", true),
                FieldDefinition.number("inventoryValue", "Inventory value", true),
                FieldDefinition.number("fixedAssets", "Fixed assets", true),
                FieldDefinition.number("accountsPayable", "Accounts payable", true),
                FieldDefinition.number("otherLiabilities", "Other liabilities", true),
                FieldDefinition.number("equity", "Equity", true)));

        register(Subcategory.LIQUIDITY, List.of(
                FieldDefinition.number("expectedReceipt", "Expected receipt", true),
                FieldDefinition.date("expectedReceiptDate", "Expected receipt date", true),
                FieldDefinition.number("expectedPayment", "Expected payment", true),
                FieldDefinition.date("expectedPaymentDate", "Expected payment date", true),
                FieldDefinition.text("majorObligation", "Major obligation", true),
                FieldDefinition.date("obligationDueDate", "Obligation due date", true),
                FieldDefinition.number("availableCredit", "Available credit", true)));

        register(Subcategory.PROFITABILITY, List.of(
                FieldDefinition.text("period", "Period (e.g. 2026-03)", true),
                FieldDefinition.number("revenue", "Revenue", true),
                FieldDefinition.number("cogs", "COGS (Cost of Goods Sold)", true),
                FieldDefinition.number("operatingExpenses", "Operating expenses", true),
                FieldDefinition.number("financeCost", "Finance cost", true),
                FieldDefinition.number("tax", "Tax", true)));

        register(Subcategory.FORECAST, List.of(
                FieldDefinition.text("period", "Period (e.g. 2026-03)", true),
                FieldDefinition.number("budgetRevenue", "Budget revenue", true),
                FieldDefinition.number("budgetCost", "Budget cost", true),
                FieldDefinition.number("forecastRevenue", "Forecast revenue", true),
                FieldDefinition.number("forecastCost", "Forecast cost", true),
                FieldDefinition.number("expectedCollection", "Expected collection", true),
                FieldDefinition.number("expectedPayment", "Expected payment", true),
                FieldDefinition.number("plannedCapex", "Planned capex (capital expenditure)", true),
                FieldDefinition.number("fundingRequirement", "Funding requirement", true),
                FieldDefinition.text("scenario", "Scenario", true)));

        register(Subcategory.COST, List.of(
                FieldDefinition.text("costHead", "Cost head", true),
                FieldDefinition.text("departmentOrBusinessUnit", "Department / business unit", true),
                FieldDefinition.text("costType", "Cost type", true),
                FieldDefinition.number("actualAmount", "Actual amount", true),
                FieldDefinition.text("budgetReference", "Budget reference", true),
                FieldDefinition.text("varianceReason", "Variance reason", true)));

        register(Subcategory.CAPITAL, List.of(
                FieldDefinition.enumField("capitalType", "Capital type", true, "EQUITY", "DEBT", "GRANT", "OTHER"),
                FieldDefinition.number("amount", "Amount", true),
                FieldDefinition.text("source", "Source", true),
                FieldDefinition.number("outstandingDebt", "Outstanding debt", true),
                FieldDefinition.number("interestRate", "Interest rate", true),
                FieldDefinition.date("maturityDate", "Maturity date", true),
                FieldDefinition.date("requirementDate", "Requirement date", true),
                FieldDefinition.text("purpose", "Purpose", true)));

        // ACCOUNTING
        register(Subcategory.REPORTING, List.of(
                FieldDefinition.text("reportPeriod", "Report period (e.g. 2026-03)", true),
                FieldDefinition.enumField("reportType", "Report type", true, "PL", "BALANCE_SHEET", "CASH_FLOW", "OTHER"),
                FieldDefinition.enumField("status", "Status", true, "DRAFT", "FINALIZED", "FILED"),
                FieldDefinition.text("preparedBy", "Prepared by", false),
                FieldDefinition.date("finalizedDate", "Finalized date", false)));

        register(Subcategory.WORKING_CAPITAL, List.of(
                FieldDefinition.date("asOfDate", "As-of date", true),
                FieldDefinition.number("currentAssets", "Current assets", true),
                FieldDefinition.number("currentLiabilities", "Current liabilities", true),
                FieldDefinition.text("notes", "Notes", false)));

        register(Subcategory.ASSETS_LIABILITIES, List.of(
                FieldDefinition.date("asOfDate", "As-of date", true),
                FieldDefinition.text("itemName", "Item name", true),
                FieldDefinition.enumField("type", "Type", true, "ASSET", "LIABILITY"),
                FieldDefinition.enumField("category", "Category", true,
                        "FIXED_ASSET", "CURRENT_ASSET", "LONG_TERM_LIABILITY", "CURRENT_LIABILITY"),
                FieldDefinition.number("value", "Value", true),
                FieldDefinition.text("notes", "Notes", false)));

        register(Subcategory.RECONCILIATION, List.of(
                FieldDefinition.text("accountName", "Account name", true),
                FieldDefinition.text("period", "Period (e.g. 2026-03)", true),
                FieldDefinition.number("bookBalance", "Book balance", true),
                FieldDefinition.number("statementBalance", "Statement balance", true),
                FieldDefinition.enumField("status", "Status", true, "RECONCILED", "UNRECONCILED", "IN_PROGRESS"),
                FieldDefinition.text("notes", "Notes", false)));

        register(Subcategory.ACCOUNTING_CONTROLS, List.of(
                FieldDefinition.text("controlName", "Control name", true),
                FieldDefinition.text("owner", "Owner", true),
                FieldDefinition.enumField("frequency", "Frequency", true, "DAILY", "WEEKLY", "MONTHLY", "QUARTERLY"),
                FieldDefinition.date("lastPerformedDate", "Last performed date", false),
                FieldDefinition.enumField("status", "Status", true, "EFFECTIVE", "NEEDS_REVIEW", "INEFFECTIVE")));

        // AUDITING
        register(Subcategory.AUDIT_STATUS, List.of(
                FieldDefinition.text("auditName", "Audit name", true),
                FieldDefinition.enumField("auditType", "Audit type", true, "INTERNAL", "EXTERNAL", "REGULATORY"),
                FieldDefinition.text("auditor", "Auditor", false),
                FieldDefinition.date("startDate", "Start date", true),
                FieldDefinition.date("endDate", "End date", false),
                FieldDefinition.enumField("status", "Status", true, "PLANNED", "IN_PROGRESS", "COMPLETED")));

        register(Subcategory.FINDINGS, List.of(
                FieldDefinition.text("auditName", "Audit name", true),
                FieldDefinition.text("findingTitle", "Finding title", true),
                FieldDefinition.enumField("severity", "Severity", true, "LOW", "MEDIUM", "HIGH", "CRITICAL"),
                FieldDefinition.text("description", "Description", false),
                FieldDefinition.date("identifiedDate", "Identified date", true),
                FieldDefinition.enumField("status", "Status", true, "OPEN", "REMEDIATED", "ACCEPTED_RISK")));

        register(Subcategory.AUDIT_CONTROLS, List.of(
                FieldDefinition.text("controlName", "Control name", true),
                FieldDefinition.text("area", "Area", false),
                FieldDefinition.text("owner", "Owner", true),
                FieldDefinition.enumField("testFrequency", "Test frequency", true, "DAILY", "WEEKLY", "MONTHLY", "QUARTERLY"),
                FieldDefinition.date("lastTestDate", "Last test date", false),
                FieldDefinition.enumField("result", "Result", false, "PASS", "FAIL", "PARTIAL")));

        register(Subcategory.AUDIT_EXCEPTIONS, List.of(
                FieldDefinition.text("relatedFinding", "Related finding/control", false),
                FieldDefinition.text("description", "Exception description", true),
                FieldDefinition.date("identifiedDate", "Identified date", true),
                FieldDefinition.enumField("riskLevel", "Risk level", true, "LOW", "MEDIUM", "HIGH"),
                FieldDefinition.enumField("resolutionStatus", "Resolution status", true, "OPEN", "RESOLVED", "ACCEPTED"),
                FieldDefinition.date("resolutionDate", "Resolution date", false)));

        register(Subcategory.AUDIT_ACTIONS, List.of(
                FieldDefinition.text("relatedFinding", "Related finding", false),
                FieldDefinition.text("description", "Description", true),
                FieldDefinition.text("owner", "Owner", true),
                FieldDefinition.date("dueDate", "Due date", true),
                FieldDefinition.enumField("status", "Status", true, "NOT_STARTED", "IN_PROGRESS", "COMPLETED", "OVERDUE"),
                FieldDefinition.date("completedDate", "Completed date", false)));

        // COMPLIANCE
        register(Subcategory.COMPLIANCE_STATUS, List.of(
                FieldDefinition.text("requirementName", "Requirement name", true),
                FieldDefinition.text("regulationFramework", "Regulation / framework", false),
                FieldDefinition.enumField("status", "Status", true, "COMPLIANT", "NON_COMPLIANT", "IN_PROGRESS", "NOT_APPLICABLE"),
                FieldDefinition.date("lastReviewedDate", "Last reviewed date", false),
                FieldDefinition.text("owner", "Owner", false)));

        register(Subcategory.TAX_GST, List.of(
                FieldDefinition.text("filingPeriod", "Filing period (e.g. 2026-03)", true),
                FieldDefinition.enumField("taxType", "Tax type", true, "GST", "VAT", "INCOME_TAX", "OTHER"),
                FieldDefinition.number("outputGst", "Output GST", true),
                FieldDefinition.number("inputGst", "Input GST / ITC", true),
                FieldDefinition.number("amountDue", "Amount due", true),
                FieldDefinition.number("amountPaid", "Amount paid", false),
                FieldDefinition.date("dueDate", "Due date", true),
                FieldDefinition.date("filedDate", "Filed date", false),
                FieldDefinition.enumField("status", "Status", true, "PENDING", "FILED", "PAID", "OVERDUE")));

        register(Subcategory.REGULATORY, List.of(
                FieldDefinition.text("regulationName", "Regulation name", true),
                FieldDefinition.text("jurisdiction", "Jurisdiction", false),
                FieldDefinition.text("requirementDescription", "Requirement description", false),
                FieldDefinition.enumField("status", "Status", true, "COMPLIANT", "PENDING", "NON_COMPLIANT"),
                FieldDefinition.date("reviewDate", "Review date", false),
                FieldDefinition.text("owner", "Owner", false)));

        register(Subcategory.FILINGS, List.of(
                FieldDefinition.text("filingName", "Filing name", true),
                FieldDefinition.text("filingType", "Type", false),
                FieldDefinition.text("regulatoryBody", "Regulatory body", false),
                FieldDefinition.date("dueDate", "Due date", true),
                FieldDefinition.date("filedDate", "Filed date", false),
                FieldDefinition.enumField("status", "Status", true, "PENDING", "FILED", "LATE"),
                FieldDefinition.text("referenceNumber", "Reference number", false)));

        register(Subcategory.COMPLIANCE_EXCEPTIONS, List.of(
                FieldDefinition.text("relatedRequirement", "Related requirement", false),
                FieldDefinition.text("description", "Description", true),
                FieldDefinition.enumField("riskLevel", "Risk level", true, "LOW", "MEDIUM", "HIGH"),
                FieldDefinition.date("identifiedDate", "Identified date", true),
                FieldDefinition.enumField("status", "Status", true, "OPEN", "RESOLVED", "ACCEPTED")));

        // RISK - a manual risk register per subcategory. Mitigation is deliberately
        // excluded: it's already the real risk -> recommendation -> action -> outcome
        // workflow (RiskEngine + RecommendationEngine), not a manual entry form.
        register(Subcategory.RISK_EXPOSURE, List.of(
                FieldDefinition.text("riskTitle", "Risk title", true),
                FieldDefinition.enumField("category", "Category", true, "MARKET", "CREDIT", "LIQUIDITY", "STRATEGIC", "OTHER"),
                FieldDefinition.text("description", "Description", false),
                FieldDefinition.enumField("likelihood", "Likelihood", true, "LOW", "MEDIUM", "HIGH"),
                FieldDefinition.enumField("impact", "Impact", true, "LOW", "MEDIUM", "HIGH"),
                FieldDefinition.text("owner", "Owner", false),
                FieldDefinition.date("identifiedDate", "Identified date", true)));

        register(Subcategory.RISK_FINANCIAL, List.of(
                FieldDefinition.text("riskTitle", "Risk title", true),
                FieldDefinition.enumField("financialArea", "Financial area", true, "CASH_FLOW", "CREDIT", "MARKET", "CAPITAL", "OTHER"),
                FieldDefinition.text("description", "Description", false),
                FieldDefinition.enumField("likelihood", "Likelihood", true, "LOW", "MEDIUM", "HIGH"),
                FieldDefinition.enumField("impact", "Impact", true, "LOW", "MEDIUM", "HIGH"),
                FieldDefinition.text("owner", "Owner", false),
                FieldDefinition.date("identifiedDate", "Identified date", true)));

        register(Subcategory.RISK_OPERATIONAL, List.of(
                FieldDefinition.text("riskTitle", "Risk title", true),
                FieldDefinition.text("processArea", "Process / area", false),
                FieldDefinition.text("description", "Description", false),
                FieldDefinition.enumField("likelihood", "Likelihood", true, "LOW", "MEDIUM", "HIGH"),
                FieldDefinition.enumField("impact", "Impact", true, "LOW", "MEDIUM", "HIGH"),
                FieldDefinition.text("owner", "Owner", false),
                FieldDefinition.date("identifiedDate", "Identified date", true)));

        register(Subcategory.RISK_COMPLIANCE, List.of(
                FieldDefinition.text("riskTitle", "Risk title", true),
                FieldDefinition.text("relatedRegulation", "Related regulation", false),
                FieldDefinition.text("description", "Description", false),
                FieldDefinition.enumField("likelihood", "Likelihood", true, "LOW", "MEDIUM", "HIGH"),
                FieldDefinition.enumField("impact", "Impact", true, "LOW", "MEDIUM", "HIGH"),
                FieldDefinition.text("owner", "Owner", false),
                FieldDefinition.date("identifiedDate", "Identified date", true)));

        register(Subcategory.RISK_TECHNOLOGY, List.of(
                FieldDefinition.text("riskTitle", "Risk title", true),
                FieldDefinition.text("systemAsset", "System / asset", false),
                FieldDefinition.text("description", "Description", false),
                FieldDefinition.enumField("likelihood", "Likelihood", true, "LOW", "MEDIUM", "HIGH"),
                FieldDefinition.enumField("impact", "Impact", true, "LOW", "MEDIUM", "HIGH"),
                FieldDefinition.text("owner", "Owner", false),
                FieldDefinition.date("identifiedDate", "Identified date", true)));
    }

    private void register(Subcategory subcategory, List<FieldDefinition> fields) {
        schemas.put(subcategory, new SubcategorySchema(subcategory, fields));
    }

    public Optional<SubcategorySchema> find(Subcategory subcategory) {
        return Optional.ofNullable(schemas.get(subcategory));
    }

    public SubcategorySchema require(Subcategory subcategory) {
        return find(subcategory)
                .orElseThrow(() -> new IllegalArgumentException("No data-entry schema defined for " + subcategory));
    }

    public boolean isDefined(Subcategory subcategory) {
        return schemas.containsKey(subcategory);
    }

    public List<SubcategorySchema> all() {
        return List.copyOf(schemas.values());
    }
}
