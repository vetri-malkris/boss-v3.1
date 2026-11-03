package com.boss.scenario.dto;

public record ScenarioResultDto(int horizonMonths, boolean hasEnoughData, ScenarioSideDto base, ScenarioSideDto scenario) {

    public static ScenarioResultDto insufficientData(int horizonMonths) {
        return new ScenarioResultDto(horizonMonths, false, null, null);
    }
}
