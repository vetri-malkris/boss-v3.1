/** Only ever returned on full success - a rejected upload comes back as an HTTP
 * error instead, with every problem row listed in its message. */
export interface CategoryUploadResult {
  fileName: string;
  recordsBySheet: Record<string, number>;
  totalRecords: number;
}
