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
import static org.openlmis.requisition.i18n.MessageKeys.ERROR_STOCK_ADJUSTMENT_NON_NEGATIVE;
import static org.openlmis.requisition.i18n.MessageKeys.ERROR_STOCK_ADJUSTMENT_NOT_FOUND;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.openlmis.requisition.dto.OrderableDto;
import org.openlmis.requisition.dto.VersionIdentityDto;
import org.openlmis.requisition.utils.Message;

class StockAdjustmentsValidator
    extends AbstractRegularRequisitionFullSupplyLineItemStatusChangeValidator {

  StockAdjustmentsValidator(Requisition requisitionToValidate,
      Map<VersionIdentityDto, OrderableDto> orderables) {
    super(requisitionToValidate, orderables);
  }

  protected void validateFullSupplyLineItem(Map<String, Message> errors,
                                            RequisitionLineItem item) {
    List<UUID> reasons = requisitionToValidate
        .getStockAdjustmentReasons()
        .stream().map(StockAdjustmentReason::getReasonId).collect(Collectors.toList());

    for (StockAdjustment adjustment : new ArrayList<>(item.getStockAdjustments())) {
      if (!reasons.contains(adjustment.getReasonId())) {
        errors.put(REQUISITION_LINE_ITEMS,
            new Message(ERROR_STOCK_ADJUSTMENT_NOT_FOUND, adjustment.getReasonId()));
      }

      if (adjustment.getQuantity() == null || adjustment.getQuantity() < 0) {
        errors.put(REQUISITION_LINE_ITEMS,
            new Message(ERROR_STOCK_ADJUSTMENT_NON_NEGATIVE, adjustment.getReasonId()));
      }
    }
  }

  /**
   * There is not point for checking if adjustments are valid when changing status to approve
   * or in approval because after requisition is authorized, stock adjustments can't be changed.
   */
  @Override
  public boolean isForApprove() {
    return false;
  }
}
