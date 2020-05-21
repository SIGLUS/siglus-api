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

import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public final class UserDto extends BaseDto {
  private String username;
  private String firstName;
  private String lastName;
  private String email;
  private boolean verified;
  private UUID homeFacilityId;
  private Set<RoleAssignmentDto> roleAssignments;
  private Boolean allowNotify;
  private boolean active;

  /**
   * Checks if user has supervisory supervision role.
   */
  public boolean hasMatchingSupervisorySupervisionRole(UUID roleId, UUID programId,
      UUID supervisoryNodeId) {
    return roleAssignments
        .stream()
        .anyMatch(item -> roleId.equals(item.getRoleId())
            && programId.equals(item.getProgramId())
            && supervisoryNodeId.equals(item.getSupervisoryNodeId()));
  }

  /**
   * Checks if user has home facility supervision role.
   */
  public boolean hasMatchingHomeFacilitySupervisionRole(UUID roleId,
      UUID programId, UUID facilityId) {
    if (null == homeFacilityId) {
      return false;
    }

    if (!homeFacilityId.equals(facilityId)) {
      return false;
    }

    return roleAssignments
        .stream()
        .anyMatch(item -> roleId.equals(item.getRoleId())
            && programId.equals(item.getProgramId())
            && null == item.getSupervisoryNodeId());
  }

  /**
   * Prints the name of the user for display purposes.
   * The format is "firstName lastName". If one of them is missing, it is
   * omitted and the space is trimmed. If they are both missing, the
   * user name is used.
   * @return the name of the user for display purposes
   */
  public String printName() {
    if (StringUtils.isBlank(firstName) && StringUtils.isBlank(lastName)) {
      return username;
    } else {
      return StringUtils.trim(StringUtils.defaultString(firstName) + ' '
          + StringUtils.defaultString(lastName));
    }
  }

  public boolean allowNotify() {
    return this.getAllowNotify() != null && this.getAllowNotify();
  }

  public boolean activeAndVerified() {
    return this.isActive() && this.isVerified();
  }
}
