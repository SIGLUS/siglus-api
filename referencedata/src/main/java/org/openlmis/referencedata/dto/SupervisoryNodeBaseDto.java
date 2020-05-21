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
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.openlmis.referencedata.domain.Facility;
import org.openlmis.referencedata.domain.RequisitionGroup;
import org.openlmis.referencedata.domain.SupervisoryNode;
import org.openlmis.referencedata.web.FacilityController;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true, exclude = {"serviceUrl"})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SupervisoryNodeBaseDto extends BaseDto implements SupervisoryNode.Exporter,
    SupervisoryNode.Importer {

  @Setter
  @Getter(AccessLevel.PROTECTED)
  @JsonIgnore
  private String serviceUrl;

  private String code;
  private ObjectReferenceDto facility;
  private String name;
  private String description;
  private Map<String, Object> extraData;

  SupervisoryNodeBaseDto(UUID id) {
    setId(id);
  }

  @JsonSetter
  public void setFacility(ObjectReferenceDto facility) {
    this.facility = facility;
  }

  @JsonIgnore
  @Override
  public void setFacility(Facility facility) {
    this.facility = null != facility
        ? new ObjectReferenceDto(serviceUrl, FacilityController.RESOURCE_PATH, facility.getId())
        : null;
  }

  @Override
  public void setParentNode(SupervisoryNode parentNode) {
    // unsupported operation
  }

  @Override
  public void setPartnerNodeOf(SupervisoryNode partnerNodeOf) {
    // unsupported operation
  }

  @Override
  public void assignChildNodes(Set<SupervisoryNode> childNodes) {
    // unsupported operation
  }

  @Override
  public void setRequisitionGroup(RequisitionGroup requisitionGroup) {
    // unsupported operation
  }

  @Override
  public void assignPartnerNodes(Set<SupervisoryNode> partnerNodes) {
    // unsupported operation
  }

  @Override
  @JsonIgnore
  public UUID getFacilityId() {
    return Optional
        .ofNullable(facility)
        .map(BaseDto::getId)
        .orElse(null);
  }

  @Override
  @JsonIgnore
  public UUID getParentNodeId() {
    // unsupported operation
    return null;
  }

  @Override
  @JsonIgnore
  public UUID getPartnerNodeOfId() {
    // unsupported operation
    return null;
  }

  @Override
  @JsonIgnore
  public Set<UUID> getChildNodeIds() {
    // unsupported operation
    return Collections.emptySet();
  }

  @Override
  @JsonIgnore
  public Set<UUID> getPartnerNodeIds() {
    // unsupported operation
    return Collections.emptySet();
  }

  @Override
  @JsonIgnore
  public UUID getRequisitionGroupId() {
    // unsupported operation
    return null;
  }
}
