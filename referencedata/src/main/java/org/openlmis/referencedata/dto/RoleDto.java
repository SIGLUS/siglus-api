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

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import org.openlmis.referencedata.domain.Right;
import org.openlmis.referencedata.domain.Role;

public class RoleDto extends BaseDto implements Role.Exporter, Role.Importer {

  @Getter
  @Setter
  private String name;

  @Getter
  @Setter
  private String description;

  private Set<RightDto> rights = new HashSet<>();

  @Getter
  @Setter
  private Long count;

  @Override
  public void setRights(Set<Right> rights) {
    for (Right right : rights) {
      RightDto rightDto = new RightDto();
      right.export(rightDto);
      this.rights.add(rightDto);
    }
  }

  @Override
  public Set<Right.Importer> getRights() {
    Set<Right.Importer> rights = new HashSet<>();
    rights.addAll(this.rights);
    return rights;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof RoleDto)) {
      return false;
    }
    RoleDto roleDto = (RoleDto) obj;
    return Objects.equals(name, roleDto.name)
        && Objects.equals(rights, roleDto.rights);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, rights);
  }
}
