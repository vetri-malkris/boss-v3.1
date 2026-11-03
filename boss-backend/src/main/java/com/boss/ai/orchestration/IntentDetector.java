package com.boss.ai.orchestration;

import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class IntentDetector {

    public Intent detect(String message) {
        String text = message.toLowerCase(Locale.ROOT);

        if (containsAny(text, "forecast", "projection", "predict", "next month", "next quarter", "future")) {
            return Intent.FORECAST;
        }
        if (containsAny(text, "profit", "loss", "losing money", "income")) {
            return Intent.PROFIT;
        }
        if (containsAny(text, "runway", "cash out", "run out of cash", "months of cash")) {
            return Intent.RUNWAY;
        }
        if (containsAny(text, "margin")) {
            return Intent.MARGIN;
        }
        if (containsAny(text, "expense", "cost", "spend", "spending", "burn")) {
            return Intent.EXPENSES;
        }
        if (containsAny(text, "revenue", "sales")) {
            return Intent.REVENUE;
        }
        if (containsAny(text, "risk", "anomaly", "concern", "worried", "problem")) {
            return Intent.RISKS;
        }
        return Intent.GENERAL;
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
