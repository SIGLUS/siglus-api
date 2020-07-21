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

package org.openlmis.requisition.dto;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public final class RequisitionGroupDto extends BaseDto {

  private String code;
  private String name;
  private String description;
  private Set<FacilityDto> memberFacilities;
  // [SIGLUS change start]
  // [change reason]: need program information.
  private SupervisoryNodeDto supervisoryNode;
  private Set<RequisitionGroupProgramScheduleDto> requisitionGroupProgramSchedules;
  // [SIGLUS change end]

  /**
   * Checks if there is a facility with the given ID in this requisition group.
   */
  public boolean hasFacility(UUID facilityId) {
    return memberFacilities
        .stream()
        .anyMatch(facility -> Objects.equals(facilityId, facility.getId()));
  }

  // [SIGLUS change start]
  // [change reason]: need program information.
  public boolean supportsProgram(UUID programId) {
    return requisitionGroupProgramSchedules.stream()
        .map(RequisitionGroupProgramScheduleDto::getProgram)
        .map(BaseDto::getId)
        .anyMatch(programId::equals);
  }
  // [SIGLUS change end]
}
