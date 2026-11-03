package com.boss.analytics.dto;

public record TrendDto(String metric, TrendDirection direction, int monthsObserved) {
}
