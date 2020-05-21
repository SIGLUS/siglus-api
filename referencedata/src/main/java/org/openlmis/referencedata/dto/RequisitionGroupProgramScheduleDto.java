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
import org.openlmis.referencedata.domain.Facility;
import org.openlmis.referencedata.domain.Program;
import org.openlmis.referencedata.domain.RequisitionGroup;

public class RequisitionGroupProgramScheduleDto extends RequisitionGroupProgramScheduleBaseDto {

  @JsonIgnore
  @Override
  public void setRequisitionGroup(RequisitionGroup requisitionGroup) {
    if (requisitionGroup != null) {
      RequisitionGroupBaseDto requisitionGroupBaseDto = new RequisitionGroupDto();
      requisitionGroup.export(requisitionGroupBaseDto);
      setRequisitionGroup(requisitionGroupBaseDto);
    } else {
      setRequisitionGroup((RequisitionGroupBaseDto) null);
    }
  }

  @JsonIgnore
  @Override
  public void setProgram(Program program) {
    if (program != null) {
      ProgramDto programDto = new ProgramDto();
      program.export(programDto);
      setProgram(programDto);
    } else {
      setProgram((ProgramDto) null);
    }
  }

  @JsonIgnore
  @Override
  public void setDropOffFacility(Facility dropOffFacility) {
    if (dropOffFacility != null) {
      FacilityDto facilityDto = new FacilityDto();
      dropOffFacility.export(facilityDto);
      setDropOffFacility(facilityDto);
    } else {
      setDropOffFacility((FacilityDto) null);
    }
  }
}
