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

package org.siglus.siglusapi.service.export;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.siglus.siglusapi.constant.android.UsageSectionConstants.TestConsumptionLineItems.PROJECT_CONSUMO;
import static org.siglus.siglusapi.constant.android.UsageSectionConstants.TestConsumptionLineItems.PROJECT_HIVDETERMINE;
import static org.siglus.siglusapi.constant.android.UsageSectionConstants.TestConsumptionLineItems.SERVICE_HF;

import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.context.WriteContext;
import com.alibaba.excel.write.metadata.holder.WriteSheetHolder;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.referencedata.domain.Code;
import org.openlmis.referencedata.domain.Facility;
import org.openlmis.referencedata.domain.Orderable;
import org.openlmis.requisition.domain.requisition.RequisitionStatus;
import org.openlmis.requisition.dto.ObjectReferenceDto;
import org.openlmis.requisition.dto.OrderableDto;
import org.openlmis.requisition.dto.ProcessingPeriodDto;
import org.openlmis.requisition.dto.ProgramOrderableDto;
import org.openlmis.requisition.dto.RequisitionLineItemV2Dto;
import org.openlmis.requisition.dto.StatusChangeDto;
import org.openlmis.requisition.dto.StatusMessageDto;
import org.siglus.siglusapi.dto.AgeGroupLineItemDto;
import org.siglus.siglusapi.dto.AgeGroupServiceDto;
import org.siglus.siglusapi.dto.GeographicProvinceDistrictDto;
import org.siglus.siglusapi.dto.PatientColumnDto;
import org.siglus.siglusapi.dto.PatientGroupDto;
import org.siglus.siglusapi.dto.RegimenColumnDto;
import org.siglus.siglusapi.dto.RegimenLineDto;
import org.siglus.siglusapi.dto.SiglusRequisitionDto;
import org.siglus.siglusapi.dto.TestConsumptionOutcomeDto;
import org.siglus.siglusapi.dto.TestConsumptionProjectDto;
import org.siglus.siglusapi.dto.TestConsumptionServiceDto;
import org.siglus.siglusapi.repository.OrderableRepository;
import org.siglus.siglusapi.repository.SiglusFacilityRepository;
import org.siglus.siglusapi.repository.SiglusGeographicInfoRepository;
import org.siglus.siglusapi.service.SiglusProcessingPeriodService;

@RunWith(MockitoJUnitRunner.class)
public class RapidTestRequisitionReportServiceTest {

  @InjectMocks
  private RapidTestRequisitionReportService rapidTestRequisitionReportService;

  @Mock
  private SiglusGeographicInfoRepository siglusGeographicInfoRepository;
  @Mock
  private SiglusFacilityRepository siglusFacilityRepository;
  @Mock
  private SiglusProcessingPeriodService siglusProcessingPeriodService;
  @Mock
  private OrderableRepository orderableRepository;
  @Mock
  private ExcelWriter excelWriter;

  @Test
  public void shouldGenerateExcelSuccessForTr() {
    // given
    UUID requisitionId = UUID.randomUUID();
    SiglusRequisitionDto dto = new SiglusRequisitionDto();
    dto.setId(requisitionId);
    dto.setStatus(RequisitionStatus.APPROVED);
    dto.setRequisitionNumber("123456");
    dto.setCreatedDate(ZonedDateTime.now());
    Map<String, Object> extraDataMap = new HashMap<>();
    Map<String, Object> signatureMap = new HashMap<>();
    signatureMap.put("authorize", "aaa");
    signatureMap.put("approve", Lists.newArrayList("bbb"));
    extraDataMap.put("signaure", signatureMap);
    dto.setExtraData(extraDataMap);
    UUID orderableId = UUID.randomUUID();
    OrderableDto orderableDto = new OrderableDto();
    orderableDto.setId(orderableId);
    ProgramOrderableDto programOrderableDto = new ProgramOrderableDto();
    programOrderableDto.setOrderableCategoryDisplayName("category");
    orderableDto.setPrograms(Sets.newHashSet(programOrderableDto));
    RequisitionLineItemV2Dto requisitionLineItemV2Dto = new RequisitionLineItemV2Dto();
    requisitionLineItemV2Dto.setOrderable(orderableDto);
    List<RequisitionLineItemV2Dto> requisitionLineItemV2Dtos = new ArrayList<>();
    requisitionLineItemV2Dtos.add(requisitionLineItemV2Dto);
    dto.setRequisitionLineItems(requisitionLineItemV2Dtos);
    UUID facilityId = UUID.randomUUID();
    ObjectReferenceDto objectReferenceDto = new ObjectReferenceDto();
    objectReferenceDto.setId(facilityId);
    dto.setFacility(objectReferenceDto);
    UUID processingPeriodId = UUID.randomUUID();
    ObjectReferenceDto objectReferenceDto2 = new ObjectReferenceDto();
    objectReferenceDto2.setId(processingPeriodId);
    dto.setProcessingPeriod(objectReferenceDto2);
    PatientGroupDto patientGroupDto = new PatientGroupDto();
    patientGroupDto.setName("name");
    Map<String, PatientColumnDto> patientColumnMap = new HashMap<>();
    PatientColumnDto patientColumnDto = new PatientColumnDto();
    patientColumnDto.setValue(10);
    patientColumnMap.put("key", patientColumnDto);
    patientGroupDto.setColumns(patientColumnMap);
    List<PatientGroupDto> patientLineItems = new ArrayList<>();
    patientLineItems.add(patientGroupDto);
    dto.setPatientLineItems(patientLineItems);
    AgeGroupServiceDto ageGroupServiceDto = new AgeGroupServiceDto();
    ageGroupServiceDto.setService("service");
    Map<String, AgeGroupLineItemDto> ageColumnMap = new HashMap<>();
    AgeGroupLineItemDto ageGroupLineItemDto = new AgeGroupLineItemDto();
    ageColumnMap.put("key", ageGroupLineItemDto);
    ageGroupServiceDto.setColumns(ageColumnMap);
    List<AgeGroupServiceDto> ageGroupLineItems = new ArrayList<>();
    ageGroupLineItems.add(ageGroupServiceDto);
    dto.setAgeGroupLineItems(ageGroupLineItems);
    RegimenLineDto totalRegimen = new RegimenLineDto();
    totalRegimen.setName("total");
    Map<String, RegimenColumnDto> totalRegimenMap = new HashMap<>();
    RegimenColumnDto regimenColumnDto = new RegimenColumnDto();
    regimenColumnDto.setValue(10);
    totalRegimenMap.put("community", regimenColumnDto);
    totalRegimen.setColumns(totalRegimenMap);
    dto.setRegimenLineItems(Lists.newArrayList(totalRegimen));
    StatusChangeDto statusChangeDto = new StatusChangeDto();
    StatusMessageDto statusMessageDto = new StatusMessageDto();
    statusMessageDto.setBody("comment");
    statusChangeDto.setStatusMessageDto(statusMessageDto);
    dto.setStatusHistory(Lists.newArrayList(statusChangeDto));

    Facility facility = new Facility();
    facility.setId(facilityId);
    facility.setCode("code");
    when(siglusFacilityRepository.findOne(facilityId)).thenReturn(facility);
    GeographicProvinceDistrictDto geographicProvinceDistrictDto = new GeographicProvinceDistrictDto();
    geographicProvinceDistrictDto.setProvinceName("province");
    geographicProvinceDistrictDto.setDistrictName("district");
    when(siglusGeographicInfoRepository
        .getGeographicProvinceDistrictInfo(facility.getCode())).thenReturn(geographicProvinceDistrictDto);
    ProcessingPeriodDto processingPeriodDto = new ProcessingPeriodDto();
    processingPeriodDto.setId(processingPeriodId);
    processingPeriodDto.setEndDate(LocalDate.now());
    when(siglusProcessingPeriodService.getProcessingPeriodDto(processingPeriodId)).thenReturn(processingPeriodDto);
    Orderable orderable =
        new Orderable(Code.code("productCode"), null, 0, 0, Boolean.FALSE, orderableId, 0L);
    orderable.setFullProductName("productName");
    when(orderableRepository.findLatestByIds(Sets.newHashSet(orderableId))).thenReturn(Lists.newArrayList(orderable));
    WriteContext writeContext = Mockito.mock(WriteContext.class);
    when(excelWriter.writeContext()).thenReturn(writeContext);
    WriteSheetHolder writeSheetHolder = mock(WriteSheetHolder.class);
    when(writeContext.writeSheetHolder()).thenReturn(writeSheetHolder);
    Sheet sheet = mock(Sheet.class);
    when(writeSheetHolder.getCachedSheet()).thenReturn(sheet);
    for (int i = 20; i <= 58; i++) {
      Row row = mock(Row.class);
      when(sheet.getRow(i)).thenReturn(row);
    }
    for (int i = 6; i <= 44; i++) {
      Row row = mock(Row.class);
      when(sheet.getRow(i)).thenReturn(row);
    }

    TestConsumptionServiceDto testConsumptionServiceDto = new TestConsumptionServiceDto();
    testConsumptionServiceDto.setService(SERVICE_HF);
    TestConsumptionProjectDto testConsumptionProjectDto = new TestConsumptionProjectDto();
    testConsumptionProjectDto.setProject(PROJECT_HIVDETERMINE);
    Map<String, TestConsumptionOutcomeDto> outcomes = new HashMap<>();
    TestConsumptionOutcomeDto testConsumptionOutcomeDto = new TestConsumptionOutcomeDto();
    testConsumptionOutcomeDto.setValue(10);
    outcomes.put(PROJECT_CONSUMO, testConsumptionOutcomeDto);
    testConsumptionProjectDto.setOutcomes(outcomes);
    Map<String, TestConsumptionProjectDto> projects = new HashMap<>();
    projects.put(PROJECT_HIVDETERMINE, testConsumptionProjectDto);
    testConsumptionServiceDto.setProjects(projects);
    dto.setTestConsumptionLineItems(Lists.newArrayList(testConsumptionServiceDto));
    // when
    rapidTestRequisitionReportService.generateReport(dto, excelWriter);

    // then
    verify(siglusGeographicInfoRepository).getGeographicProvinceDistrictInfo(any());
    verify(siglusProcessingPeriodService).getProcessingPeriodDto(any());
    verify(orderableRepository).findLatestByIds(any());
    verify(siglusFacilityRepository).findOne(any(UUID.class));
  }
}
