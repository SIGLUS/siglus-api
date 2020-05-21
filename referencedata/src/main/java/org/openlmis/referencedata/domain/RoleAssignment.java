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

import static org.openlmis.referencedata.util.messagekeys.RoleAssignmentMessageKeys.ERROR_TYPE_NOT_ACCEPTABLE;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import javax.persistence.ColumnResult;
import javax.persistence.ConstructorResult;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedNativeQueries;
import javax.persistence.NamedNativeQuery;
import javax.persistence.SqlResultSetMapping;
import javax.persistence.SqlResultSetMappings;
import javax.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.openlmis.referencedata.dto.RoleAssignmentDto;
import org.openlmis.referencedata.exception.ValidationMessageException;
import org.openlmis.referencedata.repository.CountResource;
import org.openlmis.referencedata.util.Message;

@Entity
@Table(name = "role_assignments", schema = "referencedata")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "type", discriminatorType = DiscriminatorType.STRING)
@DiscriminatorValue("abstract")
@NoArgsConstructor
@NamedNativeQueries({
    @NamedNativeQuery(name = "RoleAssignment.findByUser",
        query = "SELECT ra.roleid"
            + "   , ra.programid"
            + "   , ra.supervisorynodeid"
            + "   , ra.warehouseid"
            + " FROM referencedata.role_assignments ra"
            + " WHERE ra.userid = :userId",
        resultSetMapping = "RoleAssignment.idResource"),
    @NamedNativeQuery(name = "RoleAssignment.countUsersAssignedToRoles",
        query = "SELECT ra.roleid as id, COUNT(DISTINCT ra.userid) as count"
            + " FROM referencedata.role_assignments ra"
            + " GROUP BY roleid",
        resultSetMapping = "RoleAssignment.countResource")
    })
@SqlResultSetMappings({
    @SqlResultSetMapping(
        name = "RoleAssignment.idResource",
        classes = {
            @ConstructorResult(
                targetClass = RoleAssignmentDto.class,
                columns = {
                    @ColumnResult(name = "roleid", type = UUID.class),
                    @ColumnResult(name = "programid", type = UUID.class),
                    @ColumnResult(name = "supervisorynodeid", type = UUID.class),
                    @ColumnResult(name = "warehouseid", type = UUID.class)
                }
            )
        }
    ),
    @SqlResultSetMapping(
        name = "RoleAssignment.countResource",
        classes = {
            @ConstructorResult(
                targetClass = CountResource.class,
                columns = {
                    @ColumnResult(name = "id", type = UUID.class),
                    @ColumnResult(name = "count", type = Long.class)
                }
            )
        }
    )
    })
public abstract class RoleAssignment extends BaseEntity {

  @ManyToOne
  @JoinColumn(name = "roleid")
  @Getter
  protected Role role;

  @ManyToOne
  @JoinColumn(name = "userid")
  protected User user;

  /**
   * Default constructor. Must always have a role and a user.
   *
   * @param role the role being assigned
   * @param user the user to which the role is being assigned
   * @throws ValidationMessageException if role passed in has rights which are not an acceptable
   *      right type
   */
  public RoleAssignment(Role role, User user) {
    Set<RightType> acceptableRightTypes = getAcceptableRightTypes();
    boolean roleTypeAcceptable = acceptableRightTypes.stream()
        .anyMatch(rightType -> rightType == role.getRightType());
    if (!roleTypeAcceptable) {
      throw new ValidationMessageException(
          new Message(ERROR_TYPE_NOT_ACCEPTABLE, role.getRightType(), acceptableRightTypes));
    }

    this.role = role;
    this.user = user;
  }

  protected abstract Set<RightType> getAcceptableRightTypes();

  public abstract boolean hasRight(RightQuery rightQuery);

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof RoleAssignment)) {
      return false;
    }
    RoleAssignment that = (RoleAssignment) obj;
    return Objects.equals(role, that.role)
        && Objects.equals(user, that.user);
  }

  @Override
  public int hashCode() {
    return Objects.hash(role, user);
  }

  public interface Exporter {
    void setRole(Role role);
  }
}
