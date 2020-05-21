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

package org.openlmis.requisition.testutils;

import java.util.UUID;
import org.openlmis.requisition.dto.ReleasableRequisitionDto;
import org.openlmis.requisition.testutils.api.DtoDataBuilder;

public class ReleasableRequisitionDtoDataBuilder implements
    DtoDataBuilder<ReleasableRequisitionDto> {

  private UUID requisitionId;
  private UUID supplyingDepotId;

  /**
   * Builder for {@link ReleasableRequisitionDto}.
   */
  public ReleasableRequisitionDtoDataBuilder() {
    requisitionId = UUID.randomUUID();
    supplyingDepotId = UUID.randomUUID();
  }


  @Override
  public ReleasableRequisitionDto buildAsDto() {
    return new ReleasableRequisitionDto(requisitionId, supplyingDepotId);
  }

  public ReleasableRequisitionDtoDataBuilder withRequisitionId(UUID requisitionId) {
    this.requisitionId = requisitionId;
    return this;
  }

  public ReleasableRequisitionDtoDataBuilder withSupplyingDepotId(UUID supplyingDepotId) {
    this.supplyingDepotId = supplyingDepotId;
    return this;
  }
}
