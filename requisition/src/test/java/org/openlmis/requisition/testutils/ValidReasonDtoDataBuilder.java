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
import org.openlmis.requisition.dto.ReasonDto;
import org.openlmis.requisition.dto.ValidReasonDto;
import org.openlmis.requisition.testutils.api.DtoDataBuilder;

public class ValidReasonDtoDataBuilder implements DtoDataBuilder<ValidReasonDto> {

  private UUID programId;
  private UUID facilityTypeId;
  private ReasonDto reason;
  private Boolean hidden;

  /**
   * Builder for {@link ValidReasonDto}.
   */
  public ValidReasonDtoDataBuilder() {
    programId = UUID.randomUUID();
    facilityTypeId = UUID.randomUUID();
    reason = new ReasonDtoDataBuilder().buildAsDto();
    hidden = false;
  }

  @Override
  public ValidReasonDto buildAsDto() {
    ValidReasonDto dto = new ValidReasonDto();
    dto.setProgramId(programId);
    dto.setFacilityTypeId(facilityTypeId);
    dto.setReason(reason);
    dto.setHidden(hidden);
    return dto;
  }
}
