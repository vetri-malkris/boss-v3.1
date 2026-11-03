package com.boss.dataentry.categoryupload.dto;

import java.util.Map;

/** Only ever returned on full success - a rejected upload throws instead, with
 * every problem row listed in the exception message (nothing partial is possible). */
public record CategoryUploadResultDto(String fileName, Map<String, Integer> recordsBySheet, int totalRecords) {
}
