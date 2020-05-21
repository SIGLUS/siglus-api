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

import static org.openlmis.referencedata.util.messagekeys.IdealStockAmountMessageKeys.ERROR_FROM_FIELD_REQUIRED;

import org.openlmis.referencedata.dto.IdealStockAmountCsvModel;
import org.openlmis.referencedata.exception.ValidationMessageException;
import org.openlmis.referencedata.util.Message;
import org.springframework.stereotype.Component;

@Component
public class IdealStockAmountValidator {

  public void validate(IdealStockAmountCsvModel idealStockAmount) {
    validateNullValues(idealStockAmount);
  }

  private void validateNullValues(IdealStockAmountCsvModel idealStockAmount) {
    validateNotNull(idealStockAmount.getFacility(), ERROR_FROM_FIELD_REQUIRED, "facility");
    validateNotNull(idealStockAmount.getCommodityType(), ERROR_FROM_FIELD_REQUIRED,
        "commodityType");
    validateNotNull(idealStockAmount.getProcessingPeriod(), ERROR_FROM_FIELD_REQUIRED,
        "processingPeriod");
    validateNotNull(idealStockAmount.getProcessingPeriod().getProcessingSchedule(),
        ERROR_FROM_FIELD_REQUIRED, "processingPeriod.processingSchedule");
    validateNotNull(idealStockAmount.getAmount(), ERROR_FROM_FIELD_REQUIRED, "amount");
  }

  private void validateNotNull(Object field, String errorMessage, String fieldName) {
    if (field == null) {
      throw new ValidationMessageException(new Message(errorMessage, fieldName));
    }
  }
}
