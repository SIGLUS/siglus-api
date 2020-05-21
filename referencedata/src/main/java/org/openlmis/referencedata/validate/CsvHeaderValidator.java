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

package org.openlmis.referencedata.validate;

import static org.openlmis.referencedata.util.StringHelper.lowerCase;
import static org.openlmis.referencedata.util.messagekeys.CsvUploadMessageKeys.ERROR_UPLOAD_HEADER_INVALID;
import static org.openlmis.referencedata.util.messagekeys.CsvUploadMessageKeys.ERROR_UPLOAD_HEADER_MISSING;
import static org.openlmis.referencedata.util.messagekeys.CsvUploadMessageKeys.ERROR_UPLOAD_MISSING_MANDATORY_COLUMNS;

import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.collections.ListUtils;
import org.openlmis.referencedata.exception.ValidationMessageException;
import org.openlmis.referencedata.util.Message;
import org.openlmis.referencedata.web.csv.model.ModelClass;
import org.openlmis.referencedata.web.csv.model.ModelField;
import org.springframework.stereotype.Component;

@Component
public class CsvHeaderValidator {

  /**
   * Validate csv header names.
   */
  public void validateHeaders(List<String> headers,
                              ModelClass modelClass,
                              boolean acceptExtraHeaders) {
    validateNullHeaders(headers);
    List<String> lowerCaseHeaders = lowerCase(headers);
    if (!acceptExtraHeaders) {
      validateInvalidHeaders(lowerCaseHeaders, modelClass);
    }
    validateMandatoryFields(lowerCaseHeaders, modelClass);
  }

  private void validateNullHeaders(List<String> headers) throws ValidationMessageException {
    for (int i = 0; i < headers.size(); i++) {
      if (headers.get(i) == null) {
        throw new ValidationMessageException(new Message(ERROR_UPLOAD_HEADER_MISSING,
            String.valueOf(i + 1)));
      }
    }
  }

  private void validateMandatoryFields(List<String> headers, ModelClass modelClass) {
    List<String> missingFields = findMissingFields(headers, modelClass);

    if (!missingFields.isEmpty()) {
      throw new ValidationMessageException(new Message(ERROR_UPLOAD_MISSING_MANDATORY_COLUMNS,
          missingFields.toString()));
    }
  }

  private void validateInvalidHeaders(List<String> headers, ModelClass modelClass) {
    List<String> fieldNames = getAllImportedFieldNames(modelClass);
    List invalidHeaders = ListUtils.subtract(headers, lowerCase(fieldNames));
    if (!invalidHeaders.isEmpty()) {
      throw new ValidationMessageException(new Message(ERROR_UPLOAD_HEADER_INVALID,
          invalidHeaders.toString()));
    }
  }

  private List<String> findMissingFields(List<String> headers, ModelClass<?> modelClass) {
    return modelClass
        .getImportFields()
        .stream()
        .filter(fields -> fields.isMandatory() && !headers.contains(fields.getName().toLowerCase()))
        .map(ModelField::getName)
        .collect(Collectors.toList());
  }

  private List<String> getAllImportedFieldNames(ModelClass<?> modelClass) {
    return modelClass
        .getImportFields()
        .stream()
        .map(ModelField::getName)
        .collect(Collectors.toList());
  }
}
