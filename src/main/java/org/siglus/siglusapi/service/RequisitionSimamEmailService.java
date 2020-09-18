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
import static org.apache.commons.collections4.CollectionUtils.isEmpty;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.collections.MapUtils;
import org.apache.poi.ss.usermodel.Workbook;
import org.openlmis.requisition.domain.requisition.VersionEntityReference;
import org.openlmis.requisition.dto.BaseRequisitionLineItemDto;
import org.openlmis.requisition.dto.FacilityDto;
import org.openlmis.requisition.dto.OrderableDto;
import org.openlmis.requisition.dto.ProcessingPeriodDto;
import org.openlmis.requisition.dto.ProgramDto;
import org.openlmis.requisition.repository.RequisitionRepository;
import org.openlmis.requisition.service.referencedata.FacilityReferenceDataService;
import org.openlmis.requisition.service.referencedata.OrderableReferenceDataService;
import org.openlmis.requisition.service.referencedata.PeriodReferenceDataService;
import org.siglus.common.dto.RequisitionTemplateExtensionDto;
import org.siglus.common.util.SiglusDateHelper;
import org.siglus.siglusapi.dto.RegimenDto;
import org.siglus.siglusapi.dto.RegimenLineDto;
import org.siglus.siglusapi.dto.SiglusRequisitionDto;
import org.siglus.siglusapi.dto.TestConsumptionServiceDto;
import org.siglus.siglusapi.dto.UsageInformationOrderableDto;
import org.siglus.siglusapi.dto.UsageInformationServiceDto;
import org.siglus.siglusapi.dto.UsageTemplateColumnDto;
import org.siglus.siglusapi.dto.UsageTemplateSectionDto;
import org.siglus.siglusapi.dto.simam.EmailAttachmentDto;
import org.siglus.siglusapi.util.ExcelHandler;
import org.siglus.siglusapi.util.S3FileHandler;
import org.siglus.siglusapi.util.SingleListSheetExcelHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class RequisitionSimamEmailService {

  public static final String TEMPLATE_IMPORT_RNR_XLSX = "template_Simam_import_Requi.xlsx";
  public static final String TEMPLATE_IMPORT_RNR_XLSX_EMPTY =
      "template_Simam_import_Requi_EMPTY.xlsx";
  public static final String TEMPLATE_IMPORT_REGIMEN_XLSX = "template_Simam_import_Regimen.xlsx";
  public static final String TEMPLATE_IMPORT_REGIMEN_XLSX_EMPTY =
      "template_Simam_import_Regimen_EMPTY.xlsx";
  public static final String REGIMEN_FILE_NAME_PREFIX = "Regimen_Requi";
  public static final String REQUI_FILE_NAME_PREFIX = "Requi";
  public static final String FILE_APPLICATION_VND_MS_EXCEL = "application/excel";
  public static final String MULIPLE_PROGRAM_CODE = "MP";
  public static final String AL_PROGRAM_CODE = "ALP";
  public static final String ARV_PROGRAM_CODE = "ARVP";
  public static final String RAPID_TEST_PROGRAM_CODE = "RTP";
  public static final String EXCEL_FACILITY = "facility_name";
  public static final String EXCEL_DATE = "date";
  public static final String EXCEL_PRODUCT = "product_code";
  public static final String EXCEL_BOP = "beginning_balance";
  public static final String EXCEL_QUANTITY_DIDPENSED = "quantity_dispensed";
  public static final String EXCEL_QUANTITY_RECEIVED = "quantity_received";
  public static final String EXCEL_EMPREST = "emprest";
  public static final String EXCEL_ADJUST = "total_losses_and_adjustments";
  public static final String EXCEL_STOCK_IN_HAND = "stock_in_hand";
  public static final String EXCEL_QUANTITY_REQUESTED = "quantity_requested";
  public static final String EXCEL_INVENTORY = "inventory";
  public static final String EXCEL_SERVICE_QUANTITY = "total_service_quantity";
  public static final String EXCEL_QUANTITY_APPROVED = "quantity_approved";
  public static final String EXCEL_PROGRAM = "program_code";
  public static final String EXCEL_MOVDESCID = "movDescID";
  public static final String EXCEL_REGIMEN = "regimen_name";
  public static final String EXCEL_TOTAL = "total";
  public static final String TOTAL_SERVICE = "total";

  @Value("${email.attachment.s3.bucket}")
  private String bucketName;
  @Value("${email.attachment.s3.bucket.folder}")
  private String bucketFolder;

  @Autowired
  private SingleListSheetExcelHandler singleListSheetExcelHandler;

  @Autowired
  private S3FileHandler s3FileHandler;

  @Autowired
  RequisitionRepository requisitionRepository;

  @Autowired
  FacilityReferenceDataService facilityReferenceDataService;

  @Autowired
  OrderableReferenceDataService orderableReferenceDataService;

  @Autowired
  private PeriodReferenceDataService periodReferenceDataService;

  protected static final Map<String, String> SIMAM_PROGRAMS_MAP = MapUtils.putAll(newHashMap(),
      new String[][]{
          {ARV_PROGRAM_CODE, "TARV"},
          {MULIPLE_PROGRAM_CODE, "Via Clássica"},
          {AL_PROGRAM_CODE, "Malaria"},
          {RAPID_TEST_PROGRAM_CODE, "Testes Rápidos Diag."}});


  private static final Map<String, String> AL_REGIMEN_MAP = MapUtils.putAll(newHashMap(),
      new String[][]{
          {"08O05", "Consultas AL US/APE Malaria 1x6"},
          {"08O05Z", "Consultas AL US/APE Malaria 2x6"},
          {"08O05Y", "Consultas AL US/APE Malaria 3x6"},
          {"08O05X", "Consultas AL US/APE Malaria 4x6"}});

  private static final Map<String, String> RAPID_TEST_REGIMEN_MAP = MapUtils.putAll(newHashMap(),
      new String[][]{
          {"CONSUMO_HIVDETERMINE", "HIV Determine Consumo"},
          {"POSITIVE_HIVDETERMINE", "HIV Determine Positivos +"},
          {"UNJUSTIFIED_HIVDETERMINE", "HIV Determine Injustificado"},
          {"CONSUMO_HIVUNIGOLD", "HIV Unigold Consumo"},
          {"POSITIVE_HIVUNIGOLD", "HIV Unigold Positivos +"},
          {"UNJUSTIFIED_HIVUNIGOLD", "HIV Unigold Injustificado"},
          {"CONSUMO_SYPHILLIS", "Sífilis Teste Rápido Consumo"},
          {"POSITIVE_SYPHILLIS", "Sífilis Teste Positivos +"},
          {"UNJUSTIFIED_SYPHILLIS", "Sífilis Teste Rápido Injustificado"},
          {"CONSUMO_MALARIA", "Malaria Teste Rápido Consumo"},
          {"POSITIVE_MALARIA", "Malaria Teste Positivos +"},
          {"UNJUSTIFIED_MALARIA", "Malaria Teste Rápido Injustificado"}});


  public List<EmailAttachmentDto> prepareEmailAttachmentsForSimam(
      SiglusRequisitionDto requisition, ProgramDto program) {
    ProcessingPeriodDto period =
        periodReferenceDataService.findOne(requisition.getProcessingPeriodId());
    FacilityDto facility = facilityReferenceDataService.findOne(requisition.getFacilityId());

    List<EmailAttachmentDto> emailAttachments = new ArrayList<>();
    EmailAttachmentDto attachmentForRequisition = loadSimamRequisitionExcelToS3(requisition,
        program, period, facility);
    emailAttachments.add(attachmentForRequisition);

    EmailAttachmentDto attachmentForRegimen = loadSimamRegimenExcelToS3(requisition,
        program, period, facility);
    emailAttachments.add(attachmentForRegimen);

    return emailAttachments;
  }

  private EmailAttachmentDto loadSimamRequisitionExcelToS3(SiglusRequisitionDto requisition,
      ProgramDto program, ProcessingPeriodDto period, FacilityDto facility) {
    String filePath = generateRequisitionExcelForSimam(requisition, program, period, facility);
    String fileName = formatFileName(requisition.getId(), program, period, facility,
        REQUI_FILE_NAME_PREFIX);
    s3FileHandler.uploadFileToS3(filePath, fileName);

    return new EmailAttachmentDto(bucketName, bucketFolder, fileName,
        FILE_APPLICATION_VND_MS_EXCEL);
  }

  private EmailAttachmentDto loadSimamRegimenExcelToS3(SiglusRequisitionDto requisition,
      ProgramDto program, ProcessingPeriodDto period, FacilityDto facility) {
    String filePath = generateRegimenExcelForSimam(requisition, program, period, facility);
    String fileName = formatFileName(requisition.getId(), program, period, facility,
        REGIMEN_FILE_NAME_PREFIX);
    s3FileHandler.uploadFileToS3(filePath, fileName);
    return new EmailAttachmentDto(bucketName, bucketFolder, fileName,
        FILE_APPLICATION_VND_MS_EXCEL);
  }

  private String generateRequisitionExcelForSimam(SiglusRequisitionDto requisition,
      ProgramDto program, ProcessingPeriodDto period, FacilityDto facility) {
    Workbook workbook;

    List<Map<String, String>> requisitionItemsData = getDataColumnsForRequisition(
        requisition, program, facility);
    if (isEmpty(requisitionItemsData)) {
      workbook = singleListSheetExcelHandler.readXssTemplateFile(
          TEMPLATE_IMPORT_RNR_XLSX_EMPTY, ExcelHandler.PathType.FILE);
    } else {
      workbook = singleListSheetExcelHandler.readXssTemplateFile(
          TEMPLATE_IMPORT_RNR_XLSX, ExcelHandler.PathType.FILE);
      singleListSheetExcelHandler.createDataRows(workbook.getSheetAt(0), requisitionItemsData);
    }

    String fileName = formatFileName(requisition.getId(), program, period, facility,
        REQUI_FILE_NAME_PREFIX);
    return singleListSheetExcelHandler.createXssFile(workbook, fileName);
  }

  private List<Map<String, String>> getDataColumnsForRequisition(SiglusRequisitionDto requisition,
      ProgramDto program, FacilityDto facility) {

    List<Map<String, String>> dataColumns = new ArrayList<>();
    List<BaseRequisitionLineItemDto> requisitionLineItems = requisition.getLineItems();
    if (isEmpty(requisitionLineItems)) {
      return dataColumns;
    }
    Set<VersionEntityReference> orderableIds = requisitionLineItems
        .stream()
        .map(BaseRequisitionLineItemDto::getOrderableIdentity)
        .map(versionIdentityDto -> new VersionEntityReference(versionIdentityDto.getId(),
            versionIdentityDto.getVersionNumber()))
        .collect(Collectors.toSet());
    Map<UUID, OrderableDto> orderables = findOrderables(orderableIds);
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    requisitionLineItems.forEach(item -> {
      Map<String, String> dataColumn = new HashMap<>();
      dataColumn.put(EXCEL_FACILITY, facility.getName());
      dataColumn.put(EXCEL_DATE, requisition.getCreatedDate().format(formatter));
      OrderableDto orderable = orderables.get(item.getOrderableIdentity().getId());
      dataColumn.put(EXCEL_PRODUCT, "a" + orderable.getProductCode());
      dataColumn.put(EXCEL_BOP, getString(item.getBeginningBalance()));
      dataColumn.put(EXCEL_QUANTITY_DIDPENSED, getString(item.getTotalConsumedQuantity()));
      dataColumn.put(EXCEL_QUANTITY_RECEIVED, getString(item.getTotalReceivedQuantity()));
      dataColumn.put(EXCEL_EMPREST, "0");
      dataColumn.put(EXCEL_ADJUST, getString(getTotalLossesAndAdjustments(item)));
      dataColumn.put(EXCEL_STOCK_IN_HAND, getString(item.getStockOnHand()));
      dataColumn.put(EXCEL_QUANTITY_REQUESTED, getString(item.getRequestedQuantity()));
      dataColumn.put(EXCEL_INVENTORY, getString(item.getStockOnHand()));
      dataColumn.put(EXCEL_SERVICE_QUANTITY, "");
      dataColumn.put(EXCEL_QUANTITY_APPROVED, getString(item.getAuthorizedQuantity()));
      dataColumn.put(EXCEL_PROGRAM, SIMAM_PROGRAMS_MAP.get(program.getCode()));

      dataColumns.add(dataColumn);
    });

    return dataColumns;
  }

  private String getString(Object object) {
    if (object != null) {
      return object.toString();
    }
    return null;
  }

  private Integer getTotalLossesAndAdjustments(BaseRequisitionLineItemDto requisitionLineItem) {
    Integer totalLossesAndAdjustments = requisitionLineItem.getTotalLossesAndAdjustments();
    if (totalLossesAndAdjustments != null) {
      return totalLossesAndAdjustments;
    }
    Integer stockOnHand = requisitionLineItem.getStockOnHand();
    Integer beginningBalance = requisitionLineItem.getBeginningBalance();
    Integer totalReceivedQuantity = requisitionLineItem.getTotalReceivedQuantity();
    Integer totalConsumedQuantity = requisitionLineItem.getTotalConsumedQuantity();

    if (stockOnHand == null || beginningBalance == null || totalReceivedQuantity == null
        || totalConsumedQuantity == null) {
      return null;
    }
    return stockOnHand - (beginningBalance + totalReceivedQuantity - totalConsumedQuantity);
  }

  private String generateRegimenExcelForSimam(SiglusRequisitionDto requisition,
      ProgramDto program, ProcessingPeriodDto period, FacilityDto facility) {
    Workbook workbook;
    List<Map<String, String>> regimenItemsData = getDataColumnsForRegimen(requisition, program);
    if (regimenItemsData.isEmpty()) {
      workbook = singleListSheetExcelHandler.readXssTemplateFile(
          TEMPLATE_IMPORT_REGIMEN_XLSX_EMPTY, ExcelHandler.PathType.FILE);
    } else {
      workbook = singleListSheetExcelHandler.readXssTemplateFile(
          TEMPLATE_IMPORT_REGIMEN_XLSX, ExcelHandler.PathType.FILE);
      singleListSheetExcelHandler.createDataRows(workbook.getSheetAt(0), regimenItemsData);

    }
    String fileName = formatFileName(requisition.getId(), program, period, facility,
        REGIMEN_FILE_NAME_PREFIX);
    return singleListSheetExcelHandler.createXssFile(workbook, fileName);
  }

  private List<Map<String, String>> getDataColumnsForRegimen(SiglusRequisitionDto requisition,
      ProgramDto program) {
    List<Map<String, String>> dataColumns = newArrayList();
    RequisitionTemplateExtensionDto templateExtensionDto = requisition.getTemplate().getExtension();
    if (templateExtensionDto.isEnableUsageInformation()) {
      dataColumns.addAll(getMalariaDataColumns(requisition, program));
    }
    if (templateExtensionDto.isEnableRapidTestConsumption()) {
      dataColumns.addAll(getRapidTestDataColumns(requisition, program));
    }
    if (templateExtensionDto.isEnableRegimen()) {
      dataColumns.addAll(getArvDataColumns(requisition, program));
    }
    return dataColumns;
  }

  private List<Map<String, String>> getMalariaDataColumns(SiglusRequisitionDto requisition,
      ProgramDto program) {
    List<Map<String, String>> malariaDataColumns = new ArrayList<>();
    List<UsageInformationServiceDto> alModuleLineItems = requisition.getUsageInformationLineItems();
    if (isEmpty(alModuleLineItems)) {
      return malariaDataColumns;
    }
    alModuleLineItems.forEach(alModuleLineItem -> {
      if (alModuleLineItem.getService().equals(TOTAL_SERVICE)) {
        Map<UUID, UsageInformationOrderableDto> usageInformationOrderableDtoMap = alModuleLineItem
            .getInformations().get("treatmentsAttended").getOrderables();
        Set<VersionEntityReference> orderableIds = usageInformationOrderableDtoMap.keySet().stream()
            .map(orderableId -> new VersionEntityReference(orderableId, 1L))
            .collect(Collectors.toSet());
        Map<UUID, OrderableDto> orderableDtoMap = findOrderables(orderableIds);
        usageInformationOrderableDtoMap.forEach((orderableId, usageValue) -> {
          Map<String, String> dataColumns = getCommonDataColumnsForRegimen(requisition, program);
          dataColumns.put(EXCEL_REGIMEN, getMalariaRegimeName(orderableDtoMap.get(orderableId)));
          dataColumns.put(EXCEL_TOTAL, getString(usageValue.getValue()));
          malariaDataColumns.add(dataColumns);
        });
      }
    });

    return malariaDataColumns;
  }

  private String getMalariaRegimeName(OrderableDto orderable) {
    if (orderable == null) {
      return null;
    }
    String regimenNameFromSimam = AL_REGIMEN_MAP.get(orderable.getProductCode());
    return regimenNameFromSimam == null ? orderable.getFullProductName() : regimenNameFromSimam;
  }

  private List<Map<String, String>> getArvDataColumns(SiglusRequisitionDto requisition,
      ProgramDto program) {
    List<Map<String, String>> arvDataColumns = new ArrayList<>();
    List<RegimenLineDto> regimenLineItems = requisition.getRegimenLineItems();
    if (isEmpty(regimenLineItems)) {
      return arvDataColumns;
    }
    regimenLineItems.stream()
        .filter(lineItem -> lineItem.getRegimen() != null)
        .forEach(regimenLineItem -> {
          Map<String, String> dataColumns = getCommonDataColumnsForRegimen(requisition, program);
          RegimenDto regimen = regimenLineItem.getRegimen();
          Integer value = regimenLineItem.getColumns().get("patients").getValue();
          dataColumns.put(EXCEL_REGIMEN, regimen.getFullProductName());
          dataColumns.put(EXCEL_TOTAL, getString(value));
          arvDataColumns.add(dataColumns);
        });
    return arvDataColumns;
  }

  private List<Map<String, String>> getRapidTestDataColumns(SiglusRequisitionDto requisition,
      ProgramDto program) {
    List<Map<String, String>> rapidTestDataColumns = new ArrayList<>();
    List<TestConsumptionServiceDto> serviceLineItems = requisition.getTestConsumptionLineItems();
    if (isEmpty(serviceLineItems)) {
      return rapidTestDataColumns;
    }
    Map<String, List<UsageTemplateColumnDto>> usageTemplateMap = requisition.getUsageTemplate()
        .getRapidTestConsumption()
        .stream()
        .collect(Collectors.toMap(UsageTemplateSectionDto::getName,
            UsageTemplateSectionDto::getColumns));
    Map<String, String> projectLableMap = usageTemplateMap.get("project").stream().collect(
        Collectors.toMap(UsageTemplateColumnDto::getName, UsageTemplateColumnDto::getLabel));
    Map<String, String> outcomeLableMap = usageTemplateMap.get("outcome").stream().collect(
        Collectors.toMap(UsageTemplateColumnDto::getName, UsageTemplateColumnDto::getLabel));
    serviceLineItems
        .stream()
        .filter(serviceLineItem -> serviceLineItem.getService().equals(TOTAL_SERVICE))
        .forEach(serviceLineItem ->
            serviceLineItem.getProjects().forEach((project, projectDto) ->
                projectDto.getOutcomes().forEach((outcome, testConsumptionValue) -> {
                  Map<String, String> dataColumns = getCommonDataColumnsForRegimen(requisition,
                      program);
                  dataColumns.put(EXCEL_REGIMEN,
                      getRapidTestRegimenName(projectLableMap.get(project),
                          outcomeLableMap.get(outcome)));
                  dataColumns.put(EXCEL_TOTAL, getString(testConsumptionValue.getValue()));
                  rapidTestDataColumns.add(dataColumns);
                })
            ));
    return rapidTestDataColumns;
  }

  private String getRapidTestRegimenName(String project, String outcome) {
    String regimenNameFromSimam = RAPID_TEST_REGIMEN_MAP
        .get((outcome + "_" + project.replace(" ", "")).toUpperCase());
    return regimenNameFromSimam == null ? project + " " + outcome : regimenNameFromSimam;
  }

  private Map<String, String> getCommonDataColumnsForRegimen(SiglusRequisitionDto requisition,
      ProgramDto program) {
    Map<String, String> commonDataColumns = newHashMap();
    commonDataColumns.put(EXCEL_MOVDESCID, "0");
    commonDataColumns
        .put(EXCEL_DATE, SiglusDateHelper.formatDateTime(requisition.getCreatedDate()));
    commonDataColumns.put(EXCEL_PROGRAM, SIMAM_PROGRAMS_MAP.get(program.getCode()));

    return commonDataColumns;
  }

  private String formatFileName(UUID requisitionId, ProgramDto program,
      ProcessingPeriodDto period, FacilityDto facility, String fileNamePrefix) {
    String programName = SIMAM_PROGRAMS_MAP.get(program.getCode());
    return String.format("%s%s_%s_%s_%s.xlsx", fileNamePrefix, requisitionId, facility.getName(),
        period.getName(), programName);
  }

  private Map<UUID, OrderableDto> findOrderables(Set<VersionEntityReference> orderables) {
    return orderableReferenceDataService
        .findByIdentities(orderables)
        .stream()
        .collect(Collectors.toMap(OrderableDto::getId, Function.identity()));
  }

}
