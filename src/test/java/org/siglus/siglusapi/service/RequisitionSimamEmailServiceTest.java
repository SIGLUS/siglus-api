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

package org.siglus.siglusapi.service;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.collections.MapUtils;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.requisition.dto.BasicRequisitionTemplateDto;
import org.openlmis.requisition.dto.FacilityDto;
import org.openlmis.requisition.dto.OrderableDto;
import org.openlmis.requisition.dto.ProcessingPeriodDto;
import org.openlmis.requisition.dto.ProgramDto;
import org.openlmis.requisition.dto.RequisitionLineItemV2Dto;
import org.openlmis.requisition.dto.VersionObjectReferenceDto;
import org.openlmis.requisition.service.referencedata.FacilityReferenceDataService;
import org.openlmis.requisition.service.referencedata.OrderableReferenceDataService;
import org.openlmis.requisition.service.referencedata.PeriodReferenceDataService;
import org.openlmis.requisition.testutils.FacilityDtoDataBuilder;
import org.openlmis.requisition.testutils.OrderableDtoDataBuilder;
import org.openlmis.requisition.testutils.ProcessingPeriodDtoDataBuilder;
import org.openlmis.requisition.testutils.ProgramDtoDataBuilder;
import org.siglus.common.dto.RequisitionTemplateExtensionDto;
import org.siglus.common.util.SiglusDateHelper;
import org.siglus.siglusapi.domain.RegimenLineItem;
import org.siglus.siglusapi.domain.TestConsumptionLineItem;
import org.siglus.siglusapi.domain.UsageInformationLineItem;
import org.siglus.siglusapi.dto.RegimenDto;
import org.siglus.siglusapi.dto.RegimenLineDto;
import org.siglus.siglusapi.dto.SiglusRequisitionDto;
import org.siglus.siglusapi.dto.SiglusUsageTemplateDto;
import org.siglus.siglusapi.dto.TestConsumptionServiceDto;
import org.siglus.siglusapi.dto.UsageInformationServiceDto;
import org.siglus.siglusapi.dto.UsageTemplateColumnDto;
import org.siglus.siglusapi.dto.UsageTemplateSectionDto;
import org.siglus.siglusapi.dto.simam.EmailAttachmentDto;
import org.siglus.siglusapi.util.ExcelHandler;
import org.siglus.siglusapi.util.S3FileHandler;
import org.siglus.siglusapi.util.SingleListSheetExcelHandler;

@RunWith(MockitoJUnitRunner.class)
public class RequisitionSimamEmailServiceTest {

  @Mock
  private S3FileHandler s3FileHandler;

  @Mock
  private SingleListSheetExcelHandler singleListSheetExcelHandler;

  @Mock
  private FacilityReferenceDataService facilityReferenceDataService;

  @Mock
  private OrderableReferenceDataService orderableReferenceDataService;

  @Mock
  private PeriodReferenceDataService periodReferenceDataService;

  @InjectMocks
  private RequisitionSimamEmailService requisitionSimamEmailService;

  private FacilityDto facility = new FacilityDtoDataBuilder().buildAsDto();
  private ProgramDto program = new ProgramDtoDataBuilder().buildAsDto();
  private ProcessingPeriodDto period = new ProcessingPeriodDtoDataBuilder().buildAsDto();
  private OrderableDto orderable = new OrderableDtoDataBuilder()
      .withProgramOrderable(program.getId(), true).buildAsDto();
  private SiglusRequisitionDto requisition = new SiglusRequisitionDto();
  private String requisitionFileName;
  private String regimenFileName;
  private String fileType;
  private UUID regimemId = UUID.randomUUID();

  @Before
  public void setUp() {
    when(periodReferenceDataService.findOne(requisition.getProcessingPeriodId()))
        .thenReturn(period);
    when(facilityReferenceDataService.findOne(requisition.getFacilityId())).thenReturn(facility);
    doNothing().when(s3FileHandler).uploadFileToS3(anyString(), anyString());

    List<OrderableDto> orderableDtos = newArrayList(orderable);
    when(orderableReferenceDataService.findByIdentities(any())).thenReturn(orderableDtos);

    program.setCode(RequisitionSimamEmailService.MULIPLE_PROGRAM_CODE);
    requisitionFileName = RequisitionSimamEmailService.REQUI_FILE_NAME_PREFIX
        + requisition.getId() + "_" + facility.getName() + "_" + period.getName() + "_"
        + RequisitionSimamEmailService.SIMAM_PROGRAMS_MAP.get(program.getCode()) + ".xlsx";
    regimenFileName = RequisitionSimamEmailService.REGIMEN_FILE_NAME_PREFIX
        + requisition.getId() + "_" + facility.getName() + "_" + period.getName() + "_"
        + RequisitionSimamEmailService.SIMAM_PROGRAMS_MAP.get(program.getCode()) + ".xlsx";
    fileType = RequisitionSimamEmailService.FILE_APPLICATION_VND_MS_EXCEL;
    requisition.setCreatedDate(ZonedDateTime.now());
    BasicRequisitionTemplateDto template = new BasicRequisitionTemplateDto();
    template.setExtension(new RequisitionTemplateExtensionDto());
    requisition.setTemplate(template);
  }

  @Test
  public void shouldGetEmailAttachmentList() {
    Workbook workBook = new XSSFWorkbook();
    workBook.createSheet();
    when(singleListSheetExcelHandler.readXssTemplateFile(anyString(),
        any(ExcelHandler.PathType.class))).thenReturn(workBook);
    when(singleListSheetExcelHandler.createXssFile(any(Workbook.class), anyString()))
        .thenReturn(anyString());

    List<EmailAttachmentDto> emailAttachmentDtos = requisitionSimamEmailService
        .prepareEmailAttachmentsForSimam(requisition, program);

    assertEquals(2, emailAttachmentDtos.size());
    assertEquals(emailAttachmentDtos.get(0).getAttachmentFileName(), requisitionFileName);
    assertEquals(emailAttachmentDtos.get(0).getAttachmentFileType(), fileType);
    assertEquals(emailAttachmentDtos.get(1).getAttachmentFileName(), regimenFileName);
    assertEquals(emailAttachmentDtos.get(1).getAttachmentFileType(), fileType);

    verify(singleListSheetExcelHandler, times(1)).readXssTemplateFile(
        RequisitionSimamEmailService.TEMPLATE_IMPORT_RNR_XLSX_EMPTY, ExcelHandler.PathType.FILE);
    verify(singleListSheetExcelHandler, times(1)).readXssTemplateFile(
        RequisitionSimamEmailService.TEMPLATE_IMPORT_REGIMEN_XLSX_EMPTY,
        ExcelHandler.PathType.FILE);
    verify(singleListSheetExcelHandler, times(0)).createDataRows(any(), any());
    verify(singleListSheetExcelHandler, times(1))
        .createXssFile(eq(workBook), eq(requisitionFileName));
    verify(singleListSheetExcelHandler, times(1)).createXssFile(eq(workBook), eq(regimenFileName));
  }

  @Test
  public void shouldGetRequisitionDataItems() {
    VersionObjectReferenceDto versionObjectReferenceDto = new VersionObjectReferenceDto(
        orderable.getId(), null, null, orderable.getVersionNumber());
    RequisitionLineItemV2Dto requisitionLineItem = new RequisitionLineItemV2Dto();
    requisitionLineItem.setOrderable(versionObjectReferenceDto);
    List<RequisitionLineItemV2Dto> requisitionLineItems = newArrayList(requisitionLineItem);
    requisition.setRequisitionLineItems(requisitionLineItems);

    Workbook workBook = new XSSFWorkbook();
    workBook.createSheet();
    when(singleListSheetExcelHandler.readXssTemplateFile(anyString(),
        any(ExcelHandler.PathType.class))).thenReturn(workBook);
    when(singleListSheetExcelHandler.createXssFile(any(Workbook.class),
        anyString())).thenReturn(anyString());

    requisitionSimamEmailService.prepareEmailAttachmentsForSimam(requisition, program);
    verify(singleListSheetExcelHandler, times(1)).readXssTemplateFile(
        RequisitionSimamEmailService.TEMPLATE_IMPORT_RNR_XLSX, ExcelHandler.PathType.FILE);
  }

  @Test
  public void shouldGetArvRegimenDataItems() {
    RegimenLineItem regimenLineItem = new RegimenLineItem(requisition.getId(), regimemId,
        "patients", 20);
    RegimenDto regimenDto = new RegimenDto();
    regimenDto.setFullProductName("fullProductName");
    Map<UUID, RegimenDto> regimenDtoMap = newHashMap();
    regimenDtoMap.put(regimemId, regimenDto);
    List<RegimenLineDto> regimenLineItems = RegimenLineDto
        .from(newArrayList(regimenLineItem), regimenDtoMap);
    requisition.setRegimenLineItems(regimenLineItems);
    requisition.getTemplate().getExtension().setEnableRegimen(true);

    Workbook workBook = new XSSFWorkbook();
    workBook.createSheet();
    when(singleListSheetExcelHandler.readXssTemplateFile(anyString(),
        any(ExcelHandler.PathType.class))).thenReturn(workBook);
    when(singleListSheetExcelHandler.createXssFile(any(Workbook.class),
        anyString())).thenReturn(anyString());

    program.setCode(RequisitionSimamEmailService.ARV_PROGRAM_CODE);
    requisitionSimamEmailService.prepareEmailAttachmentsForSimam(requisition, program);
    verify(singleListSheetExcelHandler, times(1)).readXssTemplateFile(
        RequisitionSimamEmailService.TEMPLATE_IMPORT_RNR_XLSX_EMPTY, ExcelHandler.PathType.FILE);
    verify(singleListSheetExcelHandler, times(1)).readXssTemplateFile(
        RequisitionSimamEmailService.TEMPLATE_IMPORT_REGIMEN_XLSX, ExcelHandler.PathType.FILE);
    Map<String, String> expectedDataColumns = MapUtils.putAll(newHashMap(),
        new String[][]{
            {RequisitionSimamEmailService.EXCEL_MOVDESCID, "0"},
            {RequisitionSimamEmailService.EXCEL_DATE,
                SiglusDateHelper.formatDateTime(requisition.getCreatedDate())},
            {RequisitionSimamEmailService.EXCEL_PROGRAM, "TARV"},
            {RequisitionSimamEmailService.EXCEL_REGIMEN, "fullProductName"},
            {RequisitionSimamEmailService.EXCEL_TOTAL, "20"}
        });
    verify(singleListSheetExcelHandler, times(1))
        .createDataRows(eq(workBook.getSheetAt(0)), eq(newArrayList(expectedDataColumns)));
  }

  @Test
  public void shouldGetMalariaRegimenDataItems() {
    UsageInformationLineItem usageInformationLineItem = UsageInformationLineItem.builder()
        .service("total")
        .information("treatmentsAttended")
        .orderableId(orderable.getId())
        .value(30)
        .build();
    requisition.setUsageInformationLineItems(
        UsageInformationServiceDto.from(newArrayList(usageInformationLineItem)));
    requisition.getTemplate().getExtension().setEnableUsageInformation(true);

    Workbook workBook = new XSSFWorkbook();
    workBook.createSheet();
    when(singleListSheetExcelHandler.readXssTemplateFile(anyString(),
        any(ExcelHandler.PathType.class))).thenReturn(workBook);
    when(singleListSheetExcelHandler.createXssFile(any(Workbook.class),
        anyString())).thenReturn(anyString());

    program.setCode(RequisitionSimamEmailService.AL_PROGRAM_CODE);
    orderable.setProductCode("08O05");
    requisitionSimamEmailService.prepareEmailAttachmentsForSimam(requisition, program);

    verify(singleListSheetExcelHandler, times(1)).readXssTemplateFile(
        RequisitionSimamEmailService.TEMPLATE_IMPORT_RNR_XLSX_EMPTY, ExcelHandler.PathType.FILE);
    verify(singleListSheetExcelHandler, times(1)).readXssTemplateFile(
        RequisitionSimamEmailService.TEMPLATE_IMPORT_REGIMEN_XLSX, ExcelHandler.PathType.FILE);

    Map<String, String> expectedDataColumns = MapUtils.putAll(newHashMap(),
        new String[][]{
            {RequisitionSimamEmailService.EXCEL_MOVDESCID, "0"},
            {RequisitionSimamEmailService.EXCEL_DATE,
                SiglusDateHelper.formatDateTime(requisition.getCreatedDate())},
            {RequisitionSimamEmailService.EXCEL_PROGRAM, "Malaria"},
            {RequisitionSimamEmailService.EXCEL_REGIMEN, "Consultas AL US/APE Malaria 1x6"},
            {RequisitionSimamEmailService.EXCEL_TOTAL, "30"}
        });
    verify(singleListSheetExcelHandler, times(1))
        .createDataRows(eq(workBook.getSheetAt(0)), eq(newArrayList(expectedDataColumns)));
  }

  @Test
  public void shouldGetRapidTestRegimenDataItems() {
    TestConsumptionLineItem testConsumptionLineItem = TestConsumptionLineItem.builder()
        .service("total")
        .project("projectName")
        .outcome("columnName")
        .value(10)
        .build();
    requisition.setTestConsumptionLineItems(
        TestConsumptionServiceDto.from(newArrayList(testConsumptionLineItem)));
    requisition.getTemplate().getExtension().setEnableRapidTestConsumption(true);
    UsageTemplateColumnDto outcomeColumnDto = new UsageTemplateColumnDto();
    outcomeColumnDto.setName("columnName");
    outcomeColumnDto.setLabel("Consumo");
    UsageTemplateColumnDto projectColumnDto = new UsageTemplateColumnDto();
    projectColumnDto.setName("projectName");
    projectColumnDto.setLabel("HIV Determine");
    UsageTemplateSectionDto outcomeSectionDto = new UsageTemplateSectionDto();
    outcomeSectionDto.setName("outcome");
    outcomeSectionDto.setColumns(newArrayList(outcomeColumnDto));
    UsageTemplateSectionDto projectSectionDto = new UsageTemplateSectionDto();
    projectSectionDto.setName("project");
    projectSectionDto.setColumns(newArrayList(projectColumnDto));
    List<UsageTemplateSectionDto> sectionDtos = newArrayList(outcomeSectionDto, projectSectionDto);
    SiglusUsageTemplateDto usageTemplateDto = new SiglusUsageTemplateDto();
    usageTemplateDto.setRapidTestConsumption(sectionDtos);
    requisition.setUsageTemplate(usageTemplateDto);

    Workbook workBook = new XSSFWorkbook();
    workBook.createSheet();
    when(singleListSheetExcelHandler.readXssTemplateFile(anyString(),
        any(ExcelHandler.PathType.class))).thenReturn(workBook);
    when(singleListSheetExcelHandler.createXssFile(any(Workbook.class),
        anyString())).thenReturn(anyString());

    program.setCode(RequisitionSimamEmailService.RAPID_TEST_PROGRAM_CODE);
    requisitionSimamEmailService.prepareEmailAttachmentsForSimam(requisition, program);

    verify(singleListSheetExcelHandler, times(1)).readXssTemplateFile(
        RequisitionSimamEmailService.TEMPLATE_IMPORT_RNR_XLSX_EMPTY, ExcelHandler.PathType.FILE);
    verify(singleListSheetExcelHandler, times(1)).readXssTemplateFile(
        RequisitionSimamEmailService.TEMPLATE_IMPORT_REGIMEN_XLSX, ExcelHandler.PathType.FILE);

    Map<String, String> expectedDataColumns = MapUtils.putAll(newHashMap(),
        new String[][]{
            {RequisitionSimamEmailService.EXCEL_MOVDESCID, "0"},
            {RequisitionSimamEmailService.EXCEL_DATE,
                SiglusDateHelper.formatDateTime(requisition.getCreatedDate())},
            {RequisitionSimamEmailService.EXCEL_PROGRAM, "Testes Rápidos Diag."},
            {RequisitionSimamEmailService.EXCEL_REGIMEN, "HIV Determine Consumo"},
            {RequisitionSimamEmailService.EXCEL_TOTAL, "10"}
        });
    verify(singleListSheetExcelHandler, times(1))
        .createDataRows(eq(workBook.getSheetAt(0)), eq(newArrayList(expectedDataColumns)));
  }
}