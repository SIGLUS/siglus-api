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

package org.openlmis.requisition.validate;

import static org.openlmis.requisition.i18n.MessageKeys.ERROR_DATE_MODIFIED_MISMATCH;
import static org.openlmis.requisition.i18n.MessageKeys.VERSION_MISMATCH;

import java.time.ZonedDateTime;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.errorhandling.ValidationResult;
import org.openlmis.requisition.web.ETagResource;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

/**
 * Validates the version of a requisition. If the version is present and does not match the
 * version of the stored requisition, the validation should lead to a 409 error.
 */
@Component
public class RequisitionVersionValidator {

  /**
   * Validates if the date modified of the incoming requisition matches
   * the date modified of the existing requisition. If the incoming requisition has
   * no modified date, that will not trigger the exception.
   * @param incomingModifiedDate modified date from incoming requisition
   * @param existingReq the existing version of the requisition
   * @return ValidationResult that contains outcome of this validation
   */
  public ValidationResult validateRequisitionTimestamps(ZonedDateTime incomingModifiedDate,
      Requisition existingReq) {
    if (incomingModifiedDate != null
        && !incomingModifiedDate.isEqual(existingReq.getModifiedDate())) {
      return ValidationResult.conflict(ERROR_DATE_MODIFIED_MISMATCH,
          existingReq.getId().toString());
    }
    return ValidationResult.success();
  }

  /**
   * Validates whether the incoming request to update the requisition resource is operating
   * on the same version that is currently stored in the database (provided If-Match request
   * header is set). If the versions differ, this means the requisition has changed in the meanwhile
   * and the current version cannot be saved as it may overwrite the changes. In case If-Match
   * header is not set, this validation will always pass.
   *
   * @param request the incoming request to update requisition
   * @param requisition the existing version of the requisition
   * @return ValidationResult that contains outcome of this validation
   */
  public ValidationResult validateEtagVersionIfPresent(HttpServletRequest request,
                                                       Requisition requisition) {
    String etagVersion = request.getHeader(HttpHeaders.IF_MATCH);

    if (StringUtils.isNotBlank(etagVersion)) {
      Long version = ETagResource.readVersionFromEtag(etagVersion);

      if (!version.equals(requisition.getVersion())) {
        return ValidationResult.conflict(VERSION_MISMATCH);
      }
    }
    return ValidationResult.success();
  }
}
