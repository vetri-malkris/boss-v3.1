import { SubcategoryCode } from './access.model';

export type FieldType = 'TEXT' | 'NUMBER' | 'DATE' | 'ENUM';

export interface FieldDefinition {
  name: string;
  label: string;
  type: FieldType;
  required: boolean;
  options: string[];
}

export interface SubcategorySchema {
  subcategory: SubcategoryCode;
  fields: FieldDefinition[];
}

export interface DataRecord {
  id: string;
  subcategory: SubcategoryCode;
  fields: Record<string, string | number>;
  createdAt: string;
  updatedAt: string;
}

export interface BulkUploadResult {
  fileName: string;
  rowsCreated: number;
  rowsSkipped: number;
  warnings: string[];
}
