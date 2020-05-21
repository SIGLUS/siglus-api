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

package org.openlmis.requisition.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.domain.requisition.RequisitionStatus;
import org.openlmis.requisition.domain.requisition.StatusChange;
import org.openlmis.requisition.dto.BasicProcessingPeriodDto;
import org.openlmis.requisition.dto.GeographicZoneDto;
import org.openlmis.requisition.dto.MinimalFacilityDto;
import org.openlmis.requisition.dto.ProcessingPeriodDto;
import org.openlmis.requisition.dto.ProcessingScheduleDto;
import org.openlmis.requisition.service.referencedata.FacilityReferenceDataService;
import org.openlmis.requisition.service.referencedata.GeographicZoneReferenceDataService;
import org.openlmis.requisition.service.referencedata.PeriodReferenceDataService;

@SuppressWarnings("PMD.TooManyMethods")
@RunWith(MockitoJUnitRunner.class)
public class ReportingRateDtoBuilderTest {
  private static int GEOGRAPHIC_LEVEL_NUMBER = 3;

  @InjectMocks
  private ReportingRateReportDtoBuilder builder;

  @Mock
  private PeriodReferenceDataService periodReferenceDataService;

  @Mock
  private FacilityReferenceDataService facilityReferenceDataService;

  @Mock
  private GeographicZoneReferenceDataService geographicZoneReferenceDataService;

  @Test
  public void shouldGetLatestPeriods() {
    // given
    List<ProcessingPeriodDto> periods = generateProcessingPeriods(5, null)
        .stream()
        .sorted(Comparator.comparing(BasicProcessingPeriodDto::getStartDate))
        .collect(Collectors.toList());

    when(periodReferenceDataService.search(any(UUID.class), any(LocalDate.class)))
        .thenReturn(periods);

    // when
    Collection<ProcessingPeriodDto> result =
        builder.getLatestPeriods(periods.get(periods.size() - 1), 3);

    // then
    assertNotNull(result);

    List<ProcessingPeriodDto> resultList = new ArrayList<>(result);
    List<ProcessingPeriodDto> expected = periods
        .stream()
        .skip(2)
        .limit(3)
        .collect(Collectors.toList());

    assertEquals(3, resultList.size());
    assertEquals(expected.get(0), resultList.get(0));
    assertEquals(expected.get(1), resultList.get(1));
    assertEquals(expected.get(2), resultList.get(2));
  }

  @Test
  public void shouldGetOnlyLatestPeriodWhenPreviousNonExistent() {
    // given
    List<ProcessingPeriodDto> periods = generateProcessingPeriods(1, null);

    when(periodReferenceDataService.search(any(UUID.class), any(LocalDate.class)))
        .thenReturn(periods);

    // when
    Collection<ProcessingPeriodDto> result =
        builder.getLatestPeriods(periods.get(periods.size() - 1), 3);

    // then
    assertNotNull(result);
    assertEquals(1, result.size());
  }

  @Test
  public void shouldGetAvailableGeographicZonesForParentZone() {
    // given
    GeographicZoneDto zone = new GeographicZoneDto();
    zone.setId(UUID.randomUUID());
    List<GeographicZoneDto> expected = Collections.singletonList(zone);

    when(geographicZoneReferenceDataService.search(GEOGRAPHIC_LEVEL_NUMBER, zone.getId()))
        .thenReturn(expected);

    // when
    Collection<GeographicZoneDto> result = builder.getAvailableGeographicZones(zone);

    // then
    assertNotNull(result);
    assertEquals(expected, result);
  }

  @Test
  public void shouldGetAllGeographicZonesWhenParentZoneIsNull() {
    // given
    List<GeographicZoneDto> expected = Collections.singletonList(new GeographicZoneDto());

    when(geographicZoneReferenceDataService.search(GEOGRAPHIC_LEVEL_NUMBER, null))
        .thenReturn(expected);

    // when
    Collection<GeographicZoneDto> result = builder.getAvailableGeographicZones(null);

    // then
    assertNotNull(result);
    assertEquals(expected, result);
  }

  @Test
  public void shouldGetAvailableFacilities() {
    // given
    GeographicZoneDto zone1 = mockGeographicZoneWithFacility(true);
    GeographicZoneDto zone2 = mockGeographicZoneWithFacility(true);

    // when
    Collection<MinimalFacilityDto> result =
        builder.getAvailableFacilities(Arrays.asList(zone1, zone2));

    // then
    assertNotNull(result);
    assertEquals(2, result.size());
  }

  @Test
  public void shouldGetAvailableFacilitiesWithoutInactiveFacilities() {
    // given
    GeographicZoneDto zone1 = mockGeographicZoneWithFacility(true);
    GeographicZoneDto zone2 = mockGeographicZoneWithFacility(false);

    // when
    Collection<MinimalFacilityDto> result =
        builder.getAvailableFacilities(Arrays.asList(zone1, zone2));

    // then
    assertNotNull(result);

    List<MinimalFacilityDto> resultList = new ArrayList<>(result);

    assertEquals(1, resultList.size());
    assertEquals(zone1, resultList.get(0).getGeographicZone());
  }

  @Test
  public void shouldUpdateCompletionsWithRequisitions() {
    // given
    LocalDateTime dueDate = LocalDate.of(1994, 8, 10).atStartOfDay();
    ZoneId zoneId = ZoneId.systemDefault();

    List<Requisition> requisitions = new ArrayList<>();

    // on-time requisition
    requisitions.add(mockRequisitionWithStatusChanges(
        RequisitionStatus.APPROVED, ZonedDateTime.of(dueDate, zoneId)));

    // late requisition
    requisitions.add(mockRequisitionWithStatusChanges(
        RequisitionStatus.APPROVED, ZonedDateTime.of(dueDate.plusDays(5), zoneId)));

    // missed requisition
    requisitions.add(mockRequisitionWithStatusChanges(
        RequisitionStatus.SUBMITTED, ZonedDateTime.of(dueDate.minusDays(5), zoneId)));

    // when
    ReportingRateReportDtoBuilder.CompletionCounter counter = builder.new CompletionCounter();
    builder.updateCompletionsWithRequisitions(counter, requisitions, dueDate.toLocalDate());

    // then
    assertEquals(1, counter.getLate());
    assertEquals(1, counter.getOnTime());
    assertEquals(1, counter.getMissed());
  }

  @Test
  public void shouldUpdateCompletionsWithMissedIfNoRequisitionsProvided() {
    // given
    List<Requisition> requisitions = new ArrayList<>();

    // when
    ReportingRateReportDtoBuilder.CompletionCounter counter = builder.new CompletionCounter();
    builder.updateCompletionsWithRequisitions(counter, requisitions, LocalDate.of(1994, 8, 10));

    // then
    assertEquals(0, counter.getLate());
    assertEquals(0, counter.getOnTime());
    assertEquals(1, counter.getMissed());
  }

  private GeographicZoneDto mockGeographicZoneWithFacility(boolean facilityActive) {
    GeographicZoneDto zone = mock(GeographicZoneDto.class);
    UUID zoneId = UUID.randomUUID();

    when(zone.getId()).thenReturn(zoneId);

    MinimalFacilityDto facility = mock(MinimalFacilityDto.class);
    UUID facilityId = UUID.randomUUID();

    when(facility.getId()).thenReturn(facilityId);
    when(facility.getActive()).thenReturn(facilityActive);
    when(facility.getGeographicZone()).thenReturn(zone);
    List<MinimalFacilityDto> facilitiesForZone = Collections.singletonList(facility);

    when(facilityReferenceDataService.search(null, null, zone.getId(), true))
        .thenReturn(facilitiesForZone);

    return zone;
  }

  private Requisition mockRequisitionWithStatusChanges(
      RequisitionStatus status, ZonedDateTime date) {
    StatusChange entry = mock(StatusChange.class);
    when(entry.getStatus()).thenReturn(status);
    when(entry.getCreatedDate()).thenReturn(date);
    List<StatusChange> changes = Collections.singletonList(entry);

    Requisition requisition = mock(Requisition.class);
    when(requisition.getStatusChanges()).thenReturn(changes);

    return requisition;
  }

  private List<ProcessingPeriodDto> generateProcessingPeriods(
      int amount, ProcessingScheduleDto schedule) {
    List<ProcessingPeriodDto> periods = new ArrayList<>();
    Random random = new Random();

    if (schedule == null) {
      schedule = new ProcessingScheduleDto();
      schedule.setId(UUID.randomUUID());
    }

    Set<Integer> pickedNumbers = new HashSet<>();
    for (int i = 0; i < amount; i++) {
      ProcessingPeriodDto period = new ProcessingPeriodDto();
      period.setId(UUID.randomUUID());

      // Do not generate more than one period with the same start date
      int variation;
      do {
        variation = random.nextInt(50);
      } while (pickedNumbers.contains(variation));
      pickedNumbers.add(variation);

      LocalDate startDate = LocalDate.now()
          .minusWeeks(variation);

      LocalDate endDate = startDate
          .plusWeeks(random.nextInt(50));

      period.setStartDate(startDate);
      period.setEndDate(endDate);

      period.setProcessingSchedule(schedule);
      periods.add(period);
    }

    return periods;
  }
}
