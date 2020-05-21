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

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;

import java.util.Set;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.openlmis.requisition.testutils.DtoGenerator;

public class RoleDtoTest extends ToStringContractTest<RoleDto> {

  @Override
  protected Class<RoleDto> getTestClass() {
    return RoleDto.class;
  }

  @Override
  protected void prepare(EqualsVerifier<RoleDto> verifier) {
    RightDto right = DtoGenerator.of(RightDto.class);

    verifier
        .withRedefinedSuperclass()
        .withPrefabValues(Set.class, emptySet(), singleton(right));
  }
}
