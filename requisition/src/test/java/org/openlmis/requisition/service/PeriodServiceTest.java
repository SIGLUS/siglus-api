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

package org.openlmis.requisition.service;

import static java.time.temporal.TemporalAdjusters.firstDayOfMonth;
import static java.time.temporal.TemporalAdjusters.lastDayOfMonth;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsCollectionContaining.hasItems;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.openlmis.requisition.domain.requisition.RequisitionStatus.APPROVED;
import static org.openlmis.requisition.domain.requisition.RequisitionStatus.AUTHORIZED;
import static org.openlmis.requisition.domain.requisition.RequisitionStatus.INITIATED;
import static org.openlmis.requisition.domain.requisition.RequisitionStatus.RELEASED;
import static org.openlmis.requisition.domain.requisition.RequisitionStatus.SKIPPED;
import static org.openlmis.requisition.domain.requisition.RequisitionStatus.SUBMITTED;
import static org.openlmis.requisition.dto.BasicProcessingPeriodDto.START_DATE;

import com.google.common.collect.Lists;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.domain.requisition.RequisitionDataBuilder;
import org.openlmis.requisition.domain.requisition.RequisitionPeriod;
import org.openlmis.requisition.domain.requisition.RequisitionStatus;
import org.openlmis.requisition.dto.ProcessingPeriodDto;
import org.openlmis.requisition.dto.ProcessingScheduleDto;
import org.openlmis.requisition.dto.RequisitionPeriodDto;
import org.openlmis.requisition.exception.ValidationMessageException;
import org.openlmis.requisition.repository.RequisitionRepository;
import org.openlmis.requisition.service.referencedata.PeriodReferenceDataService;
import org.openlmis.requisition.service.referencedata.ScheduleReferenceDataService;
import org.openlmis.requisition.testutils.ProcessingPeriodDtoDataBuilder;
import org.openlmis.requisition.testutils.ProcessingScheduleDtoDataBuilder;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort.Direction;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("PMD.TooManyMethods")
public class PeriodServiceTest {

  @Mock
  private PeriodReferenceDataService periodReferenceDataService;

  @Mock
  private RequisitionRepository requisitionRepository;

  @Mock
  private ScheduleReferenceDataService scheduleReferenceDataService;

  @InjectMocks
  private PeriodService periodService;

  private UUID programId = UUID.randomUUID();
  private UUID facilityId = UUID.randomUUID();
  private UUID facility2Id = UUID.randomUUID();
  private UUID requisitionId = UUID.randomUUID();

  private ProcessingPeriodDto currentPeriod;
  private ProcessingPeriodDto period1;
  private ProcessingPeriodDto period2;
  private ProcessingPeriodDto period3;
  private ProcessingPeriodDto period4;

  private ProcessingScheduleDto schedule;

  @Before
  public void setUp() throws Exception {
    schedule = new ProcessingScheduleDtoDataBuilder().buildAsDto();

    currentPeriod = createPeriod(0);
    period1 = createPeriod(1);
    period2 = createPeriod(2);
    period3 = createPeriod(3);
    period4 = createPeriod(4);
  }

  @Test
  public void shouldReturnCurrentPeriod() throws Exception {
    doReturn(singletonList(currentPeriod))
        .when(periodReferenceDataService)
        .searchByProgramAndFacility(programId, facilityId);

    RequisitionPeriod requisitionPeriodSubmitted =
        createRequisitionPeriod(requisitionId, SUBMITTED, currentPeriod.getId());

    doReturn(singletonList(requisitionPeriodSubmitted))
        .when(requisitionRepository)
        .searchRequisitionIdAndStatusPairs(facilityId, programId, false);

    List<ProcessingPeriodDto> currentPeriods =
        periodService.getCurrentPeriods(programId, facilityId);

    assertThat(currentPeriods, hasSize(1));
    assertThat(currentPeriods.iterator().next().getId(), is(equalTo(currentPeriod.getId())));
  }

  @Test
  public void shouldReturnCurrentPeriodsIfThereIsMoreThanOne() throws Exception {
    RequisitionPeriod requisitionPeriodSubmitted =
        createRequisitionPeriod(requisitionId, SUBMITTED, currentPeriod.getId());

    ProcessingPeriodDto period6 = new ProcessingPeriodDtoDataBuilder()
        .withStartDate(currentPeriod.getStartDate())
        .withEndDate(currentPeriod.getEndDate())
        .buildAsDto();

    doReturn(Arrays.asList(currentPeriod, period6))
        .when(periodReferenceDataService)
        .searchByProgramAndFacility(programId, facilityId);

    doReturn(singletonList(requisitionPeriodSubmitted))
        .when(requisitionRepository)
        .searchRequisitionIdAndStatusPairs(facilityId, programId, false);
    doReturn(singletonList(requisitionPeriodSubmitted))
        .when(requisitionRepository)
        .searchRequisitionIdAndStatusPairs(facilityId, programId, false);

    List<ProcessingPeriodDto> currentPeriods =
        periodService.getCurrentPeriods(programId, facilityId);

    List<UUID> ids = currentPeriods
        .stream()
        .map(ProcessingPeriodDto::getId)
        .collect(Collectors.toList());

    assertThat(ids, hasSize(2));
    assertThat(ids, contains(currentPeriod.getId(), period6.getId()));
  }

  @Test
  public void shouldReturnCurrentPeriodIfThereIsNoRequisitionIdStatusPair() throws Exception {
    doReturn(singletonList(currentPeriod))
        .when(periodReferenceDataService)
        .searchByProgramAndFacility(programId, facilityId);

    doReturn(Collections.emptyList())
        .when(requisitionRepository)
        .searchRequisitionIdAndStatusPairs(facilityId, programId, false);

    List<ProcessingPeriodDto> currentPeriods =
        periodService.getCurrentPeriods(programId, facilityId);

    assertThat(currentPeriods, hasSize(1));
  }

  @Test
  public void shouldNotReturnCurrentPeriodIfItDoesNotExist() throws Exception {
    doReturn(singletonList(period1))
        .when(periodReferenceDataService)
        .searchByProgramAndFacility(programId, facilityId);

    List<ProcessingPeriodDto> currentPeriods =
        periodService.getCurrentPeriods(programId, facilityId);

    assertThat(currentPeriods, hasSize(0));
  }

  @Test
  public void shouldReturnCurrentPeriodIfThereIsNonSubmittedRequisition() throws Exception {
    RequisitionPeriod requisitionPeriodInitiated =
        createRequisitionPeriod(requisitionId, INITIATED, currentPeriod.getId());

    doReturn(singletonList(currentPeriod))
        .when(periodReferenceDataService)
        .searchByProgramAndFacility(programId, facilityId);

    doReturn(singletonList(requisitionPeriodInitiated))
        .when(requisitionRepository)
        .searchRequisitionIdAndStatusPairs(facilityId, programId, false);

    List<ProcessingPeriodDto> currentPeriods =
        periodService.getCurrentPeriods(programId, facilityId);

    assertThat(currentPeriods, hasSize(1));
  }

  @Test
  public void shouldReturnOnlyValidPeriodsForRequisitionInitiate() {
    List<ProcessingPeriodDto> list = Lists.newArrayList(
        currentPeriod, period1, period2, period3, period4
    );

    doReturn(list)
        .when(periodReferenceDataService)
        .searchByProgramAndFacility(programId, facilityId);

    RequisitionPeriod requisitionPeriodInitiated1 =
        createRequisitionPeriod(UUID.randomUUID(), INITIATED, currentPeriod.getId());
    RequisitionPeriod requisitionPeriodInitiated2 =
        createRequisitionPeriod(UUID.randomUUID(), INITIATED, period1.getId());
    RequisitionPeriod requisitionPeriodSubmitted =
        createRequisitionPeriod(UUID.randomUUID(), SUBMITTED, period2.getId());
    RequisitionPeriod requisitionPeriodAuthorized =
        createRequisitionPeriod(UUID.randomUUID(), AUTHORIZED, period3.getId());
    RequisitionPeriod requisitionPeriodApproved =
        createRequisitionPeriod(UUID.randomUUID(), APPROVED, period4.getId());

    doReturn(Arrays.asList(requisitionPeriodInitiated1, requisitionPeriodInitiated2,
        requisitionPeriodSubmitted, requisitionPeriodAuthorized, requisitionPeriodApproved))
        .when(requisitionRepository)
        .searchRequisitionIdAndStatusPairs(facilityId, programId, false);

    Collection<RequisitionPeriodDto> periods =
        periodService.getPeriods(programId, facilityId, false);

    verify(requisitionRepository, times(1))
        .searchRequisitionIdAndStatusPairs(any(UUID.class), any(UUID.class), any());

    assertNotNull(periods);
    assertEquals(3, periods.size());

    List<UUID> periodUuids = periods
        .stream()
        .map(ProcessingPeriodDto::getId)
        .collect(Collectors.toList());

    assertTrue(periodUuids.contains(currentPeriod.getId()));
    assertTrue(periodUuids.contains(period1.getId()));
    assertTrue(periodUuids.contains(period2.getId()));
  }

  @Test
  public void shouldIncludeRequisitionIdAndStatus() {
    doReturn(singletonList(currentPeriod))
        .when(periodReferenceDataService)
        .searchByProgramAndFacility(programId, facilityId);

    RequisitionPeriod requisitionPeriodInitiated =
        createRequisitionPeriod(requisitionId, INITIATED, currentPeriod.getId());

    doReturn(singletonList(requisitionPeriodInitiated))
        .when(requisitionRepository)
        .searchRequisitionIdAndStatusPairs(facilityId, programId, false);

    Collection<RequisitionPeriodDto> periods =
        periodService.getPeriods(programId, facilityId, false);

    assertNotNull(periods);
    assertThat(periods, hasSize(1));
    RequisitionPeriodDto period = periods.iterator().next();
    assertEquals(period.getRequisitionId(), requisitionPeriodInitiated.getRequisitionId());
    assertEquals(period.getRequisitionStatus(), requisitionPeriodInitiated.getRequisitionStatus());
  }

  @Test
  public void shouldReturnMultipleRequisitionPeriodsForEmergency() {
    doReturn(singletonList(currentPeriod))
        .when(periodReferenceDataService)
        .searchByProgramAndFacility(programId, facilityId);

    RequisitionPeriod requisitionPeriodInitiated =
        createRequisitionPeriod(UUID.randomUUID(), INITIATED, currentPeriod.getId());
    RequisitionPeriod requisitionPeriodSubmitted =
        createRequisitionPeriod(UUID.randomUUID(), SUBMITTED, currentPeriod.getId());

    doReturn(Arrays.asList(requisitionPeriodInitiated, requisitionPeriodSubmitted))
        .when(requisitionRepository)
        .searchRequisitionIdAndStatusPairs(facilityId, programId, true);

    Collection<RequisitionPeriodDto> periods =
        periodService.getPeriods(programId, facilityId, true);

    assertNotNull(periods);
    assertThat(periods, hasSize(3));

    for (RequisitionPeriodDto period : periods) {
      assertEquals(currentPeriod.getId(), period.getId());
    }
    List<UUID> requisitionIds = periods
        .stream()
        .map(RequisitionPeriodDto::getRequisitionId)
        .collect(Collectors.toList());
    assertTrue(requisitionIds.contains(requisitionPeriodInitiated.getRequisitionId()));
    assertTrue(requisitionIds.contains(requisitionPeriodSubmitted.getRequisitionId()));
    // should allow to initiate another requisition for the same period
    assertTrue(requisitionIds.contains(null));
  }

  @Test
  public void shouldNotReturnRequisitionsPostAuthorizeForEmergency() {
    doReturn(singletonList(currentPeriod))
        .when(periodReferenceDataService)
        .searchByProgramAndFacility(programId, facilityId);

    RequisitionPeriod requisitionPeriodApproved =
        createRequisitionPeriod(UUID.randomUUID(), APPROVED, currentPeriod.getId());
    RequisitionPeriod requisitionPeriodReleased =
        createRequisitionPeriod(UUID.randomUUID(), RELEASED, currentPeriod.getId());

    doReturn(Arrays.asList(requisitionPeriodApproved, requisitionPeriodReleased))
        .when(requisitionRepository)
        .searchRequisitionIdAndStatusPairs(facilityId, programId, true);

    Collection<RequisitionPeriodDto> periods =
        periodService.getPeriods(programId, facilityId, true);

    assertNotNull(periods);
    assertThat(periods, hasSize(1));
    assertNull(periods.iterator().next().getRequisitionId());
  }

  @Test
  public void shouldReturnPreviousPeriods() throws Exception {
    doReturn(period4)
        .when(periodReferenceDataService)
        .findOne(period4.getId());
    doReturn(Arrays.asList(currentPeriod, period1, period2, period3, period4))
        .when(periodReferenceDataService)
        .search(period4.getProcessingSchedule().getId(), period4.getStartDate().minusDays(1),
            new PageRequest(0, 2, Direction.DESC, START_DATE));

    List<ProcessingPeriodDto> previousPeriods =
        periodService.findPreviousPeriods(period4.getId(), 2);

    assertThat(previousPeriods, hasItems(period3, period2));
  }

  @Test
  public void shouldReturnPreviousPeriodsWithoutRetrievingWholePeriod() {
    doReturn(Arrays.asList(currentPeriod, period1, period2, period3, period4))
        .when(periodReferenceDataService)
        .search(period4.getProcessingSchedule().getId(), period4.getStartDate().minusDays(1),
            new PageRequest(0, 2, Direction.DESC, START_DATE));

    List<ProcessingPeriodDto> previousPeriods =
        periodService.findPreviousPeriods(period4, 2);

    assertThat(previousPeriods, hasItems(period3, period2));
  }

  @Test
  public void shouldReturnPreviousPeriod() throws Exception {
    doReturn(period4)
        .when(periodReferenceDataService)
        .findOne(period4.getId());
    doReturn(singletonList(period3))
        .when(periodReferenceDataService)
        .search(period4.getProcessingSchedule().getId(), period4.getStartDate().minusDays(1),
            new PageRequest(0, 1, Direction.DESC, START_DATE));

    assertEquals(period3, periodService.findPreviousPeriod(period4.getId()));
  }

  @Test
  public void shouldReturnWholeListIfAmountGreaterThanListSize() throws Exception {
    doReturn(period4)
        .when(periodReferenceDataService)
        .findOne(period4.getId());
    doReturn(Arrays.asList(currentPeriod, period1, period2, period3, period4))
        .when(periodReferenceDataService)
        .search(period4.getProcessingSchedule().getId(), period4.getStartDate().minusDays(1),
            new PageRequest(0, 5, Direction.DESC, START_DATE));

    List<ProcessingPeriodDto> previousPeriods =
        periodService.findPreviousPeriods(period4.getId(), 5);

    assertThat(previousPeriods, hasItems(period3, period2, period1, currentPeriod));
  }

  @Test(expected = ValidationMessageException.class)
  public void shouldThrowExceptionIfPeriodIsNotTheOldest() {
    ProcessingScheduleDto processingScheduleDto = new ProcessingScheduleDtoDataBuilder()
        .buildAsDto();

    currentPeriod.setProcessingSchedule(processingScheduleDto);
    currentPeriod.setId(UUID.randomUUID());

    when(periodReferenceDataService.searchByProgramAndFacility(programId, facilityId))
        .thenReturn(Lists.newArrayList(currentPeriod));

    when(scheduleReferenceDataService.searchByProgramAndFacility(programId, facilityId))
        .thenReturn(singletonList(processingScheduleDto));

    periodService.findPeriod(programId, facilityId, UUID.randomUUID(), false);
  }

  @Test(expected = ValidationMessageException.class)
  public void shouldThrowExceptionWhenInitiatingReqPeriodDoesNotBelongToTheSameScheduleAsProgram() {

    ProcessingScheduleDto processingScheduleDto = new ProcessingScheduleDtoDataBuilder()
        .buildAsDto();
    when(scheduleReferenceDataService.searchByProgramAndFacility(programId, facilityId))
        .thenReturn(singletonList(processingScheduleDto));

    periodService.findPeriod(programId, facilityId, null, false);
  }

  @Test(expected = ValidationMessageException.class)
  public void shouldThrowExceptionWhenPreviousReqHasInitiatedStatus() {
    setMockForFindPeriod();

    mockRequisitionFound(buildRequisition(INITIATED), currentPeriod.getId(), facilityId, programId);
    mockNoRequisitionFound(period1.getId(), facilityId, programId);

    periodService.findPeriod(programId, facilityId, null, false);
  }

  @Test(expected = ValidationMessageException.class)
  public void shouldThrowExceptionWhenPreviousReqHasSubmittedStatus() {
    setMockForFindPeriod();

    mockRequisitionFound(buildRequisition(SUBMITTED), currentPeriod.getId(), facilityId, programId);
    mockNoRequisitionFound(period1.getId(), facilityId, programId);

    periodService.findPeriod(programId, facilityId, null, false);
  }

  @Test
  public void shouldSucceedWhenPreviousReqHasAuthorizedStatus() {
    setMockForFindPeriod();

    mockRequisitionFound(buildRequisition(AUTHORIZED), currentPeriod.getId(), facilityId,
        programId);
    mockNoRequisitionFound(period1.getId(), facilityId, programId);

    ProcessingPeriodDto period = periodService.findPeriod(programId, facilityId, null, false);
    assertEquals(period1, period);
  }

  @Test
  public void shouldSucceedWhenPreviousReqHasApprovedStatus() {
    setMockForFindPeriod();

    mockRequisitionFound(buildRequisition(APPROVED), currentPeriod.getId(), facilityId, programId);
    mockNoRequisitionFound(period1.getId(), facilityId, programId);

    ProcessingPeriodDto period = periodService.findPeriod(programId, facilityId, null, false);
    assertEquals(period1, period);
  }

  @Test
  public void shouldSucceedWhenPreviousReqHasSkippedStatus() {
    setMockForFindPeriod();

    mockRequisitionFound(buildRequisition(SKIPPED), currentPeriod.getId(), facilityId, programId);
    mockNoRequisitionFound(period1.getId(), facilityId, programId);

    ProcessingPeriodDto period = periodService.findPeriod(programId, facilityId, null, false);
    assertEquals(period1, period);
  }

  @Test
  public void shouldSucceedWhenRequisitionForNextPeriodIsPresent() {
    setMockForFindPeriod();

    mockRequisitionFound(buildRequisition(AUTHORIZED), currentPeriod.getId(), facilityId,
        programId);
    mockNoRequisitionFound(period1.getId(), facilityId, programId);
    mockRequisitionFound(buildRequisition(INITIATED), period2.getId(), facilityId, programId);

    ProcessingPeriodDto period = periodService.findPeriod(programId, facilityId, null, false);
    assertEquals(period1, period);
  }

  @Test
  public void shouldUseNextAvailablePeriodWhenOneIsTaken() {
    //given
    setMockForFindPeriod();

    // we mock the requisition search to return a requisition for the first period
    // but not for the second
    mockRequisitionFound(buildRequisition(APPROVED), currentPeriod.getId(), facilityId, programId);
    mockNoRequisitionFound(period1.getId(), facilityId, programId);

    //when
    ProcessingPeriodDto period = periodService.findPeriod(programId, facilityId, null, false);

    // then, we expect a second period to be returned
    assertEquals(period1, period);
  }

  @Test
  public void shouldAllowUsingTheSamePeriodInMultipleFacilities() {
    //given
    setMockForFindPeriod();

    // we mock the requisition search to return a requisition for the first period
    // in facility with ID facilityId
    mockRequisitionFound(buildRequisition(INITIATED), currentPeriod.getId(), facilityId, programId);

    //when
    ProcessingPeriodDto period = periodService.findPeriod(programId, facility2Id, null, false);

    // then, we expect a first period to be returned for second facility, despite it being used in
    // the first facility
    assertEquals(currentPeriod, period);
  }

  private ProcessingPeriodDto createPeriod(int plusMonth) {
    return new ProcessingPeriodDtoDataBuilder()
        .withStartDate(LocalDate.now().plusMonths(plusMonth).with(firstDayOfMonth()))
        .withEndDate(LocalDate.now().plusMonths(plusMonth).with(lastDayOfMonth()))
        .withProcessingSchedule(schedule)
        .buildAsDto();
  }

  private Requisition buildRequisition(RequisitionStatus status) {
    return new RequisitionDataBuilder()
        .withStatus(status)
        .build();
  }

  private RequisitionPeriod createRequisitionPeriod(UUID requisitionId, RequisitionStatus status,
      UUID periodId) {
    return new RequisitionPeriod(requisitionId, status, periodId);
  }

  private void mockRequisitionFound(
      Requisition requisition, UUID periodId, UUID facilityId, UUID programId) {
    doReturn(singletonList(requisition))
        .when(requisitionRepository)
        .searchRequisitions(periodId, facilityId, programId, false);
  }

  private void mockNoRequisitionFound(UUID periodId, UUID facilityId, UUID programId) {
    doReturn(Collections.emptyList())
        .when(requisitionRepository)
        .searchRequisitions(periodId, facilityId, programId, false);
  }

  private void setMockForFindPeriod() {
    List<ProcessingPeriodDto> periods = new ArrayList<>();
    periods.add(currentPeriod);
    periods.add(period1);
    periods.add(period2);

    when(periodReferenceDataService.searchByProgramAndFacility(programId, facilityId))
        .thenReturn(periods);
    when(periodReferenceDataService.searchByProgramAndFacility(programId, facility2Id))
        .thenReturn(periods);

    when(scheduleReferenceDataService.searchByProgramAndFacility(programId, facilityId))
        .thenReturn(singletonList(schedule));
    when(scheduleReferenceDataService.searchByProgramAndFacility(programId, facility2Id))
        .thenReturn(singletonList(schedule));
  }

}
