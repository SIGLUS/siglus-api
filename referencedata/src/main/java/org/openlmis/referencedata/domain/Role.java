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

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.Stream.concat;
import static org.openlmis.referencedata.util.messagekeys.RoleMessageKeys.ERROR_MUST_HAVE_A_RIGHT;
import static org.openlmis.referencedata.util.messagekeys.RoleMessageKeys.ERROR_RIGHTS_ARE_DIFFERENT_TYPES;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.javers.core.metamodel.annotation.DiffIgnore;
import org.javers.core.metamodel.annotation.TypeName;
import org.openlmis.referencedata.exception.ValidationMessageException;
import org.openlmis.referencedata.util.Message;

@Entity
@Table(name = "roles", schema = "referencedata")
@NoArgsConstructor
@TypeName("Role")
@SuppressWarnings({"PMD.TooManyMethods"})
public class Role extends BaseEntity {
  private static final String TEXT = "text";

  @Column(nullable = false, unique = true, columnDefinition = TEXT)
  @Getter
  @Setter
  private String name;

  @Column(columnDefinition = TEXT)
  @Getter
  @Setter
  private String description;

  @ManyToMany(fetch = FetchType.EAGER)
  @JoinTable(name = "role_rights",
      schema = "referencedata",
      joinColumns = @JoinColumn(name = "roleid", nullable = false),
      inverseJoinColumns = @JoinColumn(name = "rightid", nullable = false))
  @Getter
  @DiffIgnore
  private Set<Right> rights;

  private Role(String name, Right... rights) {
    this.name = name;
    group(rights);
  }

  /**
   * Static factory method for constructing a new role with a name and rights.
   *
   * @param name   the role name
   * @param rights the rights to group
   * @throws ValidationMessageException if the rights do not have the same right type
   */
  public static Role newRole(String name, Right... rights) {
    return new Role(name, rights);
  }

  /**
   * Static factory method for constructing a new role using an importer (DTO).
   *
   * @param importer the role importer (DTO)
   * @throws ValidationMessageException if the rights do not have the same right type
   */
  public static Role newRole(Importer importer) {
    Set<Right> importedRights = importer.getRights().stream()
        .map(Right::newRight)
        .collect(toSet());

    Role newRole = new Role(importer.getName(),
        importedRights.toArray(new Right[importedRights.size()]));
    newRole.id = importer.getId();
    newRole.description = importer.getDescription();

    return newRole;
  }

  /**
   * Group rights together and assign to this role. These rights replace any previously existing
   * rights.
   *
   * @param rights the rights to group
   * @throws ValidationMessageException if the rights do not have the same right type
   */
  public void group(Right... rights) {
    Set<Right> rightsList = new HashSet<>(asList(rights));
    if (rightsList.size() == 0) {
      throw new ValidationMessageException(
          new Message(ERROR_MUST_HAVE_A_RIGHT));
    }
    if (checkRightTypesMatch(rightsList)) {
      this.rights = rightsList;
    } else {
      throw new ValidationMessageException(
          new Message(ERROR_RIGHTS_ARE_DIFFERENT_TYPES));
    }
  }

  public RightType getRightType() {
    return rights.iterator().next().getType();
  }

  private static boolean checkRightTypesMatch(Set<Right> rightSet) {
    if (rightSet.isEmpty()) {
      return true;
    } else {
      RightType rightType = rightSet.iterator().next().getType();
      return rightSet.stream().allMatch(right -> right.getType() == rightType);
    }
  }

  /**
   * Add additional rights to the role.
   *
   * @param additionalRights the rights to add
   * @throws ValidationMessageException if the resulting rights do not have the same right type
   */
  public void add(Right... additionalRights) {
    Set<Right> allRights = concat(rights.stream(), Arrays.stream(additionalRights))
        .collect(toSet());

    if (checkRightTypesMatch(allRights)) {
      rights.addAll(Arrays.asList(additionalRights));
    } else {
      throw new ValidationMessageException(
          new Message(ERROR_RIGHTS_ARE_DIFFERENT_TYPES));
    }
  }

  /**
   * Check if the role contains a specified right. Attached rights are also checked, but only one
   * level down and it is assumed that the attached rights structure is a "tree" with no loops.
   *
   * @param right the right to check
   * @return true if the role contains the right, false otherwise
   */
  public boolean contains(Right right) {
    return rights.contains(right);
  }

  /**
   * Export this object to the specified exporter (DTO).
   *
   * @param exporter exporter to export to
   */
  public void export(Exporter exporter) {
    exporter.setId(id);
    exporter.setName(name);
    exporter.setDescription(description);
    exporter.setRights(rights);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof Role)) {
      return false;
    }
    Role role = (Role) obj;
    return Objects.equals(name, role.name)
        && Objects.equals(rights, role.rights);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, rights);
  }

  public interface Exporter {
    void setId(UUID id);

    void setName(String name);

    void setDescription(String description);

    void setRights(Set<Right> rights);
  }

  public interface Importer {
    UUID getId();

    String getName();

    String getDescription();

    Set<Right.Importer> getRights();
  }
}
