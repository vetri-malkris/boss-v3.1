package com.boss.risk.dto;

public record RiskDto(
        String risk,
        RiskSeverity severity,
        String evidence,
        EvidenceType evidenceType,
        String financialImpact,
        String possibleCause
) {
}
