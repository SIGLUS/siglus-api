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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.openlmis.referencedata.domain.Facility;
import org.openlmis.referencedata.domain.Program;
import org.openlmis.referencedata.domain.SupervisoryNode;
import org.openlmis.referencedata.domain.SupplyLine;

@NoArgsConstructor
public class SupplyLineSimpleDto extends BaseDto implements SupplyLine.Exporter,
    SupplyLine.Importer {

  @JsonProperty
  private UUID supervisoryNode;

  @Getter
  @Setter
  private String description;

  @JsonProperty
  private UUID program;

  @JsonProperty
  private UUID supplyingFacility;

  @JsonIgnore
  @Override
  public SupervisoryNode.Importer getSupervisoryNode() {
    if (supervisoryNode != null) {
      return new SupervisoryNodeBaseDto(supervisoryNode);
    }

    return null;
  }

  @JsonIgnore
  @Override
  public void setSupervisoryNode(SupervisoryNode supervisoryNode) {
    if (supervisoryNode != null) {
      this.supervisoryNode = supervisoryNode.getId();
    } else {
      this.supervisoryNode = null;
    }
  }

  @JsonIgnore
  @Override
  public Program.Importer getProgram() {
    if (program != null) {
      return new ProgramDto(program);
    }

    return null;
  }

  @JsonIgnore
  @Override
  public void setProgram(Program program) {
    if (program != null) {
      this.program = program.getId();
    } else {
      this.program = null;
    }
  }

  @JsonIgnore
  @Override
  public Facility.Importer getSupplyingFacility() {
    if (supplyingFacility != null) {
      return new FacilityDto(supplyingFacility);
    }

    return null;
  }

  @JsonIgnore
  @Override
  public void setSupplyingFacility(Facility supplyingFacility) {
    if (supplyingFacility != null) {
      this.supplyingFacility = supplyingFacility.getId();
    } else {
      this.supplyingFacility = null;
    }
  }
}
