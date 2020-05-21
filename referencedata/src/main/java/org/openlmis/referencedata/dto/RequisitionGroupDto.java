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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.NoArgsConstructor;
import org.openlmis.referencedata.domain.Facility;
import org.openlmis.referencedata.domain.RequisitionGroupProgramSchedule;
import org.openlmis.referencedata.domain.SupervisoryNode;

@NoArgsConstructor
public class RequisitionGroupDto extends RequisitionGroupBaseDto {

  @JsonProperty
  private List<RequisitionGroupProgramScheduleBaseDto> requisitionGroupProgramSchedules;

  @JsonProperty
  private Set<FacilityDto> memberFacilities;

  public RequisitionGroupDto(UUID id) {
    super(id);
  }

  @JsonIgnore
  @Override
  public void setSupervisoryNode(SupervisoryNode supervisoryNode) {
    if (supervisoryNode != null) {
      SupervisoryNodeBaseDto supervisoryNodeBaseDto = new SupervisoryNodeBaseDto();
      supervisoryNode.export(supervisoryNodeBaseDto);
      setSupervisoryNode(supervisoryNodeBaseDto);
    } else {
      setSupervisoryNode((SupervisoryNodeBaseDto) null);
    }
  }

  @JsonIgnore
  @Override
  public void setRequisitionGroupProgramSchedules(List<RequisitionGroupProgramSchedule> schedules) {
    if (schedules != null) {
      List<RequisitionGroupProgramScheduleBaseDto> scheduleBaseDtos = new ArrayList<>();

      for (RequisitionGroupProgramSchedule schedule : schedules) {
        RequisitionGroupProgramScheduleBaseDto scheduleBaseDto =
            new RequisitionGroupProgramScheduleBaseDto();
        schedule.export(scheduleBaseDto);
        scheduleBaseDtos.add(scheduleBaseDto);
      }

      setRequisitionGroupProgramScheduleDtos(scheduleBaseDtos);
    } else {
      setRequisitionGroupProgramScheduleDtos(null);
    }
  }

  @Override
  public List<RequisitionGroupProgramSchedule.Importer> getRequisitionGroupProgramSchedules() {
    if (requisitionGroupProgramSchedules == null) {
      return null;
    }

    List<RequisitionGroupProgramSchedule.Importer> schedules = new ArrayList<>();
    schedules.addAll(requisitionGroupProgramSchedules);
    return schedules;
  }

  public void setRequisitionGroupProgramScheduleDtos(
      List<RequisitionGroupProgramScheduleBaseDto> schedules) {
    this.requisitionGroupProgramSchedules = schedules;
  }

  @JsonIgnore
  @Override
  public void setMemberFacilities(Set<Facility> memberFacilities) {
    if (memberFacilities != null) {
      Set<FacilityDto> facilityDtos = new HashSet<>();

      for (Facility facility : memberFacilities) {
        FacilityDto facilityDto = new FacilityDto();
        facility.export(facilityDto);
        facilityDtos.add(facilityDto);
      }

      setMemberFacilityDtos(facilityDtos);
    } else {
      setMemberFacilityDtos(null);
    }
  }

  @Override
  public Set<Facility.Importer> getMemberFacilities() {
    if (memberFacilities == null) {
      return null;
    }

    Set<Facility.Importer> facilities = new HashSet<>();
    facilities.addAll(memberFacilities);
    return facilities;
  }

  public void setMemberFacilityDtos(Set<FacilityDto> memberFacilities) {
    this.memberFacilities = memberFacilities;
  }

}
