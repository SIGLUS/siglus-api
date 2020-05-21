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
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.requisition.dto.ProcessingPeriodDto;
import org.openlmis.requisition.service.BaseCommunicationService;
import org.openlmis.requisition.service.BaseCommunicationServiceTest;
import org.openlmis.requisition.testutils.ProcessingPeriodDtoDataBuilder;
import org.springframework.data.domain.PageRequest;

@RunWith(MockitoJUnitRunner.class)
public class PeriodReferenceDataServiceTest
    extends BaseCommunicationServiceTest<ProcessingPeriodDto> {

  private PeriodReferenceDataService service;

  @Override
  protected ProcessingPeriodDto generateInstance() {
    return new ProcessingPeriodDtoDataBuilder().buildAsDto();
  }

  @Override
  protected BaseCommunicationService<ProcessingPeriodDto> getService() {
    return new PeriodReferenceDataService();
  }

  @Override
  @Before
  public void setUp() {
    super.setUp();
    service = (PeriodReferenceDataService) prepareService();
  }

  @Test
  public void shouldSearchProcessingPeriodsByScheduleIdAndEndDate() {
    // given
    UUID scheduleId = UUID.randomUUID();
    LocalDate date = LocalDate.now();

    // when
    ProcessingPeriodDto period = mockPageResponseEntityAndGetDto();
    Collection<ProcessingPeriodDto> result = service.search(scheduleId, date);

    // then
    assertThat(result, hasSize(1));
    assertTrue(result.contains(period));

    verifyPageRequest()
        .isGetRequest()
        .hasAuthHeader()
        .hasEmptyBody()
        .hasQueryParameter("processingScheduleId", scheduleId)
        .hasQueryParameter("endDate", date)
        .hasQueryParameter("size", Integer.MAX_VALUE);
  }

  @Test
  public void shouldFindSortedProcessingPeriodsByScheduleIdAndEndDate() {
    // given
    UUID scheduleId = UUID.randomUUID();
    LocalDate date = LocalDate.now();

    // when
    ProcessingPeriodDto period = mockPageResponseEntityAndGetDto();
    Collection<ProcessingPeriodDto> result = service.search(scheduleId, date,
        new PageRequest(0, 10));

    // then
    assertThat(result, hasSize(1));
    assertTrue(result.contains(period));

    verifyPageRequest()
        .isGetRequest()
        .hasAuthHeader()
        .hasEmptyBody()
        .hasQueryParameter("processingScheduleId", scheduleId)
        .hasQueryParameter("endDate", date)
        .hasQueryParameter("page", 0)
        .hasQueryParameter("size", 10);
  }

  @Test
  public void shouldSearchProcessingPeriodsByFacilityAndProgram() {
    // given
    UUID facilityId = UUID.randomUUID();
    UUID programId = UUID.randomUUID();

    // when
    ProcessingPeriodDto period = mockPageResponseEntityAndGetDto();
    Collection<ProcessingPeriodDto> result = service
        .searchByProgramAndFacility(programId, facilityId);

    // then
    assertThat(result, hasSize(1));
    assertTrue(result.contains(period));

    verifyPageRequest()
        .isGetRequest()
        .hasAuthHeader()
        .hasEmptyBody()
        .hasQueryParameter("facilityId", facilityId)
        .hasQueryParameter("programId", programId);
  }

  @Test
  public void shouldFindProcessingPeriodsByIds() {
    // given
    UUID period1 = UUID.randomUUID();
    UUID period2 = UUID.randomUUID();


    // when
    ProcessingPeriodDto dto = mockPageResponseEntityAndGetDto();
    List<ProcessingPeriodDto> result = service.search(Sets.newHashSet(period1, period2));

    // then
    assertThat(result, hasSize(1));
    assertTrue(result.contains(dto));

    verifyPageRequest()
        .isGetRequest()
        .hasAuthHeader()
        .hasEmptyBody();
  }
}
