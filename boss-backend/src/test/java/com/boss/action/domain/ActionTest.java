package com.boss.action.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ActionTest {

    @Test
    void startsInPendingApproval() {
        Action action = newAction();

        assertThat(action.getStatus()).isEqualTo(ActionStatus.PENDING_APPROVAL);
    }

    @Test
    void approveRecordsApproverAndTimestamp() {
        Action action = newAction();
        UUID approver = UUID.randomUUID();

        action.approve(approver);

        assertThat(action.getStatus()).isEqualTo(ActionStatus.APPROVED);
        assertThat(action.getApprovedBy()).isEqualTo(approver);
        assertThat(action.getApprovedAt()).isNotNull();
    }

    @Test
    void cannotApproveTwice() {
        Action action = newAction();
        action.approve(UUID.randomUUID());

        assertThatThrownBy(() -> action.approve(UUID.randomUUID())).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejectedActionCannotBeStarted() {
        Action action = newAction();
        action.reject(UUID.randomUUID());

        assertThat(action.getStatus()).isEqualTo(ActionStatus.REJECTED);
        assertThatThrownBy(action::start).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void fullHappyPathComputesPositiveVariance() {
        Action action = newAction(); // expected outcome value = 100.00 (see newAction())
        action.approve(UUID.randomUUID());
        action.start();

        action.complete(new BigDecimal("72.00"), "Savings were partially achieved.");

        assertThat(action.getStatus()).isEqualTo(ActionStatus.COMPLETED);
        assertThat(action.getVariance()).isEqualByComparingTo("-28.00"); // 72 - 100
        assertThat(action.getOutcomeReason()).isEqualTo("Savings were partially achieved.");
        assertThat(action.getCompletedAt()).isNotNull();
    }

    @Test
    void canCompleteDirectlyFromApprovedWithoutStarting() {
        Action action = newAction();
        action.approve(UUID.randomUUID());

        action.complete(new BigDecimal("150.00"), null);

        assertThat(action.getVariance()).isEqualByComparingTo("50.00"); // 150 - 100
    }

    @Test
    void cannotCompleteBeforeApproval() {
        Action action = newAction();

        assertThatThrownBy(() -> action.complete(new BigDecimal("100.00"), null))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void cannotCancelACompletedAction() {
        Action action = newAction();
        action.approve(UUID.randomUUID());
        action.complete(new BigDecimal("100.00"), null);

        assertThatThrownBy(action::cancel).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void pendingActionCanBeCancelled() {
        Action action = newAction();

        action.cancel();

        assertThat(action.getStatus()).isEqualTo(ActionStatus.CANCELLED);
    }

    private Action newAction() {
        return new Action(UUID.randomUUID(), "Reduce monthly expense by 100", UUID.randomUUID(),
                "Monthly profit", new BigDecimal("100.00"));
    }
}
