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
import org.openlmis.referencedata.domain.DirectRoleAssignment;
import org.openlmis.referencedata.domain.Role;
import org.openlmis.referencedata.domain.User;

public class DirectRoleAssignmentDataBuilder {

  private UUID id;
  private Role role;
  private User user;

  /**
   * Builds instance of {@link DirectRoleAssignmentDataBuilder} with sample data.
   */
  public DirectRoleAssignmentDataBuilder() {
    id = UUID.randomUUID();
    role = new RoleDataBuilder().build();
    user = new UserDataBuilder().build();
  }

  /**
   * Builds instance of {@link DirectRoleAssignment}.
   */
  public DirectRoleAssignment build() {
    DirectRoleAssignment directRoleAssignment = new DirectRoleAssignment(role, user);
    directRoleAssignment.setId(id);
    return directRoleAssignment;
  }

  /**
   * Builds instance of {@link DirectRoleAssignment} without id.
   */
  public DirectRoleAssignment buildAsNew() {
    return this.withoutId().build();
  }

  public DirectRoleAssignmentDataBuilder withoutId() {
    this.id = null;
    return this;
  }

  public DirectRoleAssignmentDataBuilder withRole(Role role) {
    this.role = role;
    return this;
  }

  public DirectRoleAssignmentDataBuilder withUser(User user) {
    this.user = user;
    return this;
  }
}
