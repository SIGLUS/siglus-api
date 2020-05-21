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

import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.openlmis.requisition.dto.ProgramDto;
import org.openlmis.requisition.service.BaseCommunicationService;
import org.openlmis.requisition.testutils.ProgramDtoDataBuilder;

public class ProgramReferenceDataServiceTest extends BaseReferenceDataServiceTest<ProgramDto> {

  private ProgramReferenceDataService service;

  @Override
  protected ProgramDto generateInstance() {
    return new ProgramDtoDataBuilder().buildAsDto();
  }

  @Override
  protected BaseCommunicationService<ProgramDto> getService() {
    return new ProgramReferenceDataService();
  }

  @Override
  @Before
  public void setUp() {
    super.setUp();
    service = (ProgramReferenceDataService) prepareService();
  }

  @Test
  public void shouldSearchSupplyLines() {
    // given
    String programName = RandomStringUtils.randomAlphanumeric(10);

    // when
    ProgramDto dto = mockArrayResponseEntityAndGetDto();
    Collection<ProgramDto> result = service.search(programName);

    // then
    assertThat(result, hasSize(1));
    assertTrue(result.contains(dto));

    verifyArrayRequest()
        .isGetRequest()
        .hasAuthHeader()
        .hasEmptyBody()
        .hasQueryParameter("name", programName);
  }

  @Test
  public void shouldFindProgramsByIds() {
    // given
    UUID program1 = UUID.randomUUID();
    UUID program2 = UUID.randomUUID();


    // when
    ProgramDto dto = mockArrayResponseEntityAndGetDto();
    List<ProgramDto> result = service.search(Sets.newHashSet(program1, program2));

    // then
    assertThat(result, hasSize(1));
    assertTrue(result.contains(dto));

    verifyArrayRequest()
        .isGetRequest()
        .hasAuthHeader()
        .hasEmptyBody();
  }
}
