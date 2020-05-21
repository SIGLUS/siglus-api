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

import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.HashSet;
import java.util.Map;
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
import org.openlmis.referencedata.domain.SupervisoryNode;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true, exclude = "serviceUrl")
public final class SupervisoryNodeObjectReferenceDto extends ObjectReferenceDto implements
    SupervisoryNode.Importer, SupervisoryNode.Exporter {

  private static final String SUPERVISORY_NODES = "supervisoryNodes";

  @JsonIgnore
  private String serviceUrl;

  private SupervisoryNodeObjectReferenceDto parentNode;
  private Set<SupervisoryNodeObjectReferenceDto> childNodes = new HashSet<>();
  private SupervisoryNodeObjectReferenceDto partnerNodeOf;
  private Set<SupervisoryNodeObjectReferenceDto> partnerNodes = new HashSet<>();
  private FacilityObjectReferenceDto facility;
  private RequisitionGroupObjectReferenceDto requisitionGroup;
  private String code;
  private String name;
  private String description;
  private Map<String, Object> extraData;

  public SupervisoryNodeObjectReferenceDto(UUID id, String serviceUrl) {
    super(serviceUrl, SUPERVISORY_NODES, id);
    this.serviceUrl = serviceUrl;
  }

  @Override
  public UUID getFacilityId() {
    if (null != facility) {
      return facility.getId();
    }
    return null;
  }

  @Override
  public UUID getParentNodeId() {
    if (null != parentNode) {
      return parentNode.getId();
    }
    return null;
  }

  @Override
  public UUID getPartnerNodeOfId() {
    if (null != partnerNodeOf) {
      return partnerNodeOf.getId();
    }
    return null;
  }

  @Override
  public Set<UUID> getChildNodeIds() {
    if (!isEmpty(childNodes)) {
      return childNodes.stream().map(SupervisoryNodeObjectReferenceDto::getId).collect(toSet());
    }
    return emptySet();
  }

  @Override
  public UUID getRequisitionGroupId() {
    if (null != requisitionGroup) {
      return requisitionGroup.getId();
    }
    return null;
  }

  @Override
  public Set<UUID> getPartnerNodeIds() {
    if (!isEmpty(partnerNodes)) {
      return partnerNodes.stream().map(SupervisoryNodeObjectReferenceDto::getId).collect(toSet());
    }
    return emptySet();
  }

  @Override
  public void setFacility(Facility facility) {
    if (null != facility) {
      this.facility = new FacilityObjectReferenceDto(facility.getId(), serviceUrl);
    }
  }

  @Override
  public void setParentNode(SupervisoryNode parentNode) {
    if (null != parentNode) {
      this.parentNode = new SupervisoryNodeObjectReferenceDto(parentNode.getId(), serviceUrl);
    }
  }

  @Override
  public void setPartnerNodeOf(SupervisoryNode partnerNodeOf) {
    if (null != partnerNodeOf) {
      this.partnerNodeOf = new SupervisoryNodeObjectReferenceDto(partnerNodeOf.getId(), serviceUrl);
    }
  }

  @Override
  public void assignChildNodes(Set<SupervisoryNode> childNodes) {
    // unsupported
  }

  @Override
  public void assignPartnerNodes(Set<SupervisoryNode> partnerNodes) {
    // unsupported
  }

  @Override
  public void setRequisitionGroup(RequisitionGroup requisitionGroup) {
    if (null != requisitionGroup) {
      this.requisitionGroup =
          new RequisitionGroupObjectReferenceDto(requisitionGroup.getId(), serviceUrl);
    }
  }
}
