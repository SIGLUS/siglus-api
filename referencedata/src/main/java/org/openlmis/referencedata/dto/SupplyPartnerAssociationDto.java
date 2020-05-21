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
import com.fasterxml.jackson.annotation.JsonSetter;
import com.google.common.collect.Lists;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.openlmis.referencedata.domain.Facility;
import org.openlmis.referencedata.domain.Orderable;
import org.openlmis.referencedata.domain.Program;
import org.openlmis.referencedata.domain.SupervisoryNode;
import org.openlmis.referencedata.domain.SupplyPartnerAssociation;
import org.openlmis.referencedata.web.BaseController;
import org.openlmis.referencedata.web.FacilityController;
import org.openlmis.referencedata.web.OrderableController;
import org.openlmis.referencedata.web.ProgramController;
import org.openlmis.referencedata.web.SupervisoryNodeController;

@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public final class SupplyPartnerAssociationDto extends BaseDto
    implements SupplyPartnerAssociation.Importer, SupplyPartnerAssociation.Exporter {

  @Setter
  @JsonIgnore
  private String serviceUrl;

  @Getter
  private ObjectReferenceDto program;

  @Getter
  private ObjectReferenceDto supervisoryNode;

  @Getter
  @Setter
  private List<ObjectReferenceDto> facilities = Lists.newArrayList();

  @Getter
  @Setter
  private List<ObjectReferenceDto> orderables = Lists.newArrayList();

  @Override
  @JsonIgnore
  public UUID getProgramId() {
    return Optional
        .ofNullable(program)
        .map(BaseDto::getId)
        .orElse(null);
  }

  @Override
  @JsonIgnore
  public UUID getSupervisoryNodeId() {
    return Optional
        .ofNullable(supervisoryNode)
        .map(BaseDto::getId)
        .orElse(null);
  }

  @Override
  @JsonIgnore
  public Set<UUID> getFacilityIds() {
    return Optional
        .ofNullable(facilities)
        .orElse(Collections.emptyList())
        .stream()
        .map(ObjectReferenceDto::getId)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
  }

  @Override
  @JsonIgnore
  public Set<UUID> getOrderableIds() {
    return Optional
        .ofNullable(orderables)
        .orElse(Collections.emptyList())
        .stream()
        .map(ObjectReferenceDto::getId)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
  }

  @JsonSetter("program")
  public void setProgram(ObjectReferenceDto program) {
    this.program = program;
  }

  @Override
  @JsonIgnore
  public void setProgram(Program program) {
    this.program = new ObjectReferenceDto(serviceUrl,
        BaseController.API_PATH + ProgramController.RESOURCE_PATH, program.getId());
  }

  @JsonSetter("supervisoryNode")
  public void setSupervisoryNode(ObjectReferenceDto supervisoryNode) {
    this.supervisoryNode = supervisoryNode;
  }

  @Override
  @JsonIgnore
  public void setSupervisoryNode(SupervisoryNode supervisoryNode) {
    this.supervisoryNode = new ObjectReferenceDto(serviceUrl,
        BaseController.API_PATH + SupervisoryNodeController.RESOURCE_PATH, supervisoryNode.getId());
  }

  @Override
  public void addFacility(Facility facility) {
    facilities.add(
        new ObjectReferenceDto(serviceUrl,
            BaseController.API_PATH + FacilityController.RESOURCE_PATH, facility.getId()));
  }

  @Override
  public void addOrderable(Orderable orderable) {
    orderables.add(
        new ObjectReferenceDto(serviceUrl,
            BaseController.API_PATH + OrderableController.RESOURCE_PATH, orderable.getId()));
  }
}
