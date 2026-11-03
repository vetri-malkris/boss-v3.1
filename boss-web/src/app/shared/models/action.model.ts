export type ActionStatus = 'PENDING_APPROVAL' | 'APPROVED' | 'REJECTED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';

export interface ActionItem {
  id: string;
  recommendationId: string;
  recommendationIssue: string;
  description: string;
  status: ActionStatus;
  proposedByEmail?: string | null;
  createdAt: string;
  approvedByEmail?: string | null;
  approvedAt?: string | null;
  executedAt?: string | null;
  completedAt?: string | null;
  expectedOutcomeMetric: string;
  expectedOutcomeValue: number;
  actualOutcomeValue?: number | null;
  variance?: number | null;
  outcomeReason?: string | null;
}

export interface ProposeActionRequest {
  recommendationId: string;
  description: string;
  expectedOutcomeMetric: string;
  expectedOutcomeValue: number;
}

export interface CompleteActionRequest {
  actualOutcomeValue: number;
  outcomeReason?: string;
}
