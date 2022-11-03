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

package org.siglus.siglusapi.service.scheduledtask;

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
import org.openlmis.referencedata.domain.ProcessingSchedule;
import org.openlmis.requisition.dto.FacilityDto;
import org.openlmis.requisition.dto.ProcessingPeriodDto;
import org.openlmis.requisition.dto.ProgramDto;
import org.openlmis.requisition.service.PeriodService;
import org.openlmis.requisition.service.referencedata.FacilityReferenceDataService;
import org.openlmis.requisition.service.referencedata.ProgramReferenceDataService;
import org.siglus.siglusapi.constant.PeriodConstants;
import org.siglus.siglusapi.domain.ProgramRequisitionNameMapping;
import org.siglus.siglusapi.domain.RequisitionMonthlyReport;
import org.siglus.siglusapi.domain.SiglusReportType;
import org.siglus.siglusapi.domain.report.RequisitionMonthlyReportFacility;
import org.siglus.siglusapi.dto.SupportedProgramDto;
import org.siglus.siglusapi.repository.FacilityNativeRepository;
import org.siglus.siglusapi.repository.ProgramRequisitionNameMappingRepository;
import org.siglus.siglusapi.repository.RequisitionMonthReportRepository;
import org.siglus.siglusapi.repository.RequisitionMonthlyNotSubmitReportRepository;
import org.siglus.siglusapi.repository.SiglusReportTypeRepository;
import org.siglus.siglusapi.repository.dto.FacilityProgramPeriodScheduleDto;
import org.siglus.siglusapi.repository.dto.FacillityStockCardDateDto;
import org.siglus.siglusapi.service.SiglusProcessingPeriodService;
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
  @InjectMocks
  private RequisitionReportTaskService requisitionReportTaskService;
  @Mock
  private SiglusReportTypeRepository reportTypeRepository;

  @Mock
  private SiglusProcessingPeriodService siglusProcessingPeriodService;
  @Mock
  private PeriodService periodService;

  private final UUID facilityId = UUID.randomUUID();

  private final UUID programId = UUID.randomUUID();
  private final UUID reportId = UUID.randomUUID();
  private final UUID periodExtensionId = UUID.randomUUID();
  private final UUID periodId = UUID.randomUUID();

  private final UUID periodScheduleId = UUID.randomUUID();
  private final String programCode = "T";

  private final ProcessingSchedule processingSchedule = new ProcessingSchedule();

  @Before
  public void setup() {
    FacilityDto facilityDto = new FacilityDto();
    facilityDto.setId(facilityId);
    List<FacilityDto> allFacilityDto = new ArrayList<>();
    allFacilityDto.add(facilityDto);
    when(facilityReferenceDataService.findAll()).thenReturn(allFacilityDto);
    ProgramDto programDto = new ProgramDto();
    programDto.setId(programId);
    programDto.setName("test 1");
    programDto.setCode(programCode);
    List<ProgramDto> allProgramDto = new ArrayList<>();
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
    item5.setReportName("Via RequisitionName");
    item5.setProgramName("code1");
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
    processingSchedule.setCode(PeriodConstants.MONTH_SCHEDULE_CODE);

    SiglusReportType reportType = new SiglusReportType();
    reportType.setFacilityId(facilityId);
    reportType.setProgramCode(programCode);
    reportType.setStartDate(LocalDate.MIN);
    List<SiglusReportType> facilityReportTypeList = new ArrayList<>();
    facilityReportTypeList.add(reportType);
    when(reportTypeRepository.findByFacilityId(facilityId)).thenReturn(facilityReportTypeList);
  }

  @Test
  public void shouldCallSaveWhenRefreshWithEmptyRequisition() {
    // given
    ProcessingPeriodDto item = new ProcessingPeriodDto();
    item.setId(periodExtensionId);
    item.setStartDate(LocalDate.of(2022, 1, 1));
    item.setEndDate(LocalDate.of(2022, 1, 30));
    item.setSubmitStartDate(LocalDate.of(2022, 1, 20));
    item.setSubmitEndDate(LocalDate.of(2022, 1, 25));
    final List<ProcessingPeriodDto> coll = new ArrayList<>();
    coll.add(item);
    when(siglusProcessingPeriodService.fillProcessingPeriodWithExtension(coll)).thenReturn(coll);
    when(periodService.searchByProgramAndFacility(programId, facilityId)).thenReturn(coll);

    when(facilityNativeRepository.findFirstStockCardGroupByFacility()).thenReturn(
        getFacillityStockCardDateDto(2021, 1, 10));

    when(requisitionMonthReportRepository.findAll()).thenReturn(new ArrayList<>());

    // when
    requisitionReportTaskService.refresh();

    // then
    verify(requisitionMonthlyNotSubmitReportRepository).save(any(List.class));

  }

  @Test
  public void shouldNotCallSaveWhenRefreshWithOneSubmitRequisition() {
    // given

    ProcessingPeriodDto item = new ProcessingPeriodDto();
    item.setId(periodExtensionId);
    item.setStartDate(LocalDate.of(2022, 1, 1));
    item.setEndDate(LocalDate.of(2022, 1, 30));
    item.setSubmitStartDate(LocalDate.of(2022, 1, 20));
    item.setSubmitEndDate(LocalDate.of(2022, 1, 25));
    final List<ProcessingPeriodDto> coll = new ArrayList<>();
    coll.add(item);
    when(siglusProcessingPeriodService.fillProcessingPeriodWithExtension(coll)).thenReturn(coll);
    when(periodService.searchByProgramAndFacility(programId, facilityId)).thenReturn(coll);

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
    requisitionReportTaskService.refresh();

    // then
    verify(requisitionMonthlyNotSubmitReportRepository).save(any(List.class));

  }

  @Test
  public void shouldNotCallSaveWhenRefreshWithHisDate() {
    // given
    ProcessingPeriodDto item = new ProcessingPeriodDto();
    item.setId(periodExtensionId);
    item.setStartDate(LocalDate.of(2022, 1, 1));
    item.setEndDate(LocalDate.of(2022, 1, 30));
    item.setSubmitStartDate(LocalDate.of(2022, 1, 20));
    item.setSubmitEndDate(LocalDate.of(2022, 1, 25));

    ProcessingPeriodDto item2 = new ProcessingPeriodDto();
    item2.setId(periodExtensionId);
    item2.setStartDate(LocalDate.of(2022, 10, 1));
    item2.setEndDate(LocalDate.of(2022, 10, 30));
    item2.setSubmitStartDate(LocalDate.of(2022, 10, 20));
    item2.setSubmitEndDate(LocalDate.of(2022, 10, 25));

    final List<ProcessingPeriodDto> coll = new ArrayList<>();
    coll.add(item);
    coll.add(item2);
    when(siglusProcessingPeriodService.fillProcessingPeriodWithExtension(coll)).thenReturn(coll);
    when(periodService.searchByProgramAndFacility(programId, facilityId)).thenReturn(coll);

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
    requisitionReportTaskService.refresh();

    // then
    verify(requisitionMonthlyNotSubmitReportRepository, times(1)).save(any(List.class));
  }

  private List<FacillityStockCardDateDto> getFacillityStockCardDateDto(int year, int month, int dayOfMonth) {
    FacillityStockCardDateDto item1 = new FacillityStockCardDateDto();
    item1.setFacilityId(facilityId);
    item1.setProgramId(programId);
    item1.setOccurredDate(java.sql.Date.valueOf(LocalDate.of(year, month, dayOfMonth)));

    FacillityStockCardDateDto item2 = new FacillityStockCardDateDto();
    item2.setFacilityId(facilityId);
    item2.setProgramId(programId);
    item2.setOccurredDate(java.sql.Date.valueOf(LocalDate.of(year, month, dayOfMonth)));

    List<FacillityStockCardDateDto> firstStockCardGroupByFacility = new ArrayList<>();
    firstStockCardGroupByFacility.add(item1);
    return firstStockCardGroupByFacility;
  }
}