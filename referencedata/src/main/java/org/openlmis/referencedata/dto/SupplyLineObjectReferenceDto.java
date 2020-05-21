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
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSetter;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.openlmis.referencedata.domain.Facility;
import org.openlmis.referencedata.domain.Program;
import org.openlmis.referencedata.domain.SupervisoryNode;
import org.openlmis.referencedata.domain.SupplyLine;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true, exclude = "serviceUrl")
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class SupplyLineObjectReferenceDto extends BaseDto implements SupplyLine.Exporter {

  @Setter
  @JsonIgnore
  private String serviceUrl;

  @Getter
  private SupervisoryNodeObjectReferenceDto supervisoryNode;

  @Setter
  @Getter
  private String description;

  @Getter
  private ProgramObjectReferenceDto program;

  @Getter
  private FacilityObjectReferenceDto supplyingFacility;

  @JsonSetter("supervisoryNode")
  public void setSupervisoryNode(SupervisoryNodeObjectReferenceDto supervisoryNode) {
    this.supervisoryNode = supervisoryNode;
  }

  @Override
  @JsonIgnore
  public void setSupervisoryNode(SupervisoryNode supervisoryNode) {
    if (supervisoryNode != null) {
      this.supervisoryNode =
          new SupervisoryNodeObjectReferenceDto(supervisoryNode.getId(), serviceUrl);
    }
  }

  @JsonSetter("program")
  public void setProgram(ProgramObjectReferenceDto program) {
    this.program = program;
  }

  @Override
  @JsonIgnore
  public void setProgram(Program program) {
    if (program != null) {
      this.program = new ProgramObjectReferenceDto(program.getId(), serviceUrl);
    }
  }

  @JsonSetter("supplyingFacility")
  public void setSupplyingFacility(FacilityObjectReferenceDto supplyingFacility) {
    this.supplyingFacility = supplyingFacility;
  }

  @Override
  @JsonIgnore
  public void setSupplyingFacility(Facility supplyingFacility) {
    if (supplyingFacility != null) {
      this.supplyingFacility =
          new FacilityObjectReferenceDto(supplyingFacility.getId(), serviceUrl);
    }
  }

  /**
   * Creates a new instance based on data from a domain object.
   */
  public static SupplyLineObjectReferenceDto newInstance(SupplyLine supplyLine, String serviceUrl) {
    SupplyLineObjectReferenceDto dto = new SupplyLineObjectReferenceDto();
    dto.setServiceUrl(serviceUrl);

    supplyLine.export(dto);

    return dto;
  }
}
