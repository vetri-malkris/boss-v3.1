package com.boss.financial.calculator;

import com.boss.financial.domain.MonthlyFinancialSnapshot;
import com.boss.financial.dto.CashFlowPointDto;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Net cash flow between consecutive months, derived only from the cash balance
 * actually recorded at upload time - never from revenue/expenses, which are
 * accrual figures this system does not reconcile to cash (section: never
 * fabricate certainty). A month's net change is null whenever that month or the
 * one before it has no recorded balance.
 */
@Component
public class CashFlowCalculator {

    public List<CashFlowPointDto> calculate(List<MonthlyFinancialSnapshot> history) {
        List<CashFlowPointDto> points = new ArrayList<>();
        BigDecimal previous = null;
        for (MonthlyFinancialSnapshot snapshot : history) {
            BigDecimal balance = snapshot.getCashBalance();
            BigDecimal netChange = (balance != null && previous != null) ? balance.subtract(previous) : null;
            points.add(new CashFlowPointDto(snapshot.getPeriod(), balance, netChange));
            previous = balance;
        }
        return points;
    }
}
