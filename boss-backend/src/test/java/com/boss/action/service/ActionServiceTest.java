package com.boss.action.service;

import com.boss.action.domain.Action;
import com.boss.action.domain.ActionStatus;
import com.boss.action.dto.ActionDto;
import com.boss.action.dto.CompleteActionRequest;
import com.boss.action.dto.ProposeActionRequest;
import com.boss.action.repository.ActionRepository;
import com.boss.audit.service.AuditService;
import com.boss.common.exception.BusinessValidationException;
import com.boss.common.exception.ResourceNotFoundException;
import com.boss.common.exception.TenantAccessDeniedException;
import com.boss.identity.repository.UserRepository;
import com.boss.recommendation.domain.Recommendation;
import com.boss.recommendation.repository.RecommendationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActionServiceTest {

    @Mock
    private ActionRepository actionRepository;
    @Mock
    private RecommendationRepository recommendationRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AuditService auditService;

    private ActionService service;
    private final UUID tenantId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new ActionService(actionRepository, recommendationRepository, userRepository, auditService);
    }

    @Test
    void proposeCreatesAnActionLinkedToTheRecommendation() {
        Recommendation recommendation = recommendation();
        when(recommendationRepository.findById(recommendation.getId())).thenReturn(Optional.of(recommendation));
        when(actionRepository.save(any())).thenAnswer(inv -> {
            Action saved = inv.getArgument(0);
            if (saved.getId() == null) {
                saved.setId(UUID.randomUUID());
            }
            return saved;
        });

        ProposeActionRequest request = new ProposeActionRequest(recommendation.getId(), "Cut discretionary spend",
                "Monthly profit", new BigDecimal("100.00"));

        ActionDto dto = service.propose(tenantId, userId, request);

        assertThat(dto.recommendationId()).isEqualTo(recommendation.getId());
        assertThat(dto.status()).isEqualTo(ActionStatus.PENDING_APPROVAL);
        assertThat(dto.recommendationIssue()).isEqualTo("Operating at a loss");
    }

    @Test
    void proposeRejectsARecommendationFromAnotherTenant() {
        Recommendation recommendation = recommendation();
        recommendation.setTenantId(UUID.randomUUID()); // different tenant
        when(recommendationRepository.findById(recommendation.getId())).thenReturn(Optional.of(recommendation));

        ProposeActionRequest request = new ProposeActionRequest(recommendation.getId(), "x", "y", BigDecimal.ONE);

        assertThatThrownBy(() -> service.propose(tenantId, userId, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void approveThenCompleteComputesVariance() {
        Action action = existingAction();
        when(actionRepository.findById(action.getId())).thenReturn(Optional.of(action));
        when(actionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.approve(tenantId, userId, action.getId());
        ActionDto completed = service.complete(tenantId, action.getId(), new CompleteActionRequest(new BigDecimal("72.00"), "Partial"));

        assertThat(completed.status()).isEqualTo(ActionStatus.COMPLETED);
        assertThat(completed.variance()).isEqualByComparingTo("-28.00");
    }

    @Test
    void cannotCompleteAnActionThatWasNeverApproved() {
        Action action = existingAction();
        when(actionRepository.findById(action.getId())).thenReturn(Optional.of(action));

        assertThatThrownBy(() -> service.complete(tenantId, action.getId(),
                new CompleteActionRequest(new BigDecimal("100.00"), null)))
                .isInstanceOf(BusinessValidationException.class);
    }

    @Test
    void enforcesTenantIsolationOnLookup() {
        Action action = existingAction();
        when(actionRepository.findById(action.getId())).thenReturn(Optional.of(action));

        assertThatThrownBy(() -> service.approve(UUID.randomUUID(), userId, action.getId()))
                .isInstanceOf(TenantAccessDeniedException.class);
    }

    private Recommendation recommendation() {
        Recommendation recommendation = new Recommendation("2026-01", "Operating at a loss", "Operating at a loss",
                "Expenses exceeded revenue", "Cash depleted", "Cut costs", "Narrow the gap", new BigDecimal("0.55"),
                "1-2 months", "Re-check next month");
        recommendation.setTenantId(tenantId);
        setId(recommendation);
        return recommendation;
    }

    private Action existingAction() {
        Action action = new Action(UUID.randomUUID(), "Cut discretionary spend", userId, "Monthly profit",
                new BigDecimal("100.00"));
        action.setTenantId(tenantId);
        setId(action);
        return action;
    }

    private void setId(com.boss.common.domain.BaseEntity entity) {
        entity.setId(UUID.randomUUID());
    }
}
