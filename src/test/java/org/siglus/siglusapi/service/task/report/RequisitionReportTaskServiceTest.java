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

package org.siglus.siglusapi.service.task.report;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.requisition.dto.FacilityDto;
import org.openlmis.requisition.dto.ProgramDto;
import org.openlmis.requisition.service.referencedata.FacilityReferenceDataService;
import org.openlmis.requisition.service.referencedata.ProgramReferenceDataService;
import org.siglus.common.domain.ProcessingPeriodExtension;
import org.siglus.common.domain.referencedata.ProcessingPeriod;
import org.siglus.common.domain.referencedata.ProcessingSchedule;
import org.siglus.common.repository.ProcessingPeriodExtensionRepository;
import org.siglus.siglusapi.domain.ProgramRequisitionNameMapping;
import org.siglus.siglusapi.domain.RequisitionMonthlyReport;
import org.siglus.siglusapi.domain.report.RequisitionMonthlyReportFacility;
import org.siglus.siglusapi.dto.SupportedProgramDto;
import org.siglus.siglusapi.repository.FacilityNativeRepository;
import org.siglus.siglusapi.repository.ProcessingPeriodRepository;
import org.siglus.siglusapi.repository.ProgramRequisitionNameMappingRepository;
import org.siglus.siglusapi.repository.RequisitionMonthReportRepository;
import org.siglus.siglusapi.repository.RequisitionMonthlyNotSubmitReportRepository;
import org.siglus.siglusapi.repository.dto.FacilityProgramPeriodScheduleDto;
import org.siglus.siglusapi.repository.dto.FacillityStockCardDateDto;
import org.siglus.siglusapi.service.client.SiglusFacilityReferenceDataService;

@RunWith(MockitoJUnitRunner.class)
public class RequisitionReportTaskServiceTest {

  @Mock
  private ProgramReferenceDataService programDataService;
  @Mock
  private FacilityReferenceDataService facilityReferenceDataService;
  @Mock
  private RequisitionMonthReportRepository requisitionMonthReportRepository;
  @Mock
  private RequisitionMonthlyNotSubmitReportRepository requisitionMonthlyNotSubmitReportRepository;
  @Mock
  private FacilityNativeRepository facilityNativeRepository;
  @Mock
  private ProgramRequisitionNameMappingRepository programRequisitionNameMappingRepository;
  @Mock
  private SiglusFacilityReferenceDataService siglusFacilityReferenceDataService;
  @Mock
  private ProcessingPeriodExtensionRepository processingPeriodExtensionRepository;
  @Mock
  private ProcessingPeriodRepository processingPeriodRepository;
  @InjectMocks
  private RequisitionReportTaskService requisitionReportTaskService;

  private final UUID facilityId = UUID.randomUUID();

  private final UUID programId = UUID.randomUUID();
  private final UUID reportId = UUID.randomUUID();
  private final UUID periodExtensionId = UUID.randomUUID();
  private final UUID periodId = UUID.randomUUID();

  private final UUID periodScheduleId = UUID.randomUUID();
  private final ProcessingSchedule processingSchedule = new ProcessingSchedule();

  @Before
  public void setup() {
    FacilityDto facilityDto = new FacilityDto();
    facilityDto.setId(facilityId);
    List<FacilityDto> allFacilityDto = new ArrayList<>();
    allFacilityDto.add(facilityDto);
    when(facilityReferenceDataService.findAll()).thenReturn(allFacilityDto);
    List<ProgramDto> allProgramDto = new ArrayList<>();
    ProgramDto programDto = new ProgramDto();
    programDto.setId(programId);
    programDto.setName("test 1");
    allProgramDto.add(programDto);
    when(programDataService.findAll()).thenReturn(allProgramDto);
    RequisitionMonthlyReportFacility item4 = new RequisitionMonthlyReportFacility();
    item4.setFacilityId(facilityId);
    item4.setDistrict("setDistrict1");
    item4.setFacilityName("setFacilityName1");
    item4.setProvince("setProvince");
    List<RequisitionMonthlyReportFacility> requisitionMonthlyReportFacilities = new ArrayList<>();
    requisitionMonthlyReportFacilities.add(item4);
    when(facilityNativeRepository.queryAllFacilityInfo()).thenReturn(
        requisitionMonthlyReportFacilities);

    ProgramRequisitionNameMapping item5 = new ProgramRequisitionNameMapping();
    item5.setProgramId(programId);
    item5.setRequisitionName("Via RequisitionName");
    item5.setProgramCode("code1");
    List<ProgramRequisitionNameMapping> requisitionNameMapping = new ArrayList<>();
    requisitionNameMapping.add(item5);
    when(programRequisitionNameMappingRepository.findAll()).thenReturn(requisitionNameMapping);

    org.siglus.siglusapi.dto.FacilityDto facilityDto2 = new org.siglus.siglusapi.dto.FacilityDto();
    facilityDto2.setActive(true);
    SupportedProgramDto supportedProgramDto = new SupportedProgramDto();
    supportedProgramDto.setSupportStartDate(LocalDate.MIN);
    supportedProgramDto.setId(programId);
    List<SupportedProgramDto> supportedProgramDtos = new ArrayList<>();
    supportedProgramDtos.add(supportedProgramDto);
    facilityDto2.setSupportedPrograms(supportedProgramDtos);
    when(siglusFacilityReferenceDataService.findOne(facilityId)).thenReturn(facilityDto2);

    final FacilityProgramPeriodScheduleDto item111 = new FacilityProgramPeriodScheduleDto();
    item111.setProcessingScheduleId(periodScheduleId);
    item111.setFacilityId(facilityId);
    item111.setProgramId(programId);
    List<FacilityProgramPeriodScheduleDto> all = new ArrayList<>();
    all.add(item111);

    when(facilityNativeRepository.findFacilityProgramPeriodSchedule()).thenReturn(all);

    processingSchedule.setId(periodScheduleId);
  }

  @Test
  public void shouldCallSaveWhenRefreshWithEmptyRequisition() {
    // given

    ProcessingPeriodExtension item = new ProcessingPeriodExtension();
    item.setId(periodExtensionId);
    item.setProcessingPeriodId(periodId);
    item.setSubmitStartDate(LocalDate.of(2022, 1, 20));
    item.setSubmitEndDate(LocalDate.of(2022, 1, 25));
    List<ProcessingPeriodExtension> allProcessingPeriodExtensionDto = new ArrayList<>();
    allProcessingPeriodExtensionDto.add(item);
    when(processingPeriodExtensionRepository.findAll()).thenReturn(allProcessingPeriodExtensionDto);
    ProcessingPeriod item2 = new ProcessingPeriod();
    item2.setId(periodId);
    item2.setStartDate(LocalDate.of(2022, 1, 1));
    item2.setEndDate(LocalDate.of(2022, 1, 30));
    item2.setProcessingSchedule(processingSchedule);
    List<ProcessingPeriod> allProcessingPeriodDto = new ArrayList<>();
    allProcessingPeriodDto.add(item2);
    when(processingPeriodRepository.findAll()).thenReturn(allProcessingPeriodDto);


    when(facilityNativeRepository.findFirstStockCardGroupByFacility()).thenReturn(
        getFacillityStockCardDateDto(2022, 1, 10));

    when(requisitionMonthReportRepository.findAll()).thenReturn(new ArrayList<>());

    // when
    requisitionReportTaskService.refresh(false);

    // then
    verify(requisitionMonthlyNotSubmitReportRepository).save(any(List.class));

  }

  @Test
  public void shouldNotCallSaveWhenRefreshWithOneSubmitRequisition() {
    // given

    ProcessingPeriodExtension item = new ProcessingPeriodExtension();
    item.setId(periodExtensionId);
    item.setProcessingPeriodId(periodId);
    item.setSubmitStartDate(LocalDate.of(2022, 1, 20));
    item.setSubmitEndDate(LocalDate.of(2022, 1, 25));
    List<ProcessingPeriodExtension> allProcessingPeriodExtensionDto = new ArrayList<>();
    allProcessingPeriodExtensionDto.add(item);
    when(processingPeriodExtensionRepository.findAll()).thenReturn(allProcessingPeriodExtensionDto);
    ProcessingPeriod item2 = new ProcessingPeriod();
    item2.setId(periodId);
    item2.setStartDate(LocalDate.of(2022, 1, 1));
    item2.setEndDate(LocalDate.of(2022, 1, 30));
    item2.setProcessingSchedule(processingSchedule);
    List<ProcessingPeriod> allProcessingPeriodDto = new ArrayList<>();
    allProcessingPeriodDto.add(item2);
    when(processingPeriodRepository.findAll()).thenReturn(allProcessingPeriodDto);

    when(facilityNativeRepository.findFirstStockCardGroupByFacility()).thenReturn(
        getFacillityStockCardDateDto(2021, 1, 10));

    RequisitionMonthlyReport report = new RequisitionMonthlyReport();
    report.setFacilityId(facilityId);
    report.setId(reportId);
    report.setProgramId(programId);
    report.setProcessingPeriodId(periodId);
    List<RequisitionMonthlyReport> requisitionMonthlyReports = new ArrayList<>();
    requisitionMonthlyReports.add(report);
    when(requisitionMonthReportRepository.findAll()).thenReturn(requisitionMonthlyReports);

    // when
    requisitionReportTaskService.refresh(false);

    // then
    verify(requisitionMonthlyNotSubmitReportRepository).save(any(List.class));

  }

  @Test
  public void shouldNotCallSaveWhenRefreshWithFutureDate() {
    // given
    ProcessingPeriodExtension item = new ProcessingPeriodExtension();
    item.setId(periodExtensionId);
    item.setProcessingPeriodId(periodId);
    item.setSubmitStartDate(LocalDate.of(2022, 1, 20));
    item.setSubmitEndDate(LocalDate.of(2022, 1, 25));
    List<ProcessingPeriodExtension> allProcessingPeriodExtensionDto = new ArrayList<>();
    allProcessingPeriodExtensionDto.add(item);

    ProcessingPeriodExtension item1 = new ProcessingPeriodExtension();
    item1.setId(periodExtensionId);
    UUID periodId2 = UUID.randomUUID();
    item1.setProcessingPeriodId(periodId2);
    item1.setSubmitStartDate(LocalDate.of(2022, 10, 20));
    item1.setSubmitEndDate(LocalDate.of(2022, 10, 25));
    allProcessingPeriodExtensionDto.add(item1);

    when(processingPeriodExtensionRepository.findAll()).thenReturn(allProcessingPeriodExtensionDto);
    ProcessingPeriod item2 = new ProcessingPeriod();
    item2.setId(periodId);
    item2.setStartDate(LocalDate.of(2022, 1, 1));
    item2.setEndDate(LocalDate.of(2022, 1, 30));
    item2.setProcessingSchedule(processingSchedule);
    List<ProcessingPeriod> allProcessingPeriodDto = new ArrayList<>();
    allProcessingPeriodDto.add(item2);
    ProcessingPeriod item22 = new ProcessingPeriod();
    item22.setId(periodId2);
    item22.setStartDate(LocalDate.of(2022, 10, 1));
    item22.setEndDate(LocalDate.of(2022, 10, 30));
    item22.setProcessingSchedule(processingSchedule);
    allProcessingPeriodDto.add(item22);

    when(processingPeriodRepository.findAll()).thenReturn(allProcessingPeriodDto);

    when(facilityNativeRepository.findFirstStockCardGroupByFacility()).thenReturn(
        getFacillityStockCardDateDto(2022, 10, 10));

    RequisitionMonthlyReport report = new RequisitionMonthlyReport();
    report.setFacilityId(facilityId);
    report.setId(reportId);
    report.setProgramId(programId);
    report.setProcessingPeriodId(UUID.randomUUID());
    List<RequisitionMonthlyReport> requisitionMonthlyReports = new ArrayList<>();
    requisitionMonthlyReports.add(report);
    when(requisitionMonthReportRepository.findAll()).thenReturn(requisitionMonthlyReports);

    // when
    requisitionReportTaskService.refresh(false);

    // then
    verify(requisitionMonthlyNotSubmitReportRepository, times(0)).save(any(List.class));

  }

  @Test
  public void shouldNotCallSaveWhenRefreshWithHisDate() {
    // given
    ProcessingPeriodExtension item = new ProcessingPeriodExtension();
    item.setId(periodExtensionId);
    item.setProcessingPeriodId(periodId);
    item.setSubmitStartDate(LocalDate.of(2022, 1, 20));
    item.setSubmitEndDate(LocalDate.of(2022, 1, 25));
    List<ProcessingPeriodExtension> allProcessingPeriodExtensionDto = new ArrayList<>();
    allProcessingPeriodExtensionDto.add(item);

    ProcessingPeriodExtension item1 = new ProcessingPeriodExtension();
    item1.setId(periodExtensionId);
    UUID periodId2 = UUID.randomUUID();
    item1.setProcessingPeriodId(periodId2);
    item1.setSubmitStartDate(LocalDate.of(2022, 10, 20));
    item1.setSubmitEndDate(LocalDate.of(2022, 10, 25));
    allProcessingPeriodExtensionDto.add(item1);

    when(processingPeriodExtensionRepository.findAll()).thenReturn(allProcessingPeriodExtensionDto);
    ProcessingPeriod item2 = new ProcessingPeriod();
    item2.setId(periodId);
    item2.setStartDate(LocalDate.of(2022, 1, 1));
    item2.setEndDate(LocalDate.of(2022, 1, 30));
    item2.setProcessingSchedule(processingSchedule);
    List<ProcessingPeriod> allProcessingPeriodDto = new ArrayList<>();
    allProcessingPeriodDto.add(item2);
    ProcessingPeriod item22 = new ProcessingPeriod();
    item22.setId(periodId2);
    item22.setStartDate(LocalDate.of(2022, 10, 1));
    item22.setProcessingSchedule(processingSchedule);
    item22.setEndDate(LocalDate.of(2022, 10, 30));
    allProcessingPeriodDto.add(item22);

    when(processingPeriodRepository.findAll()).thenReturn(allProcessingPeriodDto);

    when(facilityNativeRepository.findFirstStockCardGroupByFacility()).thenReturn(
        getFacillityStockCardDateDto(2019, 10, 10));

    RequisitionMonthlyReport report = new RequisitionMonthlyReport();
    report.setFacilityId(facilityId);
    report.setId(reportId);
    report.setProgramId(programId);
    report.setProcessingPeriodId(UUID.randomUUID());
    List<RequisitionMonthlyReport> requisitionMonthlyReports = new ArrayList<>();
    requisitionMonthlyReports.add(report);
    when(requisitionMonthReportRepository.findAll()).thenReturn(requisitionMonthlyReports);

    // when
    requisitionReportTaskService.refresh(false);

    // then
    verify(requisitionMonthlyNotSubmitReportRepository, times(1)).save(any(List.class));

  }

  @Test
  public void shouldCallTwoSaveWhenRefreshWithHisDate() {
    // given
    ProcessingPeriodExtension item = new ProcessingPeriodExtension();
    item.setId(periodExtensionId);
    item.setProcessingPeriodId(periodId);
    item.setSubmitStartDate(LocalDate.of(2022, 1, 20));
    item.setSubmitEndDate(LocalDate.of(2022, 1, 25));
    List<ProcessingPeriodExtension> allProcessingPeriodExtensionDto = new ArrayList<>();
    allProcessingPeriodExtensionDto.add(item);

    ProcessingPeriodExtension item1 = new ProcessingPeriodExtension();
    item1.setId(periodExtensionId);
    UUID periodId2 = UUID.randomUUID();
    item1.setProcessingPeriodId(periodId2);
    item1.setSubmitStartDate(LocalDate.of(2022, 2, 20));
    item1.setSubmitEndDate(LocalDate.of(2022, 2, 25));
    allProcessingPeriodExtensionDto.add(item1);

    when(processingPeriodExtensionRepository.findAll()).thenReturn(allProcessingPeriodExtensionDto);
    ProcessingPeriod item2 = new ProcessingPeriod();
    item2.setId(periodId);
    item2.setStartDate(LocalDate.of(2022, 1, 1));
    item2.setProcessingSchedule(processingSchedule);
    item2.setEndDate(LocalDate.of(2022, 1, 28));
    List<ProcessingPeriod> allProcessingPeriodDto = new ArrayList<>();
    allProcessingPeriodDto.add(item2);
    ProcessingPeriod item22 = new ProcessingPeriod();
    item22.setId(periodId2);
    item22.setProcessingSchedule(processingSchedule);
    item22.setStartDate(LocalDate.of(2022, 2, 1));
    item22.setEndDate(LocalDate.of(2022, 2, 28));
    allProcessingPeriodDto.add(item22);

    when(processingPeriodRepository.findAll()).thenReturn(allProcessingPeriodDto);

    when(facilityNativeRepository.findFirstStockCardGroupByFacility()).thenReturn(
        getFacillityStockCardDateDto(2021, 1, 19));

    RequisitionMonthlyReport report = new RequisitionMonthlyReport();
    report.setFacilityId(facilityId);
    report.setId(reportId);
    report.setProgramId(programId);
    report.setProcessingPeriodId(UUID.randomUUID());
    List<RequisitionMonthlyReport> requisitionMonthlyReports = new ArrayList<>();
    requisitionMonthlyReports.add(report);
    when(requisitionMonthReportRepository.findAll()).thenReturn(requisitionMonthlyReports);

    // when
    requisitionReportTaskService.refresh(false);

    // then
    verify(requisitionMonthlyNotSubmitReportRepository, times(1)).save(any(List.class));

  }

  @Test
  public void shouldCallOneSaveWhenRefreshWithHisDate() {
    // given
    ProcessingPeriodExtension item = new ProcessingPeriodExtension();
    item.setId(periodExtensionId);
    item.setProcessingPeriodId(periodId);
    item.setSubmitStartDate(LocalDate.of(2022, 1, 20));
    item.setSubmitEndDate(LocalDate.of(2022, 1, 25));
    List<ProcessingPeriodExtension> allProcessingPeriodExtensionDto = new ArrayList<>();
    allProcessingPeriodExtensionDto.add(item);

    ProcessingPeriodExtension item1 = new ProcessingPeriodExtension();
    item1.setId(periodExtensionId);
    UUID periodId2 = UUID.randomUUID();
    item1.setProcessingPeriodId(periodId2);
    item1.setSubmitStartDate(LocalDate.of(2022, 2, 20));
    item1.setSubmitEndDate(LocalDate.of(2022, 2, 25));
    allProcessingPeriodExtensionDto.add(item1);

    when(processingPeriodExtensionRepository.findAll()).thenReturn(allProcessingPeriodExtensionDto);
    ProcessingPeriod item2 = new ProcessingPeriod();
    item2.setId(periodId);
    item2.setStartDate(LocalDate.of(2022, 1, 1));
    item2.setProcessingSchedule(processingSchedule);
    item2.setEndDate(LocalDate.of(2022, 1, 28));
    List<ProcessingPeriod> allProcessingPeriodDto = new ArrayList<>();
    allProcessingPeriodDto.add(item2);
    ProcessingPeriod item22 = new ProcessingPeriod();
    item22.setId(periodId2);
    item22.setProcessingSchedule(processingSchedule);
    item22.setStartDate(LocalDate.of(2022, 2, 1));
    item22.setEndDate(LocalDate.of(2022, 2, 28));
    allProcessingPeriodDto.add(item22);

    when(processingPeriodRepository.findAll()).thenReturn(allProcessingPeriodDto);

    when(facilityNativeRepository.findFirstStockCardGroupByFacility()).thenReturn(
        getFacillityStockCardDateDto(2021, 1, 21));

    RequisitionMonthlyReport report = new RequisitionMonthlyReport();
    report.setFacilityId(facilityId);
    report.setId(reportId);
    report.setProgramId(programId);
    report.setProcessingPeriodId(UUID.randomUUID());
    List<RequisitionMonthlyReport> requisitionMonthlyReports = new ArrayList<>();
    requisitionMonthlyReports.add(report);
    when(requisitionMonthReportRepository.findAll()).thenReturn(requisitionMonthlyReports);

    // when
    requisitionReportTaskService.refresh(false);

    // then
    verify(requisitionMonthlyNotSubmitReportRepository, times(1)).save(any(List.class));

  }

  @Test
  public void shouldCallNotSaveWhenRefreshWithHisDate() {
    // given
    ProcessingPeriodExtension item = new ProcessingPeriodExtension();
    item.setId(periodExtensionId);
    item.setProcessingPeriodId(periodId);
    item.setSubmitStartDate(LocalDate.of(2022, 1, 20));
    item.setSubmitEndDate(LocalDate.of(2022, 1, 25));
    List<ProcessingPeriodExtension> allProcessingPeriodExtensionDto = new ArrayList<>();
    allProcessingPeriodExtensionDto.add(item);

    ProcessingPeriodExtension item1 = new ProcessingPeriodExtension();
    item1.setId(periodExtensionId);
    UUID periodId2 = UUID.randomUUID();
    item1.setProcessingPeriodId(periodId2);
    item1.setSubmitStartDate(LocalDate.of(2022, 2, 20));
    item1.setSubmitEndDate(LocalDate.of(2022, 2, 25));
    allProcessingPeriodExtensionDto.add(item1);

    when(processingPeriodExtensionRepository.findAll()).thenReturn(allProcessingPeriodExtensionDto);
    ProcessingPeriod item2 = new ProcessingPeriod();
    item2.setId(periodId);
    item2.setStartDate(LocalDate.of(2022, 1, 1));
    item2.setProcessingSchedule(processingSchedule);
    item2.setEndDate(LocalDate.of(2022, 1, 28));
    List<ProcessingPeriod> allProcessingPeriodDto = new ArrayList<>();
    allProcessingPeriodDto.add(item2);
    ProcessingPeriod item22 = new ProcessingPeriod();
    item22.setProcessingSchedule(processingSchedule);
    item22.setId(periodId2);
    item22.setStartDate(LocalDate.of(2022, 2, 1));
    item22.setEndDate(LocalDate.of(2022, 2, 28));
    allProcessingPeriodDto.add(item22);

    when(processingPeriodRepository.findAll()).thenReturn(allProcessingPeriodDto);

    when(facilityNativeRepository.findFirstStockCardGroupByFacility()).thenReturn(
        getFacillityStockCardDateDto(2021, 1, 25));

    RequisitionMonthlyReport report = new RequisitionMonthlyReport();
    report.setFacilityId(facilityId);
    report.setId(reportId);
    report.setProgramId(programId);
    report.setProcessingPeriodId(periodId);
    List<RequisitionMonthlyReport> requisitionMonthlyReports = new ArrayList<>();
    requisitionMonthlyReports.add(report);
    RequisitionMonthlyReport report2 = new RequisitionMonthlyReport();
    report2.setFacilityId(facilityId);
    report2.setId(reportId);
    report2.setProgramId(programId);
    report2.setProcessingPeriodId(periodId2);
    requisitionMonthlyReports.add(report2);
    when(requisitionMonthReportRepository.findAll()).thenReturn(requisitionMonthlyReports);

    // when
    requisitionReportTaskService.refresh(false);

    // then
    verify(requisitionMonthlyNotSubmitReportRepository).save(any(List.class));

  }


  @Test
  public void shouldCallAllMonthSaveWhenRefreshWithHisDate() {
    // given
    List<ProcessingPeriodExtension> allProcessingPeriodExtensionDto = new ArrayList<>();
    List<ProcessingPeriod> allProcessingPeriodDto = new ArrayList<>();
    for (int i = 1; i <= 12; i++) {
      UUID periodIdUuid = UUID.randomUUID();

      ProcessingPeriodExtension item1 = new ProcessingPeriodExtension();
      item1.setId(UUID.randomUUID());
      item1.setProcessingPeriodId(periodIdUuid);
      item1.setSubmitStartDate(LocalDate.of(2021, i, 20));
      item1.setSubmitEndDate(LocalDate.of(2021, i, 25));
      allProcessingPeriodExtensionDto.add(item1);

      ProcessingPeriod item22 = new ProcessingPeriod();
      item22.setId(periodIdUuid);
      item22.setProcessingSchedule(processingSchedule);
      item22.setStartDate(LocalDate.of(2021, i, 1));
      item22.setEndDate(LocalDate.of(2021, i, 28));
      allProcessingPeriodDto.add(item22);
    }
    when(processingPeriodExtensionRepository.findAll()).thenReturn(allProcessingPeriodExtensionDto);
    when(processingPeriodRepository.findAll()).thenReturn(allProcessingPeriodDto);

    when(facilityNativeRepository.findFirstStockCardGroupByFacility()).thenReturn(
        getFacillityStockCardDateDto(2021, 3, 21));

    RequisitionMonthlyReport report = new RequisitionMonthlyReport();
    report.setFacilityId(facilityId);
    report.setId(reportId);
    report.setProgramId(programId);
    report.setProcessingPeriodId(UUID.randomUUID());
    List<RequisitionMonthlyReport> requisitionMonthlyReports = new ArrayList<>();
    requisitionMonthlyReports.add(report);
    when(requisitionMonthReportRepository.findAll()).thenReturn(requisitionMonthlyReports);

    // when
    requisitionReportTaskService.refresh(false);

    // then
    verify(requisitionMonthlyNotSubmitReportRepository, times(1)).save(any(List.class));

  }

  private List<FacillityStockCardDateDto> getFacillityStockCardDateDto(int year, int month, int dayOfMonth) {
    FacillityStockCardDateDto item3 = new FacillityStockCardDateDto();
    item3.setFacilityId(facilityId);
    item3.setProgramId(programId);
    item3.setOccurredDate(java.sql.Date.valueOf(LocalDate.of(year, month, dayOfMonth)));

    List<FacillityStockCardDateDto> firstStockCardGroupByFacility = new ArrayList<>();
    firstStockCardGroupByFacility.add(item3);
    return firstStockCardGroupByFacility;
  }
}