import { RoleName } from './role.model';
import { SubcategoryCode } from './access.model';

export interface UserSummary {
  id: string;
  email: string;
  fullName: string;
  tenantId: string | null;
  roles: RoleName[];
  active: boolean;
  enabledSubcategories: SubcategoryCode[];
}

export interface CreateUserRequest {
  email: string;
  password: string;
  fullName: string;
  roles: RoleName[];
  enabledSubcategories: SubcategoryCode[];
}
