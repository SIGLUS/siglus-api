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

package org.siglus.common.dto.referencedata;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSetter;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.siglus.common.domain.referencedata.Facility;
import org.siglus.common.domain.referencedata.Program;
import org.siglus.common.domain.referencedata.SupervisoryNode;
import org.siglus.common.domain.referencedata.SupplyLine;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class SupplyLineDto extends BaseDto
    implements SupplyLine.Exporter, SupplyLine.Importer {

  private SupervisoryNodeBaseDto supervisoryNode;
  private String description;
  private ProgramDto program;
  private FacilityDto supplyingFacility;

  @JsonSetter("supervisoryNode")
  public void setSupervisoryNode(SupervisoryNodeBaseDto supervisoryNode) {
    this.supervisoryNode = supervisoryNode;
  }

  @Override
  @JsonIgnore
  public void setSupervisoryNode(SupervisoryNode supervisoryNode) {
    if (supervisoryNode != null) {
      this.supervisoryNode = new SupervisoryNodeDto();
      supervisoryNode.export(this.supervisoryNode);
    } else {
      this.supervisoryNode = null;
    }
  }

  @JsonSetter("program")
  public void setProgram(ProgramDto program) {
    this.program = program;
  }

  @Override
  @JsonIgnore
  public void setProgram(Program program) {
    if (program != null) {
      this.program = new ProgramDto();
      program.export(this.program);
    } else {
      this.program = null;
    }
  }

  @JsonSetter("supplyingFacility")
  public void setSupplyingFacility(FacilityDto supplyingFacility) {
    this.supplyingFacility = supplyingFacility;
  }

  @Override
  @JsonIgnore
  public void setSupplyingFacility(Facility supplyingFacility) {
    if (supplyingFacility != null) {
      this.supplyingFacility = new FacilityDto();
      supplyingFacility.export(this.supplyingFacility);
    } else {
      this.supplyingFacility = null;
    }
  }

}
