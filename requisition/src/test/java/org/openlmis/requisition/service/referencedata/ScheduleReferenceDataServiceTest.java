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
import org.openlmis.requisition.dto.ProcessingScheduleDto;
import org.openlmis.requisition.service.BaseCommunicationService;
import org.openlmis.requisition.testutils.ProcessingScheduleDtoDataBuilder;

public class ScheduleReferenceDataServiceTest
    extends BaseReferenceDataServiceTest<ProcessingScheduleDto> {

  private ScheduleReferenceDataService service;

  @Override
  protected ProcessingScheduleDto generateInstance() {
    return new ProcessingScheduleDtoDataBuilder().buildAsDto();
  }

  @Override
  protected BaseCommunicationService<ProcessingScheduleDto> getService() {
    return new ScheduleReferenceDataService();
  }

  @Override
  @Before
  public void setUp() {
    super.setUp();
    service = (ScheduleReferenceDataService) prepareService();
  }

  @Test
  public void shouldSearchSupplyLines() {
    // given
    UUID programId = UUID.randomUUID();
    UUID facilityId = UUID.randomUUID();

    // when
    ProcessingScheduleDto dto = mockArrayResponseEntityAndGetDto();
    Collection<ProcessingScheduleDto> result = service
        .searchByProgramAndFacility(programId, facilityId);

    // then
    assertThat(result, hasSize(1));
    assertTrue(result.contains(dto));

    verifyArrayRequest()
        .isGetRequest()
        .hasAuthHeader()
        .hasEmptyBody()
        .hasQueryParameter("programId", programId)
        .hasQueryParameter("facilityId", facilityId);
  }
}
