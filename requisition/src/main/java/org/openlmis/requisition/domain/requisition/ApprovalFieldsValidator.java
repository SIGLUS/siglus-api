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
import static org.openlmis.requisition.domain.requisition.RequisitionLineItem.APPROVED_QUANTITY;
import static org.openlmis.requisition.i18n.MessageKeys.ERROR_ONLY_AVAILABLE_FOR_APPROVAL;

import java.util.Map;
import lombok.AllArgsConstructor;
import org.openlmis.requisition.domain.RequisitionTemplate;
import org.openlmis.requisition.utils.Message;

@AllArgsConstructor
class ApprovalFieldsValidator
    implements RequisitionUpdateDomainValidator, RequisitionStatusChangeDomainValidator {
  private final Requisition requisitionUpdater;
  private final Requisition requisitionToUpdate;

  @Override
  public boolean isForRegularOnly() {
    return false;
  }

  @Override
  public boolean isForApprove() {
    return true;
  }

  @Override
  public void validateCanUpdate(Map<String, Message> errors) {
    requisitionUpdater.getNonSkippedRequisitionLineItems()
        .forEach(i -> validateLineItemForUpdate(errors, i));
  }

  @Override
  public void validateCanChangeStatus(Map<String, Message> errors) {
    requisitionToUpdate.getNonSkippedRequisitionLineItems()
        .forEach(i -> validateLineItem(errors, i));
  }

  private void validateLineItemForUpdate(Map<String, Message> errors,
                                         RequisitionLineItem item) {
    rejectIfInvalidStatusAndNotNull(errors, item.getApprovedQuantity(),
        new Message(ERROR_ONLY_AVAILABLE_FOR_APPROVAL,
            RequisitionLineItem.APPROVED_QUANTITY));

    rejectIfInvalidStatusAndNotNull(errors, item.getRemarks(),
        new Message(ERROR_ONLY_AVAILABLE_FOR_APPROVAL,
            RequisitionLineItem.REMARKS_COLUMN));
  }

  private void rejectIfInvalidStatusAndNotNull(Map<String, Message> errors,
                                               Object value,
                                               Message message) {
    if (requisitionToUpdate.getStatus().duringApproval() || value == null) {
      return;
    }
    errors.put(REQUISITION_LINE_ITEMS, message);
  }

  private void validateLineItem(Map<String, Message> errors, RequisitionLineItem item) {
    RequisitionTemplate template = requisitionToUpdate.getTemplate();

    if (requisitionToUpdate.isApprovable()) {
      rejectIfNull(errors, template, item.getApprovedQuantity(), APPROVED_QUANTITY);
      rejectIfLessThanZero(errors, template, item.getApprovedQuantity(), APPROVED_QUANTITY);
    }
  }
}
