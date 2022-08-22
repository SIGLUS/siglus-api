/*
 * This program is part of the OpenLMIS logistics management information system platform software.
 * Copyright © 2017 VillageReach
 *
 * This program is free software: you can redistribute it and/or modify it under the terms
 * of the GNU Affero General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details. You should have received a copy of
 * the GNU Affero General Public License along with this program. If not, see
 * http://www.gnu.org/licenses.  For additional information contact info@OpenLMIS.org.
 */

package org.siglus.siglusapi.validator;

import com.google.common.collect.Lists;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang.StringUtils;
import org.siglus.siglusapi.constant.LocationConstants;
import org.siglus.siglusapi.dto.Message;
import org.siglus.siglusapi.exception.BusinessDataException;
import org.siglus.siglusapi.exception.ValidationMessageException;
import org.siglus.siglusapi.i18n.CsvUploadMessageKeys;
import org.springframework.stereotype.Component;

@Component
public class CsvValidator {
  private static final String ERROR_BUSINESS_CODE = "upload csv failed";
  private static final List<String> mandatoryColumnNames = Arrays.asList(LocationConstants.LOCATION_CODE,
      LocationConstants.AREA, LocationConstants.ZONE, LocationConstants.RACK,
      LocationConstants.BARCODE, LocationConstants.BIN, LocationConstants.LEVEL);

  public void validateCsvHeaders(CSVParser csvParser) {
    Map<String, Integer> csvHeaderToColumnMap = csvParser.getHeaderMap();
    List<String> headers = new ArrayList<>(csvHeaderToColumnMap.keySet());
    validateNullHeaders(headers);
    List<String> lowerCaseHeaders = lowerCase(headers);
    validateInvalidHeaders(lowerCaseHeaders);
  }

  public void validateNullRow(CSVRecord eachRow) {
    long row = eachRow.getRecordNumber();
    validateEachColumn(eachRow.get(LocationConstants.LOCATION_CODE), LocationConstants.LOCATION_CODE, row);
    validateEachColumn(eachRow.get(LocationConstants.AREA), LocationConstants.AREA, row);
    validateEachColumn(eachRow.get(LocationConstants.ZONE), LocationConstants.ZONE, row);
    validateEachColumn(eachRow.get(LocationConstants.RACK), LocationConstants.RACK, row);
    validateEachColumn(eachRow.get(LocationConstants.BARCODE), LocationConstants.BARCODE, row);
    validateEachColumn(eachRow.get(LocationConstants.BIN), LocationConstants.BIN, row);
    validateEachColumn(eachRow.get(LocationConstants.LEVEL), LocationConstants.LEVEL, row);
  }

  public List<CSVRecord> validateDuplicateLocationCode(CSVParser csvParser) throws IOException {
    Map<String, List<Long>> locationCodeToDuplicateRowMap = new HashMap<>();
    List<CSVRecord> records = csvParser.getRecords();
    records.forEach(eachRow -> {
      List<Long> duplicateRows = Lists.newArrayList();
      String locationCode = eachRow.get(LocationConstants.LOCATION_CODE);
      long rowNumber = eachRow.getRecordNumber();
      if (locationCodeToDuplicateRowMap.containsKey(locationCode)) {
        duplicateRows = locationCodeToDuplicateRowMap.get(locationCode);
      }
      duplicateRows.add(rowNumber);
      locationCodeToDuplicateRowMap.put(locationCode, duplicateRows);
    });

    for (String locationCode : locationCodeToDuplicateRowMap.keySet()) {
      if (locationCodeToDuplicateRowMap.get(locationCode).size() > 1) {
        throw new BusinessDataException(new Message(CsvUploadMessageKeys.ERROR_DUPLICATE_LOCATION_CODE,
            locationCodeToDuplicateRowMap.get(locationCode).toString()), ERROR_BUSINESS_CODE);
      }
    }

    return records;
  }

  private void validateNullHeaders(List<String> headers) throws ValidationMessageException {
    if (headers.size() == 6) {
      throw new BusinessDataException(new Message(CsvUploadMessageKeys.ERROR_UPLOAD_HEADER_MISSING,
          String.valueOf(1)), ERROR_BUSINESS_CODE);
    }
    for (int i = 0; i < headers.size(); i++) {
      if (StringUtils.isEmpty(headers.get(i))) {
        throw new BusinessDataException(new Message(CsvUploadMessageKeys.ERROR_UPLOAD_HEADER_MISSING,
            String.valueOf(i + 1)), ERROR_BUSINESS_CODE);
      }
    }
  }

  private void validateInvalidHeaders(List<String> headers) {
    List<String> invalidHeaders = ListUtils.subtract(headers, lowerCase(mandatoryColumnNames));
    if (!invalidHeaders.isEmpty()) {
      throw new BusinessDataException(new Message(CsvUploadMessageKeys.ERROR_UPLOAD_HEADER_INVALID,
          invalidHeaders.toString()), ERROR_BUSINESS_CODE);
    }
  }

  private List<String> lowerCase(List<String> strings) {
    return strings.stream()
        .map(String::toLowerCase)
        .collect(Collectors.toList());
  }

  private void validateEachColumn(String columnValue, String columnName, long row) throws ValidationMessageException {
    if (StringUtils.isBlank(columnValue)) {
      throw new BusinessDataException(new Message(CsvUploadMessageKeys.ERROR_UPLOAD_MISSING_ROW,
          columnName, row), ERROR_BUSINESS_CODE);
    }
  }
}
