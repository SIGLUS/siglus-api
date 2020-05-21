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

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.openlmis.requisition.domain.requisition.RequisitionStatus;
import org.openlmis.requisition.dto.ApproveRequisitionDto;
import org.openlmis.requisition.dto.ApproveRequisitionLineItemDto;
import org.openlmis.requisition.testutils.api.DtoDataBuilder;

public class ApproveRequisitionDtoDataBuilder implements DtoDataBuilder<ApproveRequisitionDto> {

  private UUID id;
  private Boolean emergency;
  private RequisitionStatus status;
  private ZonedDateTime modifiedDate;
  private String periodName;
  private String facilityName;
  private List<ApproveRequisitionLineItemDto> requisitionLineItems;

  /**
   * Builder for {@link ApproveRequisitionDto}.
   */
  public ApproveRequisitionDtoDataBuilder() {
    id = UUID.randomUUID();
    emergency = false;
    status = RequisitionStatus.INITIATED;
    modifiedDate = ZonedDateTime.now();
    periodName = "period name";
    facilityName = "facility name";
    requisitionLineItems = Arrays.asList(
        new ApproveRequisitionLineItemDtoDataBuilder().buildAsDto());
  }

  @Override
  public ApproveRequisitionDto buildAsDto() {
    return new ApproveRequisitionDto(id,
        emergency,
        status,
        modifiedDate,
        periodName,
        facilityName,
        requisitionLineItems
    );
  }

  public ApproveRequisitionDtoDataBuilder withId(UUID id) {
    this.id = id;
    return this;
  }
}
