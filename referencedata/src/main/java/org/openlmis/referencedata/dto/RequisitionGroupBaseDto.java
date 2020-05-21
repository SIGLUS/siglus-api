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
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.openlmis.referencedata.domain.Facility;
import org.openlmis.referencedata.domain.RequisitionGroup;
import org.openlmis.referencedata.domain.RequisitionGroupProgramSchedule;
import org.openlmis.referencedata.domain.SupervisoryNode;

@NoArgsConstructor
public class RequisitionGroupBaseDto extends BaseDto implements RequisitionGroup.Importer,
    RequisitionGroup.Exporter {

  @Getter
  @Setter
  private String code;

  @Getter
  @Setter
  private String name;

  @Getter
  @Setter
  private String description;

  @JsonProperty
  @Getter
  private SupervisoryNodeBaseDto supervisoryNode;

  public RequisitionGroupBaseDto(UUID id) {
    setId(id);
  }

  @JsonIgnore
  @Override
  public void setSupervisoryNode(SupervisoryNode supervisoryNode) {
    if (supervisoryNode != null) {
      this.supervisoryNode = new SupervisoryNodeDto(supervisoryNode.getId());
    } else {
      this.supervisoryNode = null;
    }
  }

  public void setSupervisoryNode(SupervisoryNodeBaseDto supervisoryNode) {
    this.supervisoryNode = supervisoryNode;
  }

  @Override
  public List<RequisitionGroupProgramSchedule.Importer> getRequisitionGroupProgramSchedules() {
    return null;
  }

  @JsonIgnore
  @Override
  public void setRequisitionGroupProgramSchedules(
      List<RequisitionGroupProgramSchedule> schedules) {}

  @Override
  public Set<Facility.Importer> getMemberFacilities() {
    return null;
  }

  @JsonIgnore
  @Override
  public void setMemberFacilities(Set<Facility> memberFacilities) {}

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof RequisitionGroupBaseDto)) {
      return false;
    }
    RequisitionGroupBaseDto that = (RequisitionGroupBaseDto) obj;
    return Objects.equals(getId(), that.getId()) && Objects.equals(code, that.code);
  }

  @Override
  public int hashCode() {
    return Objects.hash(getId(), code);
  }
}
