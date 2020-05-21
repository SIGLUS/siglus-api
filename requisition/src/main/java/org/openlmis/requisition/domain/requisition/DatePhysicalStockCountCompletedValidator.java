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

package org.openlmis.requisition.domain.requisition;

import static org.openlmis.requisition.domain.requisition.Requisition.DATE_PHYSICAL_STOCK_COUNT_COMPLETED;
import static org.openlmis.requisition.i18n.MessageKeys.ERROR_DATE_STOCK_COUNT_MISMATCH;
import static org.openlmis.requisition.i18n.MessageKeys.ERROR_VALUE_MUST_BE_ENTERED;

import java.time.LocalDate;
import java.util.Map;
import java.util.Objects;
import lombok.AllArgsConstructor;
import org.openlmis.requisition.utils.Message;

@AllArgsConstructor
class DatePhysicalStockCountCompletedValidator
    implements RequisitionUpdateDomainValidator, RequisitionStatusChangeDomainValidator {
  private final DatePhysicalStockCountCompleted datePhysicalStockCountCompletedToValidate;
  private final Requisition requisitionToUpdate;
  private final LocalDate currentDate;
  private final boolean isDatePhysicalStockCountCompletedEnabled;

  /**
   * Validates {@link DatePhysicalStockCountCompleted} Value Object for Requisition update.
   */
  @Override
  public void validateCanUpdate(Map<String, Message> errors) {
    if (isDatePhysicalStockCountCompletedEnabled) {
      if (dateDifferAfterAuthorize()) {
        errors.put(DATE_PHYSICAL_STOCK_COUNT_COMPLETED,
            new Message(ERROR_DATE_STOCK_COUNT_MISMATCH));
      }
      if (datePhysicalStockCountCompletedToValidate != null) {
        datePhysicalStockCountCompletedToValidate.validateNotInFuture(errors, currentDate);
      }
    }
  }

  /**
   * Validates {@link DatePhysicalStockCountCompleted} Value Object for Requisition status changes.
   */
  @Override
  public void validateCanChangeStatus(Map<String, Message> errors) {
    if (isDatePhysicalStockCountCompletedEnabled
        && requisitionToUpdate.isPreAuthorize()
        && datePhysicalStockCountCompletedToValidate == null) {
      errors.put(DATE_PHYSICAL_STOCK_COUNT_COMPLETED,
          new Message(ERROR_VALUE_MUST_BE_ENTERED, DATE_PHYSICAL_STOCK_COUNT_COMPLETED));
    }
  }


  @Override
  public boolean isForRegularOnly() {
    return true;
  }

  @Override
  public boolean isForApprove() {
    return false;
  }

  private boolean dateDifferAfterAuthorize() {
    return requisitionToUpdate.getStatus().isAuthorized()
        && !Objects.equals(requisitionToUpdate.getDatePhysicalStockCountCompleted(),
        datePhysicalStockCountCompletedToValidate);
  }
}
