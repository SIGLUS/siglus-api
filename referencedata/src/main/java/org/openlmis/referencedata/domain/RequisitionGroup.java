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

package org.openlmis.referencedata.domain;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.javers.core.metamodel.annotation.DiffIgnore;
import org.javers.core.metamodel.annotation.TypeName;

/**
 * RequisitionGroup represents a group of facilities which follow a particular schedule for a
 * program. It also defines the contract for creation/upload of RequisitionGroup.
 */
@Entity
@Table(name = "requisition_groups", schema = "referencedata")
@NoArgsConstructor
@AllArgsConstructor
@TypeName("RequisitionGroup")
public class RequisitionGroup extends BaseEntity {

  @Column(unique = true, nullable = false, columnDefinition = "text")
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

  @OneToOne
  @JoinColumn(name = "supervisoryNodeId", nullable = false)
  @Getter
  @Setter
  private SupervisoryNode supervisoryNode;

  @OneToMany(mappedBy = "requisitionGroup", cascade = CascadeType.ALL, orphanRemoval = true)
  @Getter
  @Setter
  @DiffIgnore
  private List<RequisitionGroupProgramSchedule> requisitionGroupProgramSchedules;

  @ManyToMany(fetch = FetchType.EAGER)
  @JoinTable(name = "requisition_group_members",
      joinColumns = @JoinColumn(name = "requisitiongroupid", nullable = false),
      inverseJoinColumns = @JoinColumn(name = "facilityid", nullable = false))
  @Getter
  @Setter
  @DiffIgnore
  private Set<Facility> memberFacilities;

  /**
   * Create a new requisition group with a specified supervisory node, program schedules and
   * facilities.
   *
   * @param code            specified code
   * @param name            specified name
   * @param supervisoryNode specified supervisory node
   */
  public RequisitionGroup(String code, String name, SupervisoryNode supervisoryNode) {
    this.code = Objects.requireNonNull(code);
    this.name = Objects.requireNonNull(name);
    this.supervisoryNode = Objects.requireNonNull(supervisoryNode);
    this.requisitionGroupProgramSchedules = new ArrayList<>();
    this.memberFacilities = new HashSet<>();
  }

  /**
   * Static factory method for constructing a new requisition group using an importer (DTO).
   *
   * @param importer the requisition group importer (DTO)
   */
  public static RequisitionGroup newRequisitionGroup(Importer importer) {
    SupervisoryNode supervisoryNode = null;
    if (importer.getSupervisoryNode() != null) {
      supervisoryNode = SupervisoryNode.newSupervisoryNode(importer.getSupervisoryNode());
    }

    RequisitionGroup newRequisitionGroup = new RequisitionGroup(importer.getCode(),
        importer.getName(), supervisoryNode);
    newRequisitionGroup.id = importer.getId();
    newRequisitionGroup.description = importer.getDescription();

    if (importer.getRequisitionGroupProgramSchedules() != null) {
      List<RequisitionGroupProgramSchedule> requisitionGroupProgramSchedules = new ArrayList<>();

      for (RequisitionGroupProgramSchedule.Importer scheduleImporter :
          importer.getRequisitionGroupProgramSchedules()) {
        RequisitionGroupProgramSchedule newRequisitionGroupProgramSchedule =
            RequisitionGroupProgramSchedule.newRequisitionGroupProgramSchedule(scheduleImporter);
        newRequisitionGroupProgramSchedule.setRequisitionGroup(newRequisitionGroup);
        requisitionGroupProgramSchedules.add(newRequisitionGroupProgramSchedule);
      }

      newRequisitionGroup.requisitionGroupProgramSchedules = requisitionGroupProgramSchedules;
    }

    if (importer.getMemberFacilities() != null) {
      Set<Facility> memberFacilities = new HashSet<>();

      for (Facility.Importer facilityImporter : importer.getMemberFacilities()) {
        memberFacilities.add(Facility.newFacility(facilityImporter));
      }

      newRequisitionGroup.memberFacilities = memberFacilities;
    }

    return newRequisitionGroup;
  }

  /**
   * Check to see if this requisition group supports the specified program.
   */
  public boolean supports(Program program) {
    return requisitionGroupProgramSchedules.stream().anyMatch(
        rgps -> rgps.getProgram().equals(program));
  }

  /**
   * Copy properties from the given instance.
   *
   * @param requisitionGroup an instance from which properties will be used to update current
   *                         instance
   */
  public void updateFrom(RequisitionGroup requisitionGroup) {
    code = requisitionGroup.getCode();
    name = requisitionGroup.getName();
    description = requisitionGroup.getDescription();
    supervisoryNode = requisitionGroup.getSupervisoryNode();
    memberFacilities = requisitionGroup.getMemberFacilities();

    updateProgramSchedulesListFrom(requisitionGroup.getRequisitionGroupProgramSchedules());
  }

  private void updateProgramSchedulesListFrom(List<RequisitionGroupProgramSchedule> schedules) {
    if (requisitionGroupProgramSchedules == null) {
      requisitionGroupProgramSchedules = new ArrayList<>();
    }

    List<UUID> existentIds = requisitionGroupProgramSchedules
        .stream().map(BaseEntity::getId).collect(Collectors.toList());

    List<UUID> replacementIds = schedules
        .stream().map(BaseEntity::getId).collect(Collectors.toList());

    List<RequisitionGroupProgramSchedule> added = schedules
        .stream()
        .filter(schedule -> !existentIds.contains(schedule.getId()))
        .collect(Collectors.toList());

    List<RequisitionGroupProgramSchedule> removed = requisitionGroupProgramSchedules
        .stream()
        .filter(schedule -> !replacementIds.contains(schedule.getId()))
        .collect(Collectors.toList());

    requisitionGroupProgramSchedules.removeAll(removed);

    for (RequisitionGroupProgramSchedule schedule : requisitionGroupProgramSchedules) {
      Optional<RequisitionGroupProgramSchedule> replacement = schedules
          .stream()
          .filter(obj -> obj.getId().equals(schedule.getId()))
          .findFirst();

      if (replacement.isPresent()) {
        schedule.updateFrom(replacement.get());
      }
    }

    requisitionGroupProgramSchedules.addAll(added);
  }

  /**
   * Export this object to the specified exporter (DTO).
   *
   * @param exporter exporter to export to
   */
  public void export(Exporter exporter) {
    exporter.setId(id);
    exporter.setCode(code);
    exporter.setName(name);
    exporter.setDescription(description);
    exporter.setSupervisoryNode(supervisoryNode);
    exporter.setRequisitionGroupProgramSchedules(requisitionGroupProgramSchedules);
    exporter.setMemberFacilities(memberFacilities);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof RequisitionGroup)) {
      return false;
    }
    RequisitionGroup that = (RequisitionGroup) obj;
    return Objects.equals(code, that.code);
  }

  @Override
  public int hashCode() {
    return Objects.hash(code);
  }

  public interface Exporter {
    void setId(UUID id);

    void setCode(String code);

    void setName(String name);

    void setDescription(String description);

    void setSupervisoryNode(SupervisoryNode supervisoryNode);

    void setRequisitionGroupProgramSchedules(List<RequisitionGroupProgramSchedule> schedules);

    void setMemberFacilities(Set<Facility> memberFacilities);
  }

  public interface Importer {
    UUID getId();

    String getCode();

    String getName();

    String getDescription();

    SupervisoryNode.Importer getSupervisoryNode();

    List<RequisitionGroupProgramSchedule.Importer> getRequisitionGroupProgramSchedules();

    Set<Facility.Importer> getMemberFacilities();
  }
}
