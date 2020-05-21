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

import java.util.Map;
import lombok.AllArgsConstructor;
import org.openlmis.requisition.dto.OrderableDto;
import org.openlmis.requisition.dto.VersionIdentityDto;
import org.openlmis.requisition.utils.Message;

@AllArgsConstructor
abstract class AbstractRegularRequisitionFullSupplyLineItemValidator
    implements RequisitionUpdateDomainValidator, RequisitionStatusChangeDomainValidator {

  protected final Requisition requisitionToValidate;
  private Map<VersionIdentityDto, OrderableDto> orderables;

  @Override
  public boolean isForRegularOnly() {
    return true;
  }

  @Override
  public void validateCanUpdate(Map<String, Message> errors) {
    requisitionToValidate.getNonSkippedFullSupplyRequisitionLineItems(orderables)
        .forEach(i -> validateFullSupplyLineItemForUpdate(errors, i));
  }

  @Override
  public void validateCanChangeStatus(Map<String, Message> errors) {
    requisitionToValidate.getNonSkippedFullSupplyRequisitionLineItems(orderables)
        .forEach(i -> validateFullSupplyLineItem(errors, i));
  }

  protected abstract void validateFullSupplyLineItemForUpdate(Map<String, Message> errors,
                                                              RequisitionLineItem lineItem);

  protected abstract void validateFullSupplyLineItem(Map<String, Message> errors,
                                                     RequisitionLineItem lineItem);

}
