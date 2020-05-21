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

import static java.util.Collections.emptyList;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.List;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.openlmis.requisition.dto.SupervisoryNodeDto;
import org.openlmis.requisition.service.BaseCommunicationService;
import org.openlmis.requisition.testutils.SupervisoryNodeDtoDataBuilder;

public class SupervisoryNodeReferenceDataServiceTest
    extends BaseReferenceDataServiceTest<SupervisoryNodeDto> {

  private SupervisoryNodeReferenceDataService service;

  @Override
  protected SupervisoryNodeDto generateInstance() {
    return new SupervisoryNodeDtoDataBuilder().buildAsDto();
  }

  @Override
  protected BaseCommunicationService<SupervisoryNodeDto> getService() {
    return new SupervisoryNodeReferenceDataService();
  }

  @Override
  @Before
  public void setUp() {
    super.setUp();
    service = (SupervisoryNodeReferenceDataService) prepareService();
  }

  @Test
  public void shouldFindSupervisoryNode() {
    // given
    UUID program = UUID.randomUUID();
    UUID facility = UUID.randomUUID();

    // when
    SupervisoryNodeDto dto = mockPageResponseEntityAndGetDto();
    SupervisoryNodeDto result = service.findSupervisoryNode(program, facility);

    // then
    assertThat(result, is(dto));

    verifyPageRequest()
        .isGetRequest()
        .hasAuthHeader()
        .hasEmptyBody()
        .hasQueryParameter("programId", program)
        .hasQueryParameter("facilityId", facility);
  }

  @Test
  public void shouldReturnNullIfCanNotFindSupervisoryNode() {
    // given
    UUID program = UUID.randomUUID();
    UUID facility = UUID.randomUUID();

    // when
    mockPageResponseEntity(Lists.newArrayList());
    SupervisoryNodeDto result = service.findSupervisoryNode(program, facility);

    // then
    assertThat(result, is(nullValue()));

    verifyPageRequest()
        .isGetRequest()
        .hasAuthHeader()
        .hasEmptyBody()
        .hasQueryParameter("programId", program)
        .hasQueryParameter("facilityId", facility);
  }

  @Test
  public void shouldFindByIds() {
    // given
    UUID id1 = UUID.randomUUID();
    UUID id2 = UUID.randomUUID();

    // when
    SupervisoryNodeDto dto = mockPageResponseEntityAndGetDto();
    List<SupervisoryNodeDto> result = service.findByIds(Sets.newHashSet(id1, id2));

    // then
    assertThat(result, hasSize(1));
    assertTrue(result.contains(dto));

    verifyPageRequest()
        .isGetRequest()
        .hasAuthHeader()
        .hasEmptyBody();
  }

  @Test
  public void shouldReturnEmptyListForFindByIdsIfParameterIsEmptyOrNull() {
    // given
    disableAuthCheck();

    // when & then
    assertThat(service.findByIds(null), hasSize(0));
    assertThat(service.findByIds(emptyList()), hasSize(0));
  }

}
