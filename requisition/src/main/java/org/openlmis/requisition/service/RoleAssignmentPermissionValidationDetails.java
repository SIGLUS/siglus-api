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

import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.openlmis.requisition.domain.requisition.Requisition;

@ToString
@EqualsAndHashCode
final class RoleAssignmentPermissionValidationDetails implements PermissionValidationDetails {

  @Getter
  private String rightName;

  private Requisition requisition;

  RoleAssignmentPermissionValidationDetails(String rightName, Requisition requisition) {
    this.rightName = rightName;
    this.requisition = requisition;
  }

  @Override
  public UUID getFacilityId() {
    return requisition.getFacilityId();
  }

  @Override
  public UUID getProgramId() {
    return requisition.getProgramId();
  }

  @Override
  public UUID getWarehouseId() {
    return null;
  }

  @Override
  public UUID getSupervisoryNodeId() {
    return requisition.getSupervisoryNodeId();
  }

  @Override
  public boolean containsPartnerRequisition() {
    return requisition.hasOriginalRequisitionId();
  }

}
