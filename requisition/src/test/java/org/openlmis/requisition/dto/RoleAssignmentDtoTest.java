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

package org.openlmis.requisition.dto;

import java.util.List;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.junit.Test;
import org.openlmis.requisition.testutils.DtoGenerator;

public class RoleAssignmentDtoTest {

  @Test
  public void equalsContract() {
    List<RoleDto> roles = DtoGenerator.of(RoleDto.class, 2);
    List<UserDto> users = DtoGenerator.of(UserDto.class, 2);

    EqualsVerifier
        .forClass(RoleAssignmentDto.class)
        .withPrefabValues(RoleDto.class, roles.get(0), roles.get(1))
        .withPrefabValues(UserDto.class, users.get(0), users.get(1))
        .withRedefinedSuperclass()
        .suppress(Warning.NONFINAL_FIELDS) // fields in DTO cannot be final
        .verify();
  }

}
