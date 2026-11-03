export interface EvidenceItem {
  label: string;
  value: string;
}

export interface Diagnosis {
  issue: string;
  rootCause: string;
  impact: string;
}

export interface Recommendation {
  action: string;
  expectedOutcome: string;
  confidence: number;
  assumptions: string[];
  alternatives: string[];
  risks: string[];
}

export interface ChatResponse {
  summary: string;
  evidence: EvidenceItem[];
  diagnosis: Diagnosis | null;
  recommendations: Recommendation[];
}

export interface ChatMessage {
  role: 'user' | 'assistant';
  text: string;
  response?: ChatResponse;
}
