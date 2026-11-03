export type RiskSeverity = 'LOW' | 'MEDIUM' | 'HIGH';
export type EvidenceType = 'FACT' | 'ASSUMPTION' | 'INFERENCE';

export interface Risk {
  risk: string;
  severity: RiskSeverity;
  evidence: string;
  evidenceType: EvidenceType;
  financialImpact: string;
  possibleCause: string;
}
