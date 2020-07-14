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

package org.siglus.common.domain.referencedata;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.javers.core.metamodel.annotation.DiffIgnore;
import org.javers.core.metamodel.annotation.TypeName;
import org.siglus.common.domain.BaseEntity;
import org.siglus.common.domain.referencedata.ExtraDataEntity.ExtraDataExporter;
import org.siglus.common.domain.referencedata.ExtraDataEntity.ExtraDataImporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.profiler.Profiler;

@Entity
@Table(name = "supervisory_nodes", schema = "referencedata")
@NoArgsConstructor
@AllArgsConstructor
@TypeName("SupervisoryNode")
@SuppressWarnings("PMD.TooManyMethods")
public class SupervisoryNode extends BaseEntity {

  private static final Logger LOGGER = LoggerFactory.getLogger(SupervisoryNode.class);

  @Column(nullable = false, unique = true, columnDefinition = "text")
  @Getter
  @Setter
  private String code;

  @Column(nullable = false, columnDefinition = "text")
  @Getter
  @Setter
  private String name;

  @Column(columnDefinition = "text")
  @Getter
  @Setter
  private String description;

  @ManyToOne
  @JoinColumn(name = "facilityid")
  @Getter
  @Setter
  private Facility facility;

  @ManyToOne
  @JoinColumn(name = "parentid")
  @Getter
  private SupervisoryNode parentNode;

  @ManyToOne
  @JoinColumn(name = "partnerId")
  @Getter
  private SupervisoryNode partnerNodeOf;

  @OneToMany(mappedBy = "parentNode")
  @Getter
  @DiffIgnore
  private Set<SupervisoryNode> childNodes;

  @OneToMany(mappedBy = "partnerNodeOf")
  @Getter
  @DiffIgnore
  private Set<SupervisoryNode> partnerNodes;

  @OneToOne(mappedBy = "supervisoryNode", fetch = FetchType.LAZY)
  @Getter
  @Setter
  private RequisitionGroup requisitionGroup;

  @Embedded
  private ExtraDataEntity extraData = new ExtraDataEntity();

  /**
   * Assign this node's parent supervisory node. Also add this node to the parent's set of child
   * nodes.
   *
   * @param parentNode parent supervisory node to assign.
   */
  public void assignParentNode(SupervisoryNode parentNode) {
    if (null != parentNode) {
      this.parentNode = parentNode;
      parentNode.childNodes.add(this);
    } else if (null != this.parentNode && null != this.parentNode.childNodes) {
      this.parentNode.childNodes.remove(this);
      this.parentNode = null;
    }
  }

  /**
   * Assign a partner node of for this supervisory node.
   */
  public void assignPartnerNodeOf(SupervisoryNode partnerNodeOf) {
    if (null != partnerNodeOf) {
      this.partnerNodeOf = partnerNodeOf;
      partnerNodeOf.partnerNodes.add(this);
    } else if (null != this.partnerNodeOf) {
      this.partnerNodeOf.partnerNodes.remove(this);
      this.partnerNodeOf = null;
    }
  }

  /**
   * Get all facilities being supervised by this supervisory node, by program.
   * <p/>
   * Note, this does not get the facility attached to this supervisory node. "All supervised
   * facilities" means all facilities supervised by this node and all recursive child nodes.
   *
   * @param program program to check, can be null.
   * @return all supervised facilities
   */
  public Set<Facility> getAllSupervisedFacilities(Program program) {
    Profiler profiler = new Profiler("SUPERVISORY_NODE_GET_FACILITIES_FOR_PROGRAM");
    profiler.setLogger(LOGGER);

    Set<Facility> supervisedFacilities = new HashSet<>();

    profiler.start("CHECK_IF_REQ_GROUP_SUPPORTS_PROGRAM");

    if (requisitionGroup != null && (null == program || requisitionGroup.supports(program))) {
      profiler.start("REQ_GROUP_GET_MEMBER_FACILITIES");
      Set<Facility> facilities = requisitionGroup
          .getMemberFacilities()
          .stream()
          .filter(member -> null == program || member.supports(program))
          .collect(Collectors.toSet());
      supervisedFacilities.addAll(facilities);
    }

    profiler.start("GET_FACILITIES_FROM_CHILD_NODES");
    if (childNodes != null) {
      for (SupervisoryNode childNode : childNodes) {
        profiler.start("GET_SUPERVISED_FACILITIES_FROM_NODE");
        supervisedFacilities.addAll(childNode.getAllSupervisedFacilities(program));
      }
    }

    profiler.stop().log();

    return supervisedFacilities;
  }

  /**
   * Check to see if this supervisory node supervises the specified facility, by program.
   */
  public boolean supervises(Facility facility, Program program) {
    return getAllSupervisedFacilities(program).contains(facility);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof SupervisoryNode)) {
      return false;
    }
    SupervisoryNode that = (SupervisoryNode) obj;
    return Objects.equals(code, that.code);
  }

  @Override
  public int hashCode() {
    return Objects.hash(code);
  }

  public interface Exporter extends BaseExporter, ExtraDataExporter {

    void setCode(String code);

    void setName(String name);

    void setDescription(String description);

    void setFacility(Facility facility);

    void setParentNode(SupervisoryNode parentNode);

    void setPartnerNodeOf(SupervisoryNode partnerNodeOf);

    void assignChildNodes(Set<SupervisoryNode> childNodes);

    void assignPartnerNodes(Set<SupervisoryNode> partnerNodes);

    void setRequisitionGroup(RequisitionGroup requisitionGroup);
  }

  public interface Importer extends BaseImporter, ExtraDataImporter {

    String getCode();

    String getName();

    String getDescription();

    UUID getFacilityId();

    UUID getParentNodeId();

    UUID getPartnerNodeOfId();

    Set<UUID> getChildNodeIds();

    UUID getRequisitionGroupId();

    Set<UUID> getPartnerNodeIds();
  }
}
