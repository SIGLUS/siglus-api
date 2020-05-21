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

package org.openlmis.referencedata.service;

import static java.util.Collections.emptySet;
import static org.javers.common.collections.Sets.asSet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.referencedata.domain.Facility;
import org.openlmis.referencedata.domain.ProcessingPeriod;
import org.openlmis.referencedata.domain.ProcessingSchedule;
import org.openlmis.referencedata.domain.Program;
import org.openlmis.referencedata.domain.RequisitionGroupProgramSchedule;
import org.openlmis.referencedata.exception.NotFoundException;
import org.openlmis.referencedata.repository.FacilityRepository;
import org.openlmis.referencedata.repository.ProcessingPeriodRepository;
import org.openlmis.referencedata.repository.ProcessingScheduleRepository;
import org.openlmis.referencedata.repository.ProgramRepository;
import org.openlmis.referencedata.repository.RequisitionGroupProgramScheduleRepository;
import org.openlmis.referencedata.testbuilder.FacilityDataBuilder;
import org.openlmis.referencedata.testbuilder.ProcessingPeriodDataBuilder;
import org.openlmis.referencedata.testbuilder.ProcessingScheduleDataBuilder;
import org.openlmis.referencedata.testbuilder.ProgramDataBuilder;
import org.openlmis.referencedata.util.Pagination;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.util.LinkedMultiValueMap;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings({"PMD.TooManyMethods"})
public class ProcessingPeriodServiceTest {

  private static final String PROGRAM_ID = "programId";
  private static final String FACILITY_ID = "facilityId";
  private static final String PROCESSING_SCHEDULE_ID = "processingScheduleId";
  private static final String START_DATE = "startDate";
  private static final String END_DATE = "endDate";
  private static final String ID = "id";

  @InjectMocks
  private ProcessingPeriodService periodService;

  @Mock
  private ProcessingPeriodRepository periodRepository;

  @Mock
  private RequisitionGroupProgramScheduleRepository repository;

  @Mock
  private ProgramRepository programRepository;

  @Mock
  private FacilityRepository facilityRepository;

  @Mock
  private ProcessingScheduleRepository processingScheduleRepository;

  @Mock
  private RequisitionGroupProgramSchedule requisitionGroupProgramSchedule;

  private LinkedMultiValueMap<String, Object> queryMap;

  private ProcessingPeriod period = new ProcessingPeriodDataBuilder().build();
  private ProcessingSchedule schedule = new ProcessingScheduleDataBuilder().build();
  private Program program = new ProgramDataBuilder().build();
  private Facility facility = new FacilityDataBuilder().build();
  private List<ProcessingPeriod> periods = generateInstances();
  private PageRequest pageable = new PageRequest(0, 10);

  @Before
  public void setUp() {
    queryMap = new LinkedMultiValueMap<>();

    when(requisitionGroupProgramSchedule.getProcessingSchedule()).thenReturn(schedule);
    when(facilityRepository.findOne(facility.getId())).thenReturn(facility);
    when(programRepository.findOne(program.getId())).thenReturn(program);
    when(processingScheduleRepository.findOne(schedule.getId())).thenReturn(schedule);
    when(facilityRepository.exists(facility.getId())).thenReturn(true);
    when(programRepository.exists(program.getId())).thenReturn(true);
    when(processingScheduleRepository.exists(schedule.getId())).thenReturn(true);
  }

  @Test(expected = NotFoundException.class)
  public void shouldThrowExceptionWhenProgramWasNotFoundById() {
    queryMap.add(PROGRAM_ID, program.getId().toString());
    queryMap.add(FACILITY_ID, facility.getId().toString());
    ProcessingPeriodSearchParams params = new ProcessingPeriodSearchParams(queryMap);
    when(programRepository.exists(program.getId())).thenReturn(false);
    periodService.searchPeriods(params, pageable);
  }

  @Test(expected = NotFoundException.class)
  public void shouldThrowExceptionWhenFacilityWasNotFoundById() {
    queryMap.add(PROGRAM_ID, program.getId().toString());
    queryMap.add(FACILITY_ID, facility.getId().toString());
    ProcessingPeriodSearchParams params = new ProcessingPeriodSearchParams(queryMap);
    when(facilityRepository.exists(facility.getId())).thenReturn(false);
    periodService.searchPeriods(params, pageable);
  }

  @Test
  public void shouldReturnEmptyPageWhenScheduleWasNotFoundForFacilityAndProgram() {
    queryMap.add(PROGRAM_ID, program.getId().toString());
    queryMap.add(FACILITY_ID, facility.getId().toString());
    ProcessingPeriodSearchParams params = new ProcessingPeriodSearchParams(queryMap);
    doReturn(Pagination.getPage(Collections.emptyList(), pageable, 0)).when(periodRepository)
        .search(null, program.getId(), facility.getId(), null, null, emptySet(), pageable);
    Page<ProcessingPeriod> result = periodService.searchPeriods(params, pageable);
    assertEquals(0, result.getContent().size());
  }

  @Test
  public void shouldReturnEmptyPageWhenScheduleWasNotFoundForProgram() {
    queryMap.add(PROGRAM_ID, program.getId().toString());
    ProcessingPeriodSearchParams params = new ProcessingPeriodSearchParams(queryMap);
    doReturn(Pagination.getPage(Collections.emptyList(), pageable, 0)).when(periodRepository)
        .search(null, program.getId(), null, null, null, emptySet(), pageable);
    Page<ProcessingPeriod> result = periodService.searchPeriods(params, pageable);
    assertEquals(0, result.getContent().size());
  }

  @Test
  public void shouldFindPeriodsByProgramAndFacility() {
    doReturn(Collections.singletonList(requisitionGroupProgramSchedule)).when(repository)
          .searchRequisitionGroupProgramSchedules(program.getId(), facility.getId());
    doReturn(Pagination.getPage(Collections.singletonList(period), pageable, 1))
        .when(periodRepository).search(null, program.getId(), facility.getId(), null, null,
        emptySet(), pageable);

    queryMap.add(PROGRAM_ID, program.getId().toString());
    queryMap.add(FACILITY_ID, facility.getId().toString());
    ProcessingPeriodSearchParams params = new ProcessingPeriodSearchParams(queryMap);

    periodService.searchPeriods(params, pageable);

    verify(periodRepository).search(null, program.getId(), facility.getId(), null, null, emptySet(),
        pageable);
  }

  @Test
  public void shouldFindPeriodsByProgram() {
    doReturn(Collections.singletonList(requisitionGroupProgramSchedule)).when(repository)
            .searchRequisitionGroupProgramSchedules(program.getId(), null);
    doReturn(Pagination.getPage(Collections.singletonList(period), pageable, 1))
        .when(periodRepository).search(null, program.getId(), null, null, null, emptySet(),
        pageable);

    queryMap.add(PROGRAM_ID, program.getId().toString());
    ProcessingPeriodSearchParams params = new ProcessingPeriodSearchParams(queryMap);

    periodService.searchPeriods(params, pageable);

    verify(periodRepository).search(null, program.getId(), null, null, null, emptySet(), pageable);
  }

  @Test(expected = NotFoundException.class)
  public void shouldThrowExceptionWhenScheduleWasNotFoundById() {
    doReturn(false).when(processingScheduleRepository).exists(schedule.getId());

    queryMap.add(PROCESSING_SCHEDULE_ID, schedule.getId().toString());
    ProcessingPeriodSearchParams params = new ProcessingPeriodSearchParams(queryMap);

    periodService.searchPeriods(params, pageable);
  }

  @Test
  public void shouldFindPeriodsBySchedule() {
    doReturn(Pagination.getPage(Collections.singletonList(period), pageable, 1))
        .when(periodRepository).search(schedule.getId(), program.getId(), facility.getId(), null,
        null, emptySet(), pageable);

    queryMap.add(PROCESSING_SCHEDULE_ID, schedule.getId().toString());
    ProcessingPeriodSearchParams params = new ProcessingPeriodSearchParams(queryMap);

    periodService.searchPeriods(params, pageable);

    verify(periodRepository).search(schedule.getId(), null, null, null, null, emptySet(),
        pageable);
  }

  @Test
  public void shouldFindPeriodsWithStartDateAndEndDate() {
    List<ProcessingPeriod> matchedPeriods = new ArrayList<>();
    matchedPeriods.addAll(periods);
    matchedPeriods.remove(periods.get(0));

    queryMap.add(PROCESSING_SCHEDULE_ID, schedule.getId().toString());
    queryMap.add(START_DATE, periods.get(0).getStartDate().toString());
    queryMap.add(END_DATE, periods.get(0).getEndDate().toString());
    ProcessingPeriodSearchParams params = new ProcessingPeriodSearchParams(queryMap);

    when(periodRepository
        .search(schedule.getId(), null, null, periods.get(0).getStartDate(),
            periods.get(0).getEndDate(), emptySet(), pageable))
        .thenReturn(Pagination.getPage(matchedPeriods, pageable, 5));

    Page<ProcessingPeriod> receivedPeriods = periodService
        .searchPeriods(params, pageable);

    assertEquals(4, receivedPeriods.getContent().size());
    for (ProcessingPeriod period : receivedPeriods.getContent()) {
      assertEquals(schedule, period.getProcessingSchedule());
      assertTrue(periods.get(0).getStartDate().isAfter(period.getStartDate()));
    }
  }

  @Test
  public void shouldFindPeriodsByIds() {
    queryMap.add(ID, periods.get(0).getId().toString());
    queryMap.add(ID, periods.get(1).getId().toString());
    ProcessingPeriodSearchParams params = new ProcessingPeriodSearchParams(queryMap);

    when(periodRepository
        .search(null,null, null, null, null, asSet(periods.get(0).getId(), periods.get(1).getId()),
            pageable))
        .thenReturn(Pagination.getPage(periods, pageable, 5));

    Page<ProcessingPeriod> receivedPeriods = periodService
        .searchPeriods(params, pageable);

    assertEquals(receivedPeriods.getContent(), periods);
  }

  private List<ProcessingPeriod> generateInstances() {
    return IntStream
        .range(0, 5)
        .mapToObj(this::generatePeriod)
        .collect(Collectors.toList());
  }

  private ProcessingPeriod generatePeriod(int instanceNumber) {
    LocalDate now = LocalDate.now();

    return new ProcessingPeriodDataBuilder()
        .withSchedule(schedule)
        .withPeriod(now.minusDays(instanceNumber), now.plusDays(instanceNumber))
        .build();
  }

}
