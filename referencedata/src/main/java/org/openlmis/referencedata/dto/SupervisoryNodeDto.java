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
import org.openlmis.referencedata.domain.RequisitionGroup;
import org.openlmis.referencedata.domain.SupervisoryNode;
import org.openlmis.referencedata.web.RequisitionGroupController;
import org.openlmis.referencedata.web.SupervisoryNodeController;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class SupervisoryNodeDto extends SupervisoryNodeBaseDto {

  @Getter
  private ObjectReferenceDto parentNode;

  @Getter
  private ObjectReferenceDto requisitionGroup;

  @Getter
  @Setter
  private Set<ObjectReferenceDto> childNodes;

  @Getter
  private ObjectReferenceDto partnerNodeOf;

  @Getter
  @Setter
  private Set<ObjectReferenceDto> partnerNodes;

  public SupervisoryNodeDto(UUID id) {
    super(id);
  }

  @JsonIgnore
  @Override
  public void setParentNode(SupervisoryNode parentNode) {
    this.parentNode = null != parentNode
        ? new ObjectReferenceDto(
            getServiceUrl(), SupervisoryNodeController.RESOURCE_PATH, parentNode.getId())
        : null;
  }

  @JsonSetter("parentNode")
  public void setParentNode(ObjectReferenceDto parentNode) {
    this.parentNode = parentNode;
  }

  @JsonIgnore
  @Override
  public void setPartnerNodeOf(SupervisoryNode partnerNodeOf) {
    this.partnerNodeOf = null != partnerNodeOf
        ? new ObjectReferenceDto(
        getServiceUrl(), SupervisoryNodeController.RESOURCE_PATH, partnerNodeOf.getId())
        : null;
  }

  @JsonSetter("partnerNodeOf")
  public void setPartnerNodeOf(ObjectReferenceDto partnerNodeOf) {
    this.partnerNodeOf = partnerNodeOf;
  }

  @JsonIgnore
  @Override
  public void assignChildNodes(Set<SupervisoryNode> childNodes) {
    this.childNodes = Optional
        .ofNullable(childNodes)
        .orElse(Collections.emptySet())
        .stream()
        .map(node -> new ObjectReferenceDto(
            getServiceUrl(), SupervisoryNodeController.RESOURCE_PATH, node.getId()))
        .collect(Collectors.toSet());
  }

  @Override
  public void assignPartnerNodes(Set<SupervisoryNode> partnerNodes) {
    this.partnerNodes = Optional
        .ofNullable(partnerNodes)
        .orElse(Collections.emptySet())
        .stream()
        .map(node -> new ObjectReferenceDto(
            getServiceUrl(), SupervisoryNodeController.RESOURCE_PATH, node.getId()))
        .collect(Collectors.toSet());
  }

  @JsonIgnore
  @Override
  public void setRequisitionGroup(RequisitionGroup requisitionGroup) {
    this.requisitionGroup = null != requisitionGroup
        ? new ObjectReferenceDto(
            getServiceUrl(), RequisitionGroupController.RESOURCE_PATH, requisitionGroup.getId())
        : null;
  }

  @JsonSetter("requisitionGroup")
  public void setRequisitionGroup(ObjectReferenceDto requisitionGroup) {
    this.requisitionGroup = requisitionGroup;
  }

  @Override
  @JsonIgnore
  public UUID getParentNodeId() {
    return Optional
        .ofNullable(parentNode)
        .map(BaseDto::getId)
        .orElse(null);
  }

  @Override
  public UUID getPartnerNodeOfId() {
    return Optional
        .ofNullable(partnerNodeOf)
        .map(BaseDto::getId)
        .orElse(null);
  }

  @Override
  @JsonIgnore
  public Set<UUID> getChildNodeIds() {
    return Optional
        .ofNullable(childNodes)
        .orElse(Collections.emptySet())
        .stream()
        .map(BaseDto::getId)
        .collect(Collectors.toSet());
  }

  @Override
  public Set<UUID> getPartnerNodeIds() {
    return Optional
        .ofNullable(partnerNodes)
        .orElse(Collections.emptySet())
        .stream()
        .map(BaseDto::getId)
        .collect(Collectors.toSet());
  }

  @Override
  @JsonIgnore
  public UUID getRequisitionGroupId() {
    return Optional
        .ofNullable(requisitionGroup)
        .map(BaseDto::getId)
        .orElse(null);
  }

}
