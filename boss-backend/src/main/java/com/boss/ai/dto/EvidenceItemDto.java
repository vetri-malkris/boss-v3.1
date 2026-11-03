package com.boss.ai.dto;

/** One fact the response is grounded in - always traceable back to a deterministic calculation. */
public record EvidenceItemDto(String label, String value) {
}
