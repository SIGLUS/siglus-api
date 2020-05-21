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

import static org.apache.commons.lang3.BooleanUtils.isNotTrue;
import static org.springframework.util.CollectionUtils.isEmpty;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openlmis.requisition.dto.ApprovedProductDto;
import org.openlmis.requisition.dto.OrderableDto;
import org.openlmis.requisition.dto.VersionIdentityDto;
import org.openlmis.requisition.errorhandling.ValidationResult;
import org.openlmis.requisition.utils.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StatusChangeValidationService {

  private static final Logger logger = LoggerFactory.getLogger(StatusChangeValidationService.class);

  private final Requisition requisition;
  private List<RequisitionStatusChangeDomainValidator> validators = new ArrayList<>();

  /**
   * Constructs new requisition validation service.
   */
  public StatusChangeValidationService(Requisition requisition, LocalDate currentDate,
      boolean isDatePhysicalStockCountCompletedEnabled,
      Map<VersionIdentityDto, OrderableDto> orderables,
      Map<VersionIdentityDto, ApprovedProductDto> approvedProducts) {
    this.requisition = requisition;
    validators.add(new RequisitionInvariantsValidator(requisition, requisition, orderables));
    validators.add(new ApprovalFieldsValidator(requisition, requisition));
    validators.add(new DatePhysicalStockCountCompletedValidator(
        requisition.getDatePhysicalStockCountCompleted(), requisition, currentDate,
        isDatePhysicalStockCountCompletedEnabled));
    validators.add(new StockOnHandValidator(requisition, requisition.getTemplate(), orderables));
    validators.add(new TotalConsumedQuantityValidator(requisition,
        requisition.getTemplate(), orderables));
    validators.add(new StockOutDaysValidator(requisition, requisition.getNumberOfMonthsInPeriod(),
        requisition.getTemplate(), orderables));
    validators.add(new BeginningBalanceValidator(requisition,
        requisition.getTemplate(), orderables));
    validators.add(new CalculatedFieldsValidator(requisition,
        requisition.getTemplate(), orderables, approvedProducts));
    validators.add(new NumberOfNewPatientsAddedValidator(requisition, orderables));
    validators.add(new RequestedQuantityValidator(requisition, orderables));
    validators.add(new StockAdjustmentsValidator(requisition, orderables));
    validators.add(new TotalFieldValidator(requisition, requisition.getTemplate(), orderables));
    validators.add(new TotalReceivedQuantityValidator(requisition,
        requisition.getTemplate(), orderables));
  }

  /**
   * Validates if requisition can be updated. Return errors as {@link ValidationResult}.
   */
  ValidationResult validateRequisitionCanChangeStatus() {
    Map<String, Message> errors = new HashMap<>();

    for (RequisitionStatusChangeDomainValidator validator : validators) {
      if (!requisition.getStatus().duringApproval() || validator.isForApprove()) {
        if (!validator.isForRegularOnly()) {
          validator.validateCanChangeStatus(errors);
        }
        if (isNotTrue(requisition.getEmergency())
            && validator.isForRegularOnly()) {
          validator.validateCanChangeStatus(errors);
        }
      }

    }
    if (isEmpty(errors)) {
      return ValidationResult.success();
    }

    logger.warn("Validation for requisition status change failed: {}", errors);
    return ValidationResult.fieldErrors(errors);
  }
}
