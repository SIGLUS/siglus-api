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

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.openlmis.referencedata.domain.Facility;
import org.openlmis.referencedata.domain.RequisitionGroup;
import org.openlmis.referencedata.domain.RequisitionGroupProgramSchedule;
import org.openlmis.referencedata.domain.SupervisoryNode;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, exclude = "serviceUrl")
@ToString(callSuper = true)
public final class RequisitionGroupObjectReferenceDto extends ObjectReferenceDto implements
    RequisitionGroup.Exporter {

  private static final String REQUISITION_GROUPS = "requisitionGroups";

  @JsonIgnore
  private String serviceUrl;

  private List<RequisitionGroupProgramScheduleBaseDto> requisitionGroupProgramSchedules =
      new ArrayList<>();
  private Set<BasicFacilityDto> memberFacilities = new HashSet<>();
  private String code;
  private String name;
  private String description;
  private SupervisoryNodeObjectReferenceDto supervisoryNode;

  public RequisitionGroupObjectReferenceDto(UUID id, String serviceUrl) {
    super(serviceUrl, REQUISITION_GROUPS, id);
    this.serviceUrl = serviceUrl;
  }

  @JsonSetter("supervisoryNode")
  public void setSupervisoryNode(SupervisoryNodeObjectReferenceDto supervisoryNode) {
    this.supervisoryNode = supervisoryNode;
  }

  @Override
  @JsonIgnore
  public void setSupervisoryNode(SupervisoryNode supervisoryNode) {
    if (null != supervisoryNode) {
      this.supervisoryNode =
          new SupervisoryNodeObjectReferenceDto(supervisoryNode.getId(), serviceUrl);
    }
  }

  @JsonIgnore
  @Override
  public void setRequisitionGroupProgramSchedules(List<RequisitionGroupProgramSchedule> schedules) {
    // unsupported
  }

  @Override
  @JsonIgnore
  public void setMemberFacilities(Set<Facility> memberFacilities) {
    // unsupported
  }

  @JsonGetter("memberFacilities")
  public Set<BasicFacilityDto> getMemberFacilities() {
    return this.memberFacilities;
  }
}
