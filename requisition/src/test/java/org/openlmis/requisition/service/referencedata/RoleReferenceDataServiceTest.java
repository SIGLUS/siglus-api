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

package org.openlmis.requisition.service.referencedata;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.openlmis.requisition.dto.RoleDto;
import org.openlmis.requisition.service.BaseCommunicationService;
import org.openlmis.requisition.testutils.RoleDtoDataBuilder;

public class RoleReferenceDataServiceTest extends BaseReferenceDataServiceTest<RoleDto> {

  private RoleReferenceDataService service;

  @Override
  protected RoleDto generateInstance() {
    return new RoleDtoDataBuilder().buildAsDto();
  }

  @Override
  protected BaseCommunicationService<RoleDto> getService() {
    return new RoleReferenceDataService();
  }

  @Override
  @Before
  public void setUp() {
    super.setUp();
    service = (RoleReferenceDataService) prepareService();
  }

  @Test
  public void shouldFindRolesByRightId() {
    // given
    UUID rightId = UUID.randomUUID();

    // when
    RoleDto dto = mockArrayResponseEntityAndGetDto();
    List<RoleDto> result = service.search(rightId);

    // thenA
    assertThat(result, hasSize(1));
    assertTrue(result.contains(dto));

    verifyArrayRequest()
        .isGetRequest()
        .hasAuthHeader()
        .hasEmptyBody()
        .hasQueryParameter("rightId", rightId);
  }

}
