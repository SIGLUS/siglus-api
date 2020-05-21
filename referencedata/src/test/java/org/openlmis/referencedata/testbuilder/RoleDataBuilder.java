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

package org.openlmis.referencedata.testbuilder;

import java.util.UUID;
import org.openlmis.referencedata.domain.Right;
import org.openlmis.referencedata.domain.Role;

public class RoleDataBuilder {

  private static int instanceNumber = 0;

  private UUID id;
  private String name;
  private Right[] rights;

  /**
   * Builds instance of {@link RoleDataBuilder} with sample data.
   */
  public RoleDataBuilder() {
    instanceNumber++;

    id = UUID.randomUUID();
    name = "Role" + instanceNumber;
    rights = new Right[]{new RightDataBuilder().build()};
  }

  /**
   * Builds instance of {@link Role}.
   */
  public Role build() {
    Role role = Role.newRole(name, rights);
    role.setId(id);
    return role;
  }

  /**
   * Builds instance of {@link Role} without id.
   */
  public Role buildAsNew() {
    return this.withoutId().build();
  }

  public RoleDataBuilder withoutId() {
    this.id = null;
    return this;
  }

  public RoleDataBuilder withName(String name) {
    this.name = name;
    return this;
  }

  public RoleDataBuilder withRights(Right... rights) {
    this.rights = rights;
    return this;
  }
}
