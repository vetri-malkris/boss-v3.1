package com.boss.analytics.dto;

import java.util.List;

public record MonthlyComparisonDto(String currentPeriod, String previousPeriod, List<ChangeDto> changes) {
}
