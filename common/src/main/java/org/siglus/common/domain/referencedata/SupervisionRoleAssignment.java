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

import static java.util.Collections.singleton;
import static org.siglus.common.domain.referencedata.RightType.SUPERVISION;

import java.util.Objects;
import java.util.Set;
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
