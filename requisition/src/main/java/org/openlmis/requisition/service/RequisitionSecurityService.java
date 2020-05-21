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

package org.openlmis.requisition.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RequisitionSecurityService {

  @Autowired
  private PermissionService permissionService;

  /**
   * Filters requisitions based on user permissions. It strives to make as little calls to the
   * reference data service as possible.
   *
   * @param allRequisitions input list containing any requisitions
   * @return filtered input list of requisitions, containing only those that the user has access to
   */
  public List<Requisition> filterInaccessibleRequisitions(List<Requisition> allRequisitions) {

    Map<RightsFor, Boolean> verified = new HashMap<>();
    List<Requisition> filteredList = new ArrayList<>();

    for (Requisition requisition : allRequisitions) {
      Boolean accessible = getCachedRight(verified, requisition);

      if (accessible == null) {
        accessible = permissionService.canViewRequisition(requisition).isSuccess();
        addCachedRight(verified, requisition, accessible);
      }

      if (accessible) {
        filteredList.add(requisition);
      }
    }

    return filteredList;
  }

  private Boolean getCachedRight(Map<RightsFor, Boolean> verified, Requisition requisition) {
    return verified.get(new RightsFor(requisition.getProgramId(), requisition.getFacilityId()));
  }

  private void addCachedRight(Map<RightsFor, Boolean> verified,
                              Requisition requisition, Boolean accessible) {
    verified.put(
        new RightsFor(requisition.getProgramId(), requisition.getFacilityId()), accessible);
  }

  @AllArgsConstructor
  @EqualsAndHashCode
  private class RightsFor {
    private UUID program;
    private UUID facility;
  }
}
