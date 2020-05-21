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

import static org.openlmis.requisition.domain.requisition.Requisition.REQUISITION_LINE_ITEMS;
import static org.openlmis.requisition.domain.requisition.RequisitionLineItem.TOTAL_STOCKOUT_DAYS;
import static org.openlmis.requisition.i18n.MessageKeys.ERROR_STOCKOUT_DAYS_CANT_BE_GREATER_THAN_LENGTH_OF_PERIOD;

import java.util.Map;
import org.openlmis.requisition.domain.RequisitionTemplate;
import org.openlmis.requisition.dto.OrderableDto;
import org.openlmis.requisition.dto.VersionIdentityDto;
import org.openlmis.requisition.utils.Message;

class StockOutDaysValidator extends AbstractRegularRequisitionFullSupplyLineItemValidator {

  private static final int DAYS_IN_MONTH = 30;

  private final Integer numberOfMonthsInPeriod;
  private final RequisitionTemplate requisitionTemplate;

  StockOutDaysValidator(Requisition requisitionToValidate, Integer numberOfMonthsInPeriod,
      RequisitionTemplate requisitionTemplate, Map<VersionIdentityDto, OrderableDto> orderables) {
    super(requisitionToValidate, orderables);
    this.numberOfMonthsInPeriod = numberOfMonthsInPeriod;
    this.requisitionTemplate = requisitionTemplate;
  }

  @Override
  protected void validateFullSupplyLineItemForUpdate(
      Map<String, Message> errors, RequisitionLineItem requisitionLineItem) {
    if (requisitionLineItem.getTotalStockoutDays() == null) {
      return;
    }
    if (isGreaterThanLengthOfThePeriod(requisitionLineItem.getTotalStockoutDays())) {
      errors.put(REQUISITION_LINE_ITEMS,
          new Message(ERROR_STOCKOUT_DAYS_CANT_BE_GREATER_THAN_LENGTH_OF_PERIOD));
    }
  }

  @Override
  protected void validateFullSupplyLineItem(Map<String, Message> errors,
                                          RequisitionLineItem item) {
    rejectIfNullOrNegative(errors, requisitionTemplate,
        item.getTotalStockoutDays(), TOTAL_STOCKOUT_DAYS);
  }

  private boolean isGreaterThanLengthOfThePeriod(int totalStockoutDays) {
    return totalStockoutDays > numberOfMonthsInPeriod * DAYS_IN_MONTH;
  }


  @Override
  public boolean isForApprove() {
    return false;
  }
}
