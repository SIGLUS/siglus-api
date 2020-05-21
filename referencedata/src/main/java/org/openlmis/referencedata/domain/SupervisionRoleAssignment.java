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

import static java.util.Collections.singleton;
import static org.openlmis.referencedata.domain.RightType.SUPERVISION;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.javers.core.metamodel.annotation.TypeName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.profiler.Profiler;

@Entity
@DiscriminatorValue("supervision")
@NoArgsConstructor
@TypeName("SupervisionRoleAssignment")
public class SupervisionRoleAssignment extends RoleAssignment {

  private static final Logger LOGGER = LoggerFactory.getLogger(SupervisionRoleAssignment.class);

  @ManyToOne
  @JoinColumn(name = "programid")
  @Getter
  private Program program;

  @ManyToOne
  @JoinColumn(name = "supervisorynodeid")
  @Getter
  private SupervisoryNode supervisoryNode;

  private SupervisionRoleAssignment(Role role, User user) {
    super(role, user);
  }

  /**
   * Constructor for home facility supervision. Must always have a role, a user and a program.
   *
   * @param role    the role being assigned
   * @param user    the user to which the role is being assigned
   * @param program the program where the role applies
   * @throws org.openlmis.referencedata.exception.ValidationMessageException if role passed in
   *      has rights which are not an acceptable right type
   */
  public SupervisionRoleAssignment(Role role, User user, Program program) {
    this(role, user);
    this.program = program;
    addRightAssignments();
  }

  /**
   * Constructor for supervisory supervision. Must always have a role, a user, a program and a
   * supervisory node.
   *
   * @param role            the role being assigned
   * @param user            the user to which the role is being assigned
   * @param program         the program where the role applies
   * @param supervisoryNode the supervisory node where the role applies
   * @throws org.openlmis.referencedata.exception.ValidationMessageException if role passed in
   *      has rights which are not an acceptable right type
   */
  public SupervisionRoleAssignment(Role role, User user, Program program,
                                   SupervisoryNode supervisoryNode) {
    this(role, user);
    this.program = program;
    this.supervisoryNode = supervisoryNode;
    addRightAssignments();
  }

  @Override
  protected Set<RightType> getAcceptableRightTypes() {
    return singleton(SUPERVISION);
  }

  /**
   * Check if this role assignment has a right based on specified criteria. For supervision,
   * check also that program matches and facility was found, either from the supervisory node or
   * the user's home facility.
   */
  @Override
  public boolean hasRight(RightQuery rightQuery) {
    Profiler profiler = new Profiler("HAS_RIGHT_FOR_RIGHT_QUERY");
    profiler.setLogger(LOGGER);

    profiler.start("SUPERVISES");
    boolean facilityFound;
    if (supervisoryNode != null) {
      profiler.start("CHECK_FOR_NODE");
      facilityFound = supervisoryNode.supervises(rightQuery.getFacility(), rightQuery.getProgram());
    } else if (user.getHomeFacilityId() != null && rightQuery.getFacility() != null) {
      profiler.start("CHECK_FOR_HOME_FACILITY");
      facilityFound = user.getHomeFacilityId().equals(rightQuery.getFacility().getId());
    } else {
      facilityFound = false;
    }

    profiler.start("CONTAINS_RIGHT_CHECK");
    boolean roleContainsRight = role.contains(rightQuery.getRight());

    profiler.start("CONTAINS_PROGRAM_CHECK");
    boolean programMatches = program.equals(rightQuery.getProgram());

    profiler.stop().log();

    return roleContainsRight && programMatches && facilityFound;
  }

  private void addRightAssignments() {
    if (null != supervisoryNode) {
      Set<Facility> supervisedFacilities = supervisoryNode.getAllSupervisedFacilities(program);
      for (Right right : role.getRights()) {
        for (Facility facility : supervisedFacilities) {
          user.addRightAssignment(right.getName(), facility.getId(), program.getId());
        }
      }
    } else if (user.getHomeFacilityId() != null) {
      for (Right right : role.getRights()) {
        user.addRightAssignment(right.getName(), user.getHomeFacilityId(), program.getId());
      }
    }
  }

  /**
   * Get all facilities being supervised by this role assignment, by right and program.
   *
   * @param right   right to check
   * @param program program to check
   * @return set of supervised facilities
   */
  public Set<Facility> getSupervisedFacilities(Right right, Program program) {
    Profiler profiler = new Profiler("GET_SUPERVISED_FACILITIES_FOR_RIGHT_AND_PROGRAM");
    profiler.setLogger(LOGGER);

    Set<Facility> possibleFacilities = new HashSet<>();
    
    if (supervisoryNode == null) {
      return possibleFacilities;
    }

    profiler.start("GET_ALL_SUPERVISED_FACILITIES_FROM_NODE");
    possibleFacilities = supervisoryNode.getAllSupervisedFacilities(program);

    profiler.start("HAS_RIGHT_CHECK");
    Set<Facility> facilities = possibleFacilities.stream()
        .filter(possibleFacility -> hasRight(new RightQuery(right, program, possibleFacility)))
        .collect(Collectors.toSet());

    profiler.stop().log();

    return facilities;
  }

  /**
   * Export this object to the specified exporter (DTO).
   *
   * @param exporter exporter to export to
   */
  public void export(Exporter exporter) {
    exporter.setRole(role);
    exporter.setProgram(program);
    if (supervisoryNode != null) {
      exporter.setSupervisoryNode(supervisoryNode);
    }
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof SupervisionRoleAssignment)) {
      return false;
    }
    if (!super.equals(obj)) {
      return false;
    }
    SupervisionRoleAssignment that = (SupervisionRoleAssignment) obj;
    return Objects.equals(role, that.role)
        && Objects.equals(user, that.user)
        && Objects.equals(program, that.program)
        && Objects.equals(supervisoryNode, that.supervisoryNode);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), program, supervisoryNode);
  }

  public interface Exporter extends RoleAssignment.Exporter {
    void setProgram(Program program);

    void setSupervisoryNode(SupervisoryNode supervisoryNode);
  }
}
