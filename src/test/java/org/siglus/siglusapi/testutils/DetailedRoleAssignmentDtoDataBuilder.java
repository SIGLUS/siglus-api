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

package org.siglus.siglusapi.testutils;

import java.util.UUID;
import org.openlmis.requisition.dto.DetailedRoleAssignmentDto;
import org.openlmis.requisition.dto.RoleDto;
import org.siglus.siglusapi.testutils.api.DtoDataBuilder;

public class DetailedRoleAssignmentDtoDataBuilder implements
    DtoDataBuilder<DetailedRoleAssignmentDto> {

  private RoleDto role;
  private String programCode;
  private String supervisoryNodeCode;
  private String warehouseCode;
  private UUID programId;
  private UUID supervisoryNodeId;

  /**
   * Builder for {@link DetailedRoleAssignmentDto}.
   */
  public DetailedRoleAssignmentDtoDataBuilder() {
    role = new org.siglus.siglusapi.testutils.RoleDtoDataBuilder().buildAsDto();
    programCode = "program code";
    supervisoryNodeCode = "supervisory node code";
    warehouseCode = "warehouse node";
    programId = UUID.randomUUID();
    supervisoryNodeId = UUID.randomUUID();
  }

  @Override
  public DetailedRoleAssignmentDto buildAsDto() {
    DetailedRoleAssignmentDto dto = new DetailedRoleAssignmentDto();
    dto.setRole(role);
    dto.setProgramCode(programCode);
    dto.setSupervisoryNodeCode(supervisoryNodeCode);
    dto.setWarehouseCode(warehouseCode);
    dto.setProgramId(programId);
    dto.setSupervisoryNodeId(supervisoryNodeId);
    return dto;
  }
}
