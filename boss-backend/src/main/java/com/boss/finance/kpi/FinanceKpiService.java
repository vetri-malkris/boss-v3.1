package com.boss.finance.kpi;

import com.boss.common.access.Subcategory;
import com.boss.dataentry.domain.SubcategoryDataRecord;
import com.boss.dataentry.repository.SubcategoryDataRecordRepository;
import com.boss.finance.kpi.dto.FinanceKpiDto;
import com.boss.financial.calculator.MarginCalculator;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Turns the manually-entered Finance subcategory forms (Position, Profitability,
 * Budget & Forecast, Capital) into real KPIs - these forms previously saved into
 * the generic {@link SubcategoryDataRecord} store and fed nothing. Reads the
 * latest (most recently entered, non-voided) record per subcategory, the same
 * "current state" convention {@code ReceivableService}/{@code WorkforceService}
 * already use - not a historical trend engine, which is
 * {@code MonthlyFinancialSnapshot}'s job over a different authoritative source.
 *
 * Current Assets/Current Liabilities for Working Capital and Current Ratio are
 * the one judgment call here: Position has no current/non-current split for
 * liabilities, so Current Liabilities is taken as accounts payable + other
 * liabilities in full. This is an accounting-policy assumption, not a fact the
 * form guarantees - flagged here rather than silently baked in.
 */
@Service
public class FinanceKpiService {

    private final SubcategoryDataRecordRepository repository;
    private final MarginCalculator marginCalculator;

    public FinanceKpiService(SubcategoryDataRecordRepository repository, MarginCalculator marginCalculator) {
        this.repository = repository;
        this.marginCalculator = marginCalculator;
    }

    public FinanceKpiDto summarize(UUID tenantId) {
        Optional<SubcategoryDataRecord> position = latest(tenantId, Subcategory.POSITION);
        Optional<SubcategoryDataRecord> profitability = latest(tenantId, Subcategory.PROFITABILITY);
        Optional<SubcategoryDataRecord> forecast = latest(tenantId, Subcategory.FORECAST);

        BigDecimal cashPosition = position.map(r -> number(r, "cashAndBankBalance")).orElse(null);
        BigDecimal workingCapital = null;
        BigDecimal currentRatio = null;
        if (position.isPresent()) {
            BigDecimal currentAssets = number(position.get(), "cashAndBankBalance")
                    .add(number(position.get(), "accountsReceivable"))
                    .add(number(position.get(), "inventoryValue"));
            BigDecimal currentLiabilities = number(position.get(), "accountsPayable")
                    .add(number(position.get(), "otherLiabilities"));
            workingCapital = currentAssets.subtract(currentLiabilities);
            currentRatio = currentLiabilities.signum() == 0 ? null
                    : currentAssets.divide(currentLiabilities, 2, RoundingMode.HALF_UP);
        }

        BigDecimal grossProfit = null;
        BigDecimal grossMargin = null;
        BigDecimal operatingProfit = null;
        BigDecimal netProfit = null;
        BigDecimal netMargin = null;
        if (profitability.isPresent()) {
            BigDecimal revenue = number(profitability.get(), "revenue");
            BigDecimal cogs = number(profitability.get(), "cogs");
            BigDecimal operatingExpenses = number(profitability.get(), "operatingExpenses");
            BigDecimal financeCost = number(profitability.get(), "financeCost");
            BigDecimal tax = number(profitability.get(), "tax");

            grossProfit = revenue.subtract(cogs);
            grossMargin = marginCalculator.calculate(grossProfit, revenue);
            operatingProfit = grossProfit.subtract(operatingExpenses);
            netProfit = operatingProfit.subtract(financeCost).subtract(tax);
            netMargin = marginCalculator.calculate(netProfit, revenue);
        }

        BigDecimal fundingRequirement = forecast.map(r -> number(r, "fundingRequirement")).orElse(null);

        List<SubcategoryDataRecord> capitalRecords = repository
                .findByTenantIdAndSubcategoryAndVoidedFalseOrderByCreatedAtDesc(tenantId, Subcategory.CAPITAL);
        BigDecimal debtExposure = capitalRecords.isEmpty() ? null
                : capitalRecords.stream()
                        .map(r -> number(r, "outstandingDebt"))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new FinanceKpiDto(cashPosition, workingCapital, currentRatio, grossProfit, grossMargin,
                operatingProfit, netProfit, netMargin, fundingRequirement, debtExposure);
    }

    /** Position's own current balance - also the source for Overview's "Receivables"
     * metric (see {@code DashboardService}), not a separate AR ledger. */
    public BigDecimal receivables(UUID tenantId) {
        return latest(tenantId, Subcategory.POSITION).map(r -> number(r, "accountsReceivable")).orElse(null);
    }

    /** Position's own current balance - also the source for Overview's "Payables"
     * metric (see {@code DashboardService}), not a separate AP ledger. */
    public BigDecimal payables(UUID tenantId) {
        return latest(tenantId, Subcategory.POSITION).map(r -> number(r, "accountsPayable")).orElse(null);
    }

    private Optional<SubcategoryDataRecord> latest(UUID tenantId, Subcategory subcategory) {
        return repository.findByTenantIdAndSubcategoryAndVoidedFalseOrderByCreatedAtDesc(tenantId, subcategory)
                .stream().findFirst();
    }

    /**
     * A NUMBER field is a {@link BigDecimal} only immediately after
     * {@code SchemaValidator} coerces it in-memory - once persisted to the jsonb
     * column and read back, Hibernate/Jackson deserializes a generic
     * {@code Map<String, Object>}'s numbers as {@code Integer}/{@code Long}/
     * {@code Double}, not {@code BigDecimal}. Converting via {@code toString()}
     * handles every numeric subtype correctly regardless of which one came back.
     */
    private BigDecimal number(SubcategoryDataRecord record, String field) {
        Map<String, Object> fields = record.getFields();
        Object value = fields.get(field);
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return new BigDecimal(number.toString());
        }
        return BigDecimal.ZERO;
    }
}
