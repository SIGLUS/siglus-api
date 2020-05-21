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
import static org.openlmis.requisition.i18n.MessageKeys.ERROR_REASON_NOT_IN_REQUISITION_REASON_LIST;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.openlmis.requisition.utils.Message;

@AllArgsConstructor
class StockAdjustmentReasonsValidator implements RequisitionUpdateDomainValidator {
  private final Requisition requisitionUpdater;
  private final Requisition requisitionToUpdate;

  @Override
  public void validateCanUpdate(Map<String, Message> errors) {
    validateIfReasonExists(errors, requisitionUpdater, requisitionToUpdate);
  }

  @Override
  public boolean isForRegularOnly() {
    return true;
  }

  private void validateIfReasonExists(Map<String, Message> errors, Requisition requisition,
                                      Requisition savedRequisition) {
    Set<UUID> reasons = savedRequisition.getStockAdjustmentReasons().stream()
        .map(StockAdjustmentReason::getReasonId)
        .collect(Collectors.toSet());

    requisition.getRequisitionLineItems().stream()
        .flatMap(i -> i.getStockAdjustments().stream())
        .forEach(a -> {
          if (!reasons.contains(a.getReasonId())) {
            errors.put(REQUISITION_LINE_ITEMS,
                new Message(ERROR_REASON_NOT_IN_REQUISITION_REASON_LIST, a.getReasonId()));
          }
        });
  }
}
