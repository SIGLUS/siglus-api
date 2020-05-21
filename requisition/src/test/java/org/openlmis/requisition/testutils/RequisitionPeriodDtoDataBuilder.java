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
import org.openlmis.requisition.domain.requisition.RequisitionStatus;
import org.openlmis.requisition.dto.RequisitionPeriodDto;
import org.openlmis.requisition.testutils.api.DtoDataBuilder;

public class RequisitionPeriodDtoDataBuilder
    implements DtoDataBuilder<RequisitionPeriodDto> {

  private UUID id;
  private UUID requisitionId;
  private RequisitionStatus requisitionStatus;

  /**
   * Builder for {@link RequisitionPeriodDto}.
   */
  public RequisitionPeriodDtoDataBuilder() {
    id = UUID.randomUUID();
    requisitionId = UUID.randomUUID();
    requisitionStatus = RequisitionStatus.INITIATED;
  }

  @Override
  public RequisitionPeriodDto buildAsDto() {
    RequisitionPeriodDto requisitionPeriodDto = new RequisitionPeriodDto();
    requisitionPeriodDto.setId(id);
    requisitionPeriodDto.setRequisitionId(requisitionId);
    requisitionPeriodDto.setRequisitionStatus(requisitionStatus);
    return requisitionPeriodDto;
  }

  public RequisitionPeriodDtoDataBuilder withId(UUID id) {
    this.id = id;
    return this;
  }
}
