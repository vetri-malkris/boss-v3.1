import { UserSummary } from './user.model';

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterTenantRequest {
  tenantName: string;
  tenantSlug: string;
  adminEmail: string;
  adminPassword: string;
  adminFullName: string;
}

export interface LoginResponse {
  accessToken: string;
  tokenType: string;
  expiresInSeconds: number;
  user: UserSummary;
}
