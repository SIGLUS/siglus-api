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

import java.util.Collection;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.openlmis.requisition.dto.UserDto;
import org.openlmis.requisition.service.BaseCommunicationService;

public class SupervisingUsersReferenceDataServiceTest extends UserReferenceDataServiceTest {

  private SupervisingUsersReferenceDataService service;

  @Override
  protected BaseCommunicationService<UserDto> getService() {
    return new SupervisingUsersReferenceDataService();
  }

  @Override
  @Before
  public void setUp() {
    super.setUp();
    service = (SupervisingUsersReferenceDataService) prepareService();
  }

  @Test
  public void testFindAll() {
    // given
    UUID right = UUID.randomUUID();
    UUID program = UUID.randomUUID();
    UUID supervisoryNode = UUID.randomUUID();

    // when
    UserDto dto = mockArrayResponseEntityAndGetDto();
    Collection<UserDto> result = service.findAll(supervisoryNode, right, program);

    // then
    assertThat(result, hasSize(1));
    assertTrue(result.contains(dto));

    verifyArrayRequest()
        .isGetRequest()
        .hasAuthHeader()
        .hasEmptyBody()
        .hasQueryParameter("rightId", right)
        .hasQueryParameter("programId", program);
  }
}
