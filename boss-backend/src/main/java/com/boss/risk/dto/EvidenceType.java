package com.boss.risk.dto;

/**
 * Every risk's evidence must be classified so the UI (and any future AI layer)
 * never presents an inference as a confirmed fact (section 10).
 */
public enum EvidenceType {
    FACT,
    ASSUMPTION,
    INFERENCE
}
