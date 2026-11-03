package com.boss.action.domain;

/**
 * The action lifecycle (section 17). {@code PROPOSED} and {@code PENDING_APPROVAL}
 * are collapsed into one initial state here - in this app, proposing an action and
 * submitting it for approval are the same user step.
 */
public enum ActionStatus {
    PENDING_APPROVAL,
    APPROVED,
    REJECTED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED
}
