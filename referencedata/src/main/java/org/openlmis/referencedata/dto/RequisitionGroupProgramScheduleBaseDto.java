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
import java.util.Objects;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.openlmis.referencedata.domain.Facility;
import org.openlmis.referencedata.domain.ProcessingSchedule;
import org.openlmis.referencedata.domain.Program;
import org.openlmis.referencedata.domain.RequisitionGroup;
import org.openlmis.referencedata.domain.RequisitionGroupProgramSchedule;

@NoArgsConstructor
@AllArgsConstructor
public class RequisitionGroupProgramScheduleBaseDto extends BaseDto implements
      RequisitionGroupProgramSchedule.Exporter, RequisitionGroupProgramSchedule.Importer {

  @JsonProperty
  @Getter
  private RequisitionGroupBaseDto requisitionGroup;

  @JsonProperty
  @Getter
  private ProgramDto program;

  @Getter
  @Setter
  private ProcessingSchedule processingSchedule;

  @Getter
  @Setter
  private Boolean directDelivery;

  @JsonProperty
  @Getter
  private FacilityDto dropOffFacility;

  public RequisitionGroupProgramScheduleBaseDto(UUID id) {
    setId(id);
  }

  @JsonIgnore
  @Override
  public void setRequisitionGroup(RequisitionGroup requisitionGroup) {
    if (requisitionGroup != null) {
      this.requisitionGroup = new RequisitionGroupDto(requisitionGroup.getId());
      this.requisitionGroup.setCode(requisitionGroup.getCode());
      this.requisitionGroup.setName(requisitionGroup.getName());
      this.requisitionGroup.setSupervisoryNode(requisitionGroup.getSupervisoryNode());
    } else {
      this.requisitionGroup = null;
    }
  }

  public void setRequisitionGroup(RequisitionGroupBaseDto requisitionGroup) {
    this.requisitionGroup = requisitionGroup;
  }

  @JsonIgnore
  @Override
  public void setProgram(Program program) {
    if (program != null) {
      ProgramDto programDto = new ProgramDto(program.getId());
      programDto.setName(program.getName());
      this.program = programDto;
    } else {
      this.program = null;
    }
  }

  public void setProgram(ProgramDto program) {
    this.program = program;
  }

  @JsonIgnore
  @Override
  public void setDropOffFacility(Facility dropOffFacility) {
    if (dropOffFacility != null) {
      this.dropOffFacility = new FacilityDto(dropOffFacility.getId());
    } else {
      this.dropOffFacility = null;
    }
  }

  public void setDropOffFacility(FacilityDto dropOffFacility) {
    this.dropOffFacility = dropOffFacility;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof RequisitionGroupProgramScheduleBaseDto)) {
      return false;
    }
    RequisitionGroupProgramScheduleBaseDto that = (RequisitionGroupProgramScheduleBaseDto) obj;
    return Objects.equals(getId(), that.getId())
        && Objects.equals(requisitionGroup, that.requisitionGroup)
        && Objects.equals(program, that.program)
        && Objects.equals(processingSchedule, that.processingSchedule);
  }

  @Override
  public int hashCode() {
    return Objects.hash(getId(), requisitionGroup, program, processingSchedule);
  }
}
