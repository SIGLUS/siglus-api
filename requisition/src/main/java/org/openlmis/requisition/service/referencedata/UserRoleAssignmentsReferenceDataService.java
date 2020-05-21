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

package org.openlmis.requisition.service.referencedata;

import java.util.Collection;
import java.util.UUID;
import org.openlmis.requisition.dto.DetailedRoleAssignmentDto;
import org.openlmis.requisition.dto.RightDto;
import org.springframework.stereotype.Service;

@Service
public class UserRoleAssignmentsReferenceDataService extends
    BaseReferenceDataService<DetailedRoleAssignmentDto> {

  @Override
  protected String getUrl() {
    return "/api/users/";
  }

  @Override
  protected Class<DetailedRoleAssignmentDto> getResultClass() {
    return DetailedRoleAssignmentDto.class;
  }

  @Override
  protected Class<DetailedRoleAssignmentDto[]> getArrayResultClass() {
    return DetailedRoleAssignmentDto[].class;
  }

  public Collection<DetailedRoleAssignmentDto> getRoleAssignments(UUID userId) {
    return findAll(userId + "/roleAssignments");
  }

  /**
   * Checks if given user has supervision right with given parameters.
   *
   * @param right             right to be checked
   * @param userId            UUID of user that roles will be verified
   * @param programId         UUID of program assigned to role
   * @param supervisoryNodeId UUID of supervisory node assigned to role
   */
  public boolean hasSupervisionRight(RightDto right, UUID userId, UUID programId,
                                     UUID supervisoryNodeId) {
    if (userId == null || right == null) {
      return false;
    }

    return getRoleAssignments(userId).stream()
        .filter(r -> r.getRole().getRights().contains(right))
        .anyMatch(r -> hasAnySupervisionRoleWithGivenParameters(r, programId, supervisoryNodeId));
  }

  private boolean hasAnySupervisionRoleWithGivenParameters(DetailedRoleAssignmentDto role,
                                                       UUID programId,
                                                       UUID supervisoryNodeId) {
    return role.getSupervisoryNodeId() != null
        && role.getProgramId() != null
        && (supervisoryNodeId == null || supervisoryNodeId.equals(role.getSupervisoryNodeId()))
        && (programId == null || programId.equals(role.getProgramId()));
  }
}
