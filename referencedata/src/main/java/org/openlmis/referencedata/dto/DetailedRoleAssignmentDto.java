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
import lombok.Getter;
import lombok.Setter;
import org.openlmis.referencedata.domain.Facility;
import org.openlmis.referencedata.domain.FulfillmentRoleAssignment;
import org.openlmis.referencedata.domain.Program;
import org.openlmis.referencedata.domain.Role;
import org.openlmis.referencedata.domain.RoleAssignment;
import org.openlmis.referencedata.domain.SupervisionRoleAssignment;
import org.openlmis.referencedata.domain.SupervisoryNode;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class DetailedRoleAssignmentDto implements RoleAssignment.Exporter,
    SupervisionRoleAssignment.Exporter, FulfillmentRoleAssignment.Exporter {

  @Getter
  private RoleDto role;

  @Getter
  @Setter
  private String programCode;

  @Getter
  @Setter
  private String supervisoryNodeCode;

  @Getter
  @Setter
  private String warehouseCode;

  @Getter
  @Setter
  private UUID programId;

  @Getter
  @Setter
  private UUID supervisoryNodeId;

  @Getter
  private UUID warehouseId;

  @Override
  public void setRole(Role role) {
    RoleDto roleDto = new RoleDto();
    role.export(roleDto);
    this.role = roleDto;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof DetailedRoleAssignmentDto)) {
      return false;
    }
    DetailedRoleAssignmentDto that = (DetailedRoleAssignmentDto) obj;
    return Objects.equals(role, that.role)
        && Objects.equals(programCode, that.programCode)
        && Objects.equals(supervisoryNodeCode, that.supervisoryNodeCode)
        && Objects.equals(warehouseCode, that.warehouseCode)
        && Objects.equals(programId, that.programId)
        && Objects.equals(supervisoryNodeId, that.supervisoryNodeId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(role, programCode, supervisoryNodeCode, warehouseCode,
        programId, supervisoryNodeId);
  }

  @Override
  public void setProgram(Program program) {
    this.programId = program.getId();
    this.programCode = program.getCode().toString();
  }

  @Override
  public void setSupervisoryNode(SupervisoryNode supervisoryNode) {
    this.supervisoryNodeId = supervisoryNode.getId();
    this.supervisoryNodeCode = supervisoryNode.getCode();
  }
  
  @Override
  public void setWarehouse(Facility warehouse) {
    this.warehouseCode = warehouse.getCode();
    this.warehouseId = warehouse.getId();
  }
}

