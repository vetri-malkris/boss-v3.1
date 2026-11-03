package com.boss.common.access;

/** The fixed top-level sidebar categories. Not tenant-customizable. */
public enum Category {
    HUMAN_RESOURCE,
    FINANCE,
    ACCOUNTING,
    AUDITING,
    COMPLIANCE,
    RISK;

    public String label() {
        return switch (this) {
            case HUMAN_RESOURCE -> "Human Resource";
            case FINANCE -> "Finance";
            case ACCOUNTING -> "Accounting";
            case AUDITING -> "Auditing";
            case COMPLIANCE -> "Compliance";
            case RISK -> "Risk";
        };
    }
}
