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
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.openlmis.requisition.dto.ResultDto;
import org.openlmis.requisition.dto.UserDto;
import org.openlmis.requisition.service.BaseCommunicationService;
import org.openlmis.requisition.testutils.UserDtoDataBuilder;

public class UserReferenceDataServiceTest extends BaseReferenceDataServiceTest<UserDto> {

  private UserReferenceDataService service;

  @Override
  protected UserDto generateInstance() {
    return new UserDtoDataBuilder().buildAsDto();
  }

  @Override
  protected BaseCommunicationService<UserDto> getService() {
    return new UserReferenceDataService();
  }

  @Override
  @Before
  public void setUp() {
    super.setUp();
    service = (UserReferenceDataService) prepareService();
  }

  @Test
  public void shouldFindUsers() {
    // given
    UUID right = UUID.randomUUID();
    UUID program = UUID.randomUUID();
    UUID supervisoryNode = UUID.randomUUID();
    UUID warehouse = UUID.randomUUID();

    // when
    UserDto dto = mockArrayResponseEntityAndGetDto();
    List<UserDto> result = service.findUsers(right, program, supervisoryNode, warehouse);

    // then
    assertThat(result, hasSize(1));
    assertTrue(result.contains(dto));

    verifyArrayRequest()
        .isGetRequest()
        .hasAuthHeader()
        .hasEmptyBody()
        .hasQueryParameter("rightId", right)
        .hasQueryParameter("programId", program)
        .hasQueryParameter("supervisoryNodeId", supervisoryNode)
        .hasQueryParameter("warehouseId", warehouse);
  }

  @Test
  public void shouldCheckIfUserHasRight() {
    // given
    UUID user = UUID.randomUUID();
    UUID right = UUID.randomUUID();
    UUID program = UUID.randomUUID();
    UUID facility = UUID.randomUUID();
    UUID warehouse = UUID.randomUUID();

    // when
    mockResultResponseEntity(true);
    ResultDto<Boolean> result = service
        .hasRight(user, right, program, facility, warehouse);

    // then
    assertThat(result.getResult(), is(true));

    verifyResultRequest()
        .isGetRequest()
        .hasAuthHeader()
        .hasEmptyBody()
        .hasQueryParameter("rightId", right)
        .hasQueryParameter("programId", program)
        .hasQueryParameter("facilityId", facility)
        .hasQueryParameter("warehouseId", warehouse);
  }

  @Test
  public void shouldGetPermissionStrings() {
    // given
    UUID user = UUID.randomUUID();
    String[] permissionStrings = {"permission"};

    // when
    mockArrayResponseEntity(permissionStrings);
    List<String> result = service.getPermissionStrings(user);

    // then
    assertThat(result, hasSize(1));
    assertTrue(result.contains(permissionStrings[0]));

    verifyArrayRequest(String[].class)
        .isGetRequest()
        .hasAuthHeader()
        .hasEmptyBody();
  }
}
