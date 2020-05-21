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

import static org.openlmis.requisition.domain.requisition.RequisitionLineItem.CALCULATED_ORDER_QUANTITY;
import static org.openlmis.requisition.domain.requisition.RequisitionLineItem.CALCULATED_ORDER_QUANTITY_ISA;
import static org.openlmis.requisition.domain.requisition.RequisitionLineItem.REQUESTED_QUANTITY;
import static org.openlmis.requisition.domain.requisition.RequisitionLineItem.REQUESTED_QUANTITY_EXPLANATION;

import java.util.Map;
import java.util.Objects;
import lombok.AllArgsConstructor;
import org.apache.commons.lang.BooleanUtils;
import org.openlmis.requisition.domain.RequisitionTemplate;
import org.openlmis.requisition.dto.OrderableDto;
import org.openlmis.requisition.dto.VersionIdentityDto;
import org.openlmis.requisition.utils.Message;

@AllArgsConstructor
class RequestedQuantityValidator implements RequisitionStatusChangeDomainValidator {
  private final Requisition requisitionToValidate;
  private Map<VersionIdentityDto, OrderableDto> orderables;

  @Override
  public void validateCanChangeStatus(Map<String, Message> errors) {
    requisitionToValidate.getNonSkippedNonFullSupplyRequisitionLineItems(orderables)
        .forEach(i -> validateNonFullSupplyLineItem(errors, i));
    requisitionToValidate.getNonSkippedFullSupplyRequisitionLineItems(orderables)
        .forEach(i -> validateFullSupplyLineItem(errors, i));
  }

  @Override
  public boolean isForRegularOnly() {
    return false;
  }

  @Override
  public boolean isForApprove() {
    return false;
  }

  private void validateNonFullSupplyLineItem(Map<String, Message> errors,
                                             RequisitionLineItem item) {
    RequisitionTemplate template = requisitionToValidate.getTemplate();

    rejectIfNullOrNegative(errors, template, item.getRequestedQuantity(),
        REQUESTED_QUANTITY);

    rejectIfNonNullValueForHiddenColumn(errors, item.getRequestedQuantityExplanation(),
        REQUESTED_QUANTITY_EXPLANATION, template.isColumnDisplayed(REQUESTED_QUANTITY_EXPLANATION));
  }

  private void validateFullSupplyLineItem(Map<String, Message> errors,
                                          RequisitionLineItem item) {
    RequisitionTemplate template = requisitionToValidate.getTemplate();

    rejectIfNonNullValueForHiddenColumn(errors, item.getRequestedQuantityExplanation(),
        REQUESTED_QUANTITY_EXPLANATION, template.isColumnDisplayed(REQUESTED_QUANTITY_EXPLANATION));

    if (BooleanUtils.isTrue(requisitionToValidate.getEmergency())) {
      rejectIfNullOrNegative(errors, template, item.getRequestedQuantity(),
          REQUESTED_QUANTITY);
    } else {
      validateRequestedQuantityAndExplanation(errors, item, template);
    }
  }

  private void validateRequestedQuantityAndExplanation(Map<String, Message> errors,
                                                       RequisitionLineItem item,
                                                       RequisitionTemplate template) {
    rejectIfLessThanZero(errors, template, item.getRequestedQuantity(), REQUESTED_QUANTITY);

    if (template.isColumnDisplayed(CALCULATED_ORDER_QUANTITY)) {
      validateRequestedQuantityAndExplanation(
          errors, item, template, item.getCalculatedOrderQuantity());
    } else if (template.isColumnInTemplate(CALCULATED_ORDER_QUANTITY_ISA)
        && template.isColumnDisplayed(CALCULATED_ORDER_QUANTITY_ISA)) {
      validateRequestedQuantityAndExplanation(
          errors, item, template, item.getCalculatedOrderQuantityIsa());
    } else {
      rejectIfNull(errors, template, item.getRequestedQuantity(), REQUESTED_QUANTITY);
    }
  }

  private void validateRequestedQuantityAndExplanation(Map<String, Message> errors,
                                                       RequisitionLineItem item,
                                                       RequisitionTemplate template,
                                                       Integer calculatedValue) {
    if (template.isColumnDisplayed(REQUESTED_QUANTITY)) {
      if (item.getRequestedQuantity() != null
          && !Objects.equals(item.getRequestedQuantity(), calculatedValue)) {
        rejectIfEmpty(errors, template, item.getRequestedQuantityExplanation(),
            REQUESTED_QUANTITY_EXPLANATION);
      }
    } else {
      rejectIfNonNullValueForHiddenColumn(errors, item.getRequestedQuantity(),
          REQUESTED_QUANTITY, template.isColumnDisplayed(REQUESTED_QUANTITY));
    }
  }

}
