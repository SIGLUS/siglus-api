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

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.openlmis.requisition.dto.RightDto;
import org.openlmis.requisition.service.BaseCommunicationService;
import org.openlmis.requisition.testutils.RightDtoDataBuilder;

public class RightReferenceDataServiceTest extends BaseReferenceDataServiceTest<RightDto> {

  private RightReferenceDataService service;

  @Override
  protected RightDto generateInstance() {
    return new RightDtoDataBuilder().buildAsDto();
  }

  @Override
  protected BaseCommunicationService<RightDto> getService() {
    return new RightReferenceDataService();
  }

  @Override
  @Before
  public void setUp() {
    super.setUp();
    service = (RightReferenceDataService) prepareService();
  }

  @Test
  public void shouldFindRightByName() {
    // given
    String name = RandomStringUtils.randomAlphanumeric(10);

    // when
    RightDto dto = mockArrayResponseEntityAndGetDto();
    RightDto result = service.findRight(name);

    // then
    assertThat(result, is(dto));

    verifyArrayRequest()
        .isGetRequest()
        .hasAuthHeader()
        .hasEmptyBody()
        .hasQueryParameter("name", name);
  }

  @Test
  public void shouldReturnNullIfRightDoesNotExist() {
    // given
    String name = RandomStringUtils.randomAlphanumeric(10);

    // when
    mockArrayResponseEntity(new RightDto[0]);
    RightDto result = service.findRight(name);

    // then
    assertThat(result, is(nullValue()));

    verifyArrayRequest()
        .isGetRequest()
        .hasAuthHeader()
        .hasEmptyBody()
        .hasQueryParameter("name", name);
  }
}
