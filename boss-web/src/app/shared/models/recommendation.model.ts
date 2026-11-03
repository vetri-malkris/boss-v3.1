export interface EvidenceItem {
  label: string;
  value: string;
}

export interface Recommendation {
  id: string;
  period: string;
  issue: string;
  evidence: EvidenceItem[];
  rootCause: string;
  impact: string;
  recommendedAction: string;
  alternatives: string[];
  expectedOutcome: string;
  risks: string[];
  confidence: number;
  assumptions: string[];
  timeHorizon: string;
  followUpMeasurement: string;
  createdAt: string;
}
