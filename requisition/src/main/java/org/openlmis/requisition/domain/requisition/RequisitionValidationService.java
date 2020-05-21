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
import org.openlmis.requisition.dto.OrderableDto;
import org.openlmis.requisition.dto.VersionIdentityDto;
import org.openlmis.requisition.errorhandling.ValidationResult;
import org.openlmis.requisition.utils.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;
import org.slf4j.profiler.Profiler;

public class RequisitionValidationService {
  private static final XLogger XLOGGER = XLoggerFactory
      .getXLogger(RequisitionValidationService.class);
  private static final Logger LOGGER = LoggerFactory
      .getLogger(RequisitionValidationService.class);

  private final Requisition savedRequisition;
  private List<RequisitionUpdateDomainValidator> validators = new ArrayList<>();

  /**
   * Constructs new requisition validation service.
   */
  public RequisitionValidationService(Requisition requisition, Requisition savedRequisition,
                                      Map<VersionIdentityDto, OrderableDto> orderables,
                                      LocalDate currentDate,
                                      boolean isDatePhysicalStockCountCompletedEnabled) {
    this.savedRequisition = savedRequisition;
    validators.add(new RequisitionInvariantsValidator(requisition, savedRequisition, orderables));
    validators.add(new ApprovalFieldsValidator(requisition, savedRequisition));
    validators.add(new StockAdjustmentReasonsValidator(requisition, savedRequisition));
    validators.add(new DatePhysicalStockCountCompletedValidator(
        requisition.getDatePhysicalStockCountCompleted(), savedRequisition, currentDate,
        isDatePhysicalStockCountCompletedEnabled));
    validators.add(new StockOnHandValidator(requisition,
        savedRequisition.getTemplate(), orderables));
    validators.add(new TotalConsumedQuantityValidator(requisition,
        savedRequisition.getTemplate(), orderables));
    validators.add(new StockOutDaysValidator(requisition,
        savedRequisition.getNumberOfMonthsInPeriod(), savedRequisition.getTemplate(), orderables));
  }

  /**
   * Validates if requisition can be updated. Return errors as {@link ValidationResult}.
   */
  ValidationResult validateRequisitionCanBeUpdated() {
    XLOGGER.entry();
    Profiler profiler = new Profiler("VALIDATE_REQUISITION_CAN_BE_UPDATE");
    profiler.setLogger(XLOGGER);

    Map<String, Message> errors = new HashMap<>();

    for (RequisitionUpdateDomainValidator validator : validators) {
      if (!validator.isForRegularOnly()) {
        profiler.start("USE_" + validator.getName());
        validator.validateCanUpdate(errors);
      }

      if (validator.isForRegularOnly() && isNotTrue(savedRequisition.getEmergency())) {
        profiler.start("USE_" + validator.getName());
        validator.validateCanUpdate(errors);
      }
    }

    ValidationResult result;

    if (isEmpty(errors)) {
      result = ValidationResult.success();
    } else {
      LOGGER.warn("Validation for requisition update failed: {}", errors);
      result = ValidationResult.fieldErrors(errors);
    }

    profiler.stop().log();
    XLOGGER.exit(result);

    return result;
  }
}
