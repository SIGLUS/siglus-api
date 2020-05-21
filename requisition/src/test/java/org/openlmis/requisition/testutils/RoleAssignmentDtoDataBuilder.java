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
import org.openlmis.requisition.dto.RoleAssignmentDto;
import org.openlmis.requisition.testutils.api.DtoDataBuilder;

public class RoleAssignmentDtoDataBuilder implements DtoDataBuilder<RoleAssignmentDto> {

  private UUID roleId;
  private UUID programId;
  private UUID supervisoryNodeId;
  private UUID warehouseId;

  /**
   * Builder for {@link RoleAssignmentDto}.
   */
  public RoleAssignmentDtoDataBuilder() {
    roleId = UUID.randomUUID();
    programId = UUID.randomUUID();
    supervisoryNodeId = UUID.randomUUID();
    warehouseId = UUID.randomUUID();
  }

  @Override
  public RoleAssignmentDto buildAsDto() {
    return new RoleAssignmentDto(roleId, programId, supervisoryNodeId, warehouseId);
  }

  public RoleAssignmentDtoDataBuilder withRoleId(UUID roleId) {
    this.roleId = roleId;
    return this;
  }

  public RoleAssignmentDtoDataBuilder withProgramId(UUID programId) {
    this.programId = programId;
    return this;
  }

  public RoleAssignmentDtoDataBuilder withSupervisoryNodeId(UUID supervisoryNodeId) {
    this.supervisoryNodeId = supervisoryNodeId;
    return this;
  }

  public RoleAssignmentDtoDataBuilder withWarehouseId(UUID warehouseId) {
    this.warehouseId = warehouseId;
    return this;
  }
}
