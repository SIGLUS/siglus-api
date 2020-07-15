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

import java.util.Objects;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@DiscriminatorValue("supervision")
@NoArgsConstructor
public class SupervisionRoleAssignment extends RoleAssignment {

  @ManyToOne
  @JoinColumn(name = "programid")
  @Getter
  private Program program;

  @ManyToOne
  @JoinColumn(name = "supervisorynodeid")
  @Getter
  private SupervisoryNode supervisoryNode;

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

}
