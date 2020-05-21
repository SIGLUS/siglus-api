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

package org.openlmis.referencedata.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Objects;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.openlmis.referencedata.domain.Facility;
import org.openlmis.referencedata.domain.FulfillmentRoleAssignment;
import org.openlmis.referencedata.domain.Program;
import org.openlmis.referencedata.domain.Role;
import org.openlmis.referencedata.domain.RoleAssignment;
import org.openlmis.referencedata.domain.SupervisionRoleAssignment;
import org.openlmis.referencedata.domain.SupervisoryNode;

@JsonInclude(JsonInclude.Include.NON_NULL)
@NoArgsConstructor
@AllArgsConstructor
public class RoleAssignmentDto implements RoleAssignment.Exporter,
    SupervisionRoleAssignment.Exporter, FulfillmentRoleAssignment.Exporter {

  @Getter
  @Setter
  private UUID roleId;

  @Getter
  @Setter
  private UUID programId;

  @Getter
  @Setter
  private UUID supervisoryNodeId;

  @Getter
  @Setter
  private UUID warehouseId;

  @Override
  public void setRole(Role role) {
    roleId = role.getId();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof RoleAssignmentDto)) {
      return false;
    }
    RoleAssignmentDto that = (RoleAssignmentDto) obj;
    return Objects.equals(roleId, that.roleId)
        && Objects.equals(programId, that.programId)
        && Objects.equals(supervisoryNodeId, that.supervisoryNodeId)
        && Objects.equals(warehouseId, that.warehouseId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(roleId, programId, supervisoryNodeId, warehouseId);
  }

  @Override
  public void setProgram(Program program) {
    programId = program.getId();
  }

  @Override
  public void setSupervisoryNode(SupervisoryNode supervisoryNode) {
    supervisoryNodeId = supervisoryNode.getId();
  }
  
  @Override
  public void setWarehouse(Facility warehouse) {
    warehouseId = warehouse.getId();
  }
}

