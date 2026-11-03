export interface ApiResponse<T> {
  data: T;
  timestamp: string;
  correlationId: string | null;
}

export interface ApiFieldError {
  field: string;
  message: string;
}

export interface ApiErrorResponse {
  timestamp: string;
  status: number;
  code: string;
  message: string;
  correlationId: string | null;
  path: string;
  fieldErrors?: ApiFieldError[];
}
