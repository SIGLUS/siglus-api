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

import static java.util.stream.Collectors.toSet;
import static org.openlmis.requisition.domain.requisition.Requisition.AI;
import static org.openlmis.requisition.domain.requisition.Requisition.DPM;
import static org.openlmis.stockmanagement.i18n.MessageKeys.ERROR_REPORTING_TEMPLATE_NOT_FOUND_WITH_NAME;
import static org.siglus.common.constant.ExtraDataConstants.ACTUAL_END_DATE;
import static org.siglus.common.constant.ExtraDataConstants.ACTUAL_START_DATE;
import static org.siglus.common.constant.ProgramConstants.MALARIA_PROGRAM_CODE;
import static org.siglus.common.constant.ProgramConstants.MTB_PROGRAM_CODE;
import static org.siglus.common.constant.ProgramConstants.RAPIDTEST_PROGRAM_CODE;
import static org.siglus.common.constant.ProgramConstants.TARV_PROGRAM_CODE;
import static org.siglus.siglusapi.constant.FacilityTypeConstants.ODF;
import static org.siglus.siglusapi.constant.FieldConstants.CONSUMED;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.ValidatorFactory;
import lombok.extern.slf4j.Slf4j;
import org.openlmis.referencedata.domain.Orderable;
import org.openlmis.referencedata.dto.OrderableDto;
import org.openlmis.requisition.dto.ProcessingPeriodDto;
import org.openlmis.requisition.dto.RequisitionV2Dto;
import org.openlmis.requisition.dto.VersionIdentityDto;
import org.openlmis.requisition.dto.stockmanagement.StockCardRangeSummaryDto;
import org.openlmis.requisition.service.PeriodService;
import org.openlmis.requisition.service.referencedata.FacilityReferenceDataService;
import org.openlmis.requisition.service.stockmanagement.StockCardRangeSummaryStockManagementService;
import org.openlmis.stockmanagement.exception.ValidationMessageException;
import org.openlmis.stockmanagement.util.Message;
import org.siglus.common.domain.ProgramAdditionalOrderable;
import org.siglus.common.repository.OrderableKitRepository;
import org.siglus.common.repository.ProgramAdditionalOrderableRepository;
import org.siglus.siglusapi.domain.KitUsageLineItem;
import org.siglus.siglusapi.domain.UsageCategory;
import org.siglus.siglusapi.domain.UsageTemplateColumn;
import org.siglus.siglusapi.domain.UsageTemplateColumnSection;
import org.siglus.siglusapi.dto.KitUsageLineItemDto;
import org.siglus.siglusapi.dto.KitUsageServiceLineItemDto;
import org.siglus.siglusapi.dto.RequisitionActionSequence;
import org.siglus.siglusapi.dto.SiglusRequisitionDto;
import org.siglus.siglusapi.dto.SiglusUsageTemplateDto;
import org.siglus.siglusapi.dto.UsageTemplateSectionDto;
import org.siglus.siglusapi.repository.KitUsageLineItemRepository;
import org.siglus.siglusapi.repository.RequisitionExtensionRepository;
import org.siglus.siglusapi.repository.UsageTemplateColumnSectionRepository;
import org.slf4j.profiler.Profiler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@SuppressWarnings("PMD.TooManyMethods")
public class SiglusUsageReportService {

  static final String KIT_COLLECTION = "collection";
  static final String KIT_SERVICE = "service";
  static final String SERVICE_HF = "HF";
  static final String CALCULATE_FROM_STOCK_CARD = "STOCK_CARDS";

  @Autowired
  UsageTemplateColumnSectionRepository columnSectionRepository;

  @Autowired
  OrderableKitRepository orderableKitRepository;

  @Autowired
  StockCardRangeSummaryStockManagementService stockCardRangeSummaryStockManagementService;

  @Autowired
  KitUsageLineItemRepository kitUsageRepository;

  @Autowired
  private List<UsageReportDataProcessor> usageReportDataProcessors;

  @Autowired
  private ValidatorFactory validatorFactory;

  @Autowired
  private ProgramAdditionalOrderableRepository programAdditionalOrderableRepository;

  @Autowired
  private PeriodService periodService;

  @Autowired
  FacilityReferenceDataService facilityReferenceDataService;

  @Autowired
  SiglusProgramService siglusProgramService;

  @Autowired
  RequisitionExtensionRepository requisitionExtensionRepository;

  public SiglusRequisitionDto searchUsageReport(RequisitionV2Dto requisitionV2Dto) {
    SiglusRequisitionDto siglusRequisitionDto = SiglusRequisitionDto.from(requisitionV2Dto);
    usageReportDataProcessors.forEach(processor -> processor.get(siglusRequisitionDto));
    log.info("get all kit line items: {}", requisitionV2Dto.getId());
    List<KitUsageLineItem> items = kitUsageRepository.findByRequisitionId(requisitionV2Dto.getId());
    siglusRequisitionDto.setKitUsageLineItems(getKitUsageLineItemDtos(items));
    setUsageTemplateDto(requisitionV2Dto.getTemplate().getId(), siglusRequisitionDto);
    return siglusRequisitionDto;
  }

  public void deleteUsageReport(UUID requisitionId) {
    usageReportDataProcessors.forEach(processor -> processor.delete(requisitionId));
    log.info("find kit requisition line item: {}", requisitionId);
    List<KitUsageLineItem> items = kitUsageRepository.findByRequisitionId(requisitionId);
    if (!items.isEmpty()) {
      log.info("delete kit requisition line item: {}", items);
      kitUsageRepository.delete(items);
      kitUsageRepository.flush();
    }
  }

  public SiglusRequisitionDto saveUsageReportWithValidation(SiglusRequisitionDto requisition,
      RequisitionV2Dto updatedDto) {
    Set<ConstraintViolation<SiglusRequisitionDto>> constraintViolations =
        validatorFactory.getValidator().validate(requisition, RequisitionActionSequence.class);
    if (!constraintViolations.isEmpty()) {
      throw new ConstraintViolationException(constraintViolations);
    }
    return saveUsageReport(requisition, updatedDto);
  }

  public SiglusRequisitionDto saveUsageReport(SiglusRequisitionDto requisitionDto, RequisitionV2Dto updatedDto) {
    SiglusRequisitionDto siglusUpdatedDto = SiglusRequisitionDto.from(updatedDto);
    usageReportDataProcessors.forEach(processor -> processor.update(requisitionDto, siglusUpdatedDto));
    updateKitUsageLineItem(requisitionDto, siglusUpdatedDto);
    return siglusUpdatedDto;
  }

  public SiglusRequisitionDto initiateUsageReport(RequisitionV2Dto requisitionV2Dto) {
    List<UsageTemplateColumnSection> templateColumnSections =
        columnSectionRepository.findByRequisitionTemplateId(requisitionV2Dto.getTemplate().getId());
    SiglusRequisitionDto siglusRequisitionDto = SiglusRequisitionDto.from(requisitionV2Dto);
    if (templateColumnSections.isEmpty()) {
      return siglusRequisitionDto;
    }
    usageReportDataProcessors.forEach(processor -> processor.initiate(siglusRequisitionDto, templateColumnSections));
    updateKitUsage(requisitionV2Dto, templateColumnSections, siglusRequisitionDto);
    buildUsageTemplateDto(siglusRequisitionDto, templateColumnSections);
    return siglusRequisitionDto;
  }

  public SiglusRequisitionDto initiateUsageReportForClient(RequisitionV2Dto requisitionV2Dto) {
    List<UsageTemplateColumnSection> templateColumnSections =
        columnSectionRepository.findByRequisitionTemplateId(requisitionV2Dto.getTemplate().getId());
    SiglusRequisitionDto siglusRequisitionDto = SiglusRequisitionDto.from(requisitionV2Dto);
    if (templateColumnSections.isEmpty()) {
      return siglusRequisitionDto;
    }
    updateKitUsage(requisitionV2Dto, templateColumnSections, siglusRequisitionDto);
    buildUsageTemplateDto(siglusRequisitionDto, templateColumnSections);
    return siglusRequisitionDto;
  }

  public void setUsageTemplateDto(UUID templateId, SiglusRequisitionDto siglusRequisitionDto) {
    List<UsageTemplateColumnSection> templateColumnSections =
        columnSectionRepository.findByRequisitionTemplateId(templateId);
    buildUsageTemplateDto(siglusRequisitionDto, templateColumnSections);
  }

  public UsageTemplateColumnSection getColumnSection(List<UsageTemplateColumnSection> templateColumnSections,
      UsageCategory category, String sectionName) {
    return templateColumnSections.stream()
        .filter(templateColumnSection ->
            templateColumnSection.getCategory().equals(category)
                && templateColumnSection.getName().equals(sectionName))
        .findFirst().orElse(null);
  }

  public boolean isNotSupplyFacilityOrNotUsageReports(UUID programId, UUID facilityId) {
    return !Arrays.asList(TARV_PROGRAM_CODE, MTB_PROGRAM_CODE, RAPIDTEST_PROGRAM_CODE, MALARIA_PROGRAM_CODE)
        .contains(siglusProgramService.getProgram(programId).getCode())
        || !isSupplyFacilityType(facilityId);
  }

  public boolean isSupplyFacilityType(UUID facilityId) {
    String facilityTypeCode = facilityReferenceDataService.findOne(facilityId).getType().getCode();
    return Arrays.asList(DPM, AI, ODF).contains(facilityTypeCode);
  }

  private void updateKitUsageLineItem(SiglusRequisitionDto requisitionDto, SiglusRequisitionDto updatedDto) {
    List<KitUsageLineItemDto> kitUsageLineItemDtos = requisitionDto.getKitUsageLineItems();
    List<KitUsageLineItem> lineItems = KitUsageLineItem.from(kitUsageLineItemDtos, requisitionDto);
    log.info("save all kit line item: {}", lineItems);
    List<KitUsageLineItem> kitUsageLineUpdate = kitUsageRepository.save(lineItems);
    updatedDto.setKitUsageLineItems(getKitUsageLineItemDtos(kitUsageLineUpdate));
  }

  private void buildUsageTemplateDto(SiglusRequisitionDto requisitionDto,
      List<UsageTemplateColumnSection> columnSections) {
    SiglusUsageTemplateDto templateDto = new SiglusUsageTemplateDto();
    SiglusRequisitionTemplateService templateService = new SiglusRequisitionTemplateService();
    Map<UsageCategory, List<UsageTemplateSectionDto>> categoryListMap =
        templateService.getUsageTempateDto(columnSections);
    templateDto.setKitUsage(templateService.getCategoryDto(categoryListMap, UsageCategory.KITUSAGE));
    templateDto.setPatient(templateService.getCategoryDto(categoryListMap, UsageCategory.PATIENT));
    templateDto.setRegimen(templateService.getCategoryDto(categoryListMap, UsageCategory.REGIMEN));
    templateDto.setConsultationNumber(templateService.getCategoryDto(categoryListMap,
        UsageCategory.CONSULTATIONNUMBER));
    templateDto.setRapidTestConsumption(templateService.getCategoryDto(categoryListMap,
        UsageCategory.RAPIDTESTCONSUMPTION));
    templateDto.setUsageInformation(templateService.getCategoryDto(categoryListMap, UsageCategory.USAGEINFORMATION));
    templateDto.setAgeGroup(templateService.getCategoryDto(categoryListMap, UsageCategory.AGEGROUP));
    requisitionDto.setUsageTemplate(templateDto);
  }

  private void updateKitUsage(RequisitionV2Dto requisitionV2Dto,
      List<UsageTemplateColumnSection> templateColumnSections,
      SiglusRequisitionDto siglusRequisitionDto) {
    if (isEnableKit(requisitionV2Dto, templateColumnSections)) {
      log.info("get all kit products");
      Profiler profiler = new Profiler("getAllKits");
      profiler.setLogger(log);
      profiler.start("get kit products");
      List<Orderable> kitProducts = getKitProducts(requisitionV2Dto);
      profiler.start("get kit stock card range summary");
      List<StockCardRangeSummaryDto> stockCardRangeSummaryDtos = stockCardRangeSummaryDtos(templateColumnSections,
          requisitionV2Dto, kitProducts);
      profiler.start("get kit lines");
      List<KitUsageLineItem> kitUsageLineItems = getKitUsageLineItems(requisitionV2Dto,
          templateColumnSections, kitProducts, stockCardRangeSummaryDtos);
      log.info("save all kit line item: {}", kitUsageLineItems);
      profiler.start("save kit lines");
      List<KitUsageLineItem> kitUsageLineUpdate = kitUsageRepository.save(kitUsageLineItems);
      kitUsageRepository.flush();
      profiler.start("get kit lines from saved");
      List<KitUsageLineItemDto> kitDtos = getKitUsageLineItemDtos(kitUsageLineUpdate);
      siglusRequisitionDto.setKitUsageLineItems(kitDtos);
      profiler.stop().log();
    }
  }

  private List<Orderable> getKitProducts(RequisitionV2Dto requisitionV2Dto) {
    Profiler profiler = new Profiler("getKitProducts");
    profiler.setLogger(log);
    profiler.start("get period");
    ProcessingPeriodDto period = periodService.getPeriod(requisitionV2Dto.getProcessingPeriodId());
    profiler.start("get kits");
    List<Orderable> allKitProducts = orderableKitRepository.findAllKitProduct();

    if (period.isReportOnly()) {
      Set<UUID> additionalOrderableIds =
          programAdditionalOrderableRepository.findAllByProgramId(requisitionV2Dto.getProgramId())
              .stream().map(ProgramAdditionalOrderable::getAdditionalOrderableId)
              .collect(toSet());
      return allKitProducts
          .stream()
          .filter(kit -> additionalOrderableIds.contains(kit.getId()))
          .collect(Collectors.toList());
    }

    profiler.stop().log();
    return allKitProducts
        .stream()
        .filter(kit -> isInProgram(kit, requisitionV2Dto.getProgramId()))
        .collect(Collectors.toList());

  }

  private boolean isInProgram(Orderable orderable, UUID programId) {
    return orderable.getProgramOrderables()
        .stream()
        .anyMatch(p -> programId.equals(p.getProgram().getId()));
  }

  private boolean isEnableKit(RequisitionV2Dto v2Dto,
      List<UsageTemplateColumnSection> columnSections) {
    return v2Dto.getTemplate().getExtension().isEnableKitUsage()
        && getColumnSection(columnSections, UsageCategory.KITUSAGE, KIT_COLLECTION) != null;
  }

  private List<StockCardRangeSummaryDto> stockCardRangeSummaryDtos(
      List<UsageTemplateColumnSection> templateColumnSections,
      RequisitionV2Dto requisitionV2Dto, List<Orderable> allKitProducts) {
    List<StockCardRangeSummaryDto> summaryDtos = new ArrayList<>();
    if (isExistCalculateStockCard(templateColumnSections)) {
      log.info("get all program extension");
      Map<UUID, List<Orderable>> groupKitProducts = allKitProducts.stream()
          .collect(Collectors.groupingBy(kitProduct -> {
            OrderableDto kitProductDto = new OrderableDto();
            kitProduct.export(kitProductDto);
            return kitProductDto.getPrograms().stream().findFirst().get().getProgramId();
          }));
      for (Map.Entry<UUID, List<Orderable>> groupKit : groupKitProducts.entrySet()) {
        updateSupportProgramStockCardRange(requisitionV2Dto, summaryDtos, groupKit);
      }
    }
    return summaryDtos;
  }

  private boolean isExistCalculateStockCard(List<UsageTemplateColumnSection> templateColumnSections) {
    UsageTemplateColumnSection section = getColumnSection(templateColumnSections,
        UsageCategory.KITUSAGE, KIT_COLLECTION);
    if (section == null) {
      return false;
    }
    return section.getColumns()
        .stream()
        .anyMatch(column -> column.getIsDisplayed() && column.getSource().equals(CALCULATE_FROM_STOCK_CARD));
  }

  private void updateSupportProgramStockCardRange(RequisitionV2Dto requisitionV2Dto,
      List<StockCardRangeSummaryDto> summaryDtos, Entry<UUID, List<Orderable>> groupKit) {
    Set<VersionIdentityDto> kitProducts = groupKit.getValue()
        .stream()
        .map(orderable -> new VersionIdentityDto(orderable.getId(), orderable.getVersionNumber()))
        .collect(toSet());
    List<StockCardRangeSummaryDto> stockCardRangeSummaryDtos =
        stockCardRangeSummaryStockManagementService.search(groupKit.getKey(),
            requisitionV2Dto.getFacility().getId(),
            kitProducts, null,
            getActualDate(requisitionV2Dto.getExtraData(), ACTUAL_START_DATE),
            getActualDate(requisitionV2Dto.getExtraData(), ACTUAL_END_DATE));
    summaryDtos.addAll(stockCardRangeSummaryDtos);
  }

  private LocalDate getActualDate(Map<String, Object> extraData, String field) {
    if (extraData != null && extraData.get(field) != null) {
      DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
      return LocalDate.parse((String) extraData.get(field), dateTimeFormatter);
    }
    return null;
  }

  private List<KitUsageLineItem> getKitUsageLineItems(RequisitionV2Dto requisitionV2Dto,
      List<UsageTemplateColumnSection> templateColumnSections, List<Orderable> allKitProducts,
      List<StockCardRangeSummaryDto> stockCardRangeSummaryDtos) {
    UsageTemplateColumnSection collection = getColumnSection(templateColumnSections,
        UsageCategory.KITUSAGE, KIT_COLLECTION);
    UsageTemplateColumnSection service = getColumnSection(templateColumnSections, UsageCategory.KITUSAGE, KIT_SERVICE);
    if (collection == null || service == null) {
      throwError(ERROR_REPORTING_TEMPLATE_NOT_FOUND_WITH_NAME, UsageCategory.KITUSAGE.toString());
    }
    List<KitUsageLineItem> kitUsageLineItems = new ArrayList<>();
    for (UsageTemplateColumn templateService : service.getColumns()) {
      if (!Boolean.TRUE.equals(templateService.getIsDisplayed())) {
        continue;
      }
      for (UsageTemplateColumn templateCollection : collection.getColumns()) {
        if (!Boolean.TRUE.equals(templateCollection.getIsDisplayed())) {
          continue;
        }
        kitUsageLineItems.add(KitUsageLineItem.builder()
            .requisitionId(requisitionV2Dto.getId())
            .collection(templateCollection.getName())
            .service(templateService.getName())
            .value(getInitialValue(templateCollection, templateService, allKitProducts,
                stockCardRangeSummaryDtos))
            .build());
      }
    }
    return kitUsageLineItems;
  }

  private Integer getInitialValue(UsageTemplateColumn collection, UsageTemplateColumn service,
      List<Orderable> kitProducts, List<StockCardRangeSummaryDto> stockCardRangeSummaryDtos) {
    Set<UUID> kitProductIds = kitProducts.stream().map(Orderable::getId).collect(toSet());
    if (service.getName().equals(SERVICE_HF) && collection.getSource().equals(CALCULATE_FROM_STOCK_CARD)) {
      return getKitQuantity(collection, kitProductIds, stockCardRangeSummaryDtos);
    }
    return null;
  }

  private Integer getKitQuantity(UsageTemplateColumn collection,
      Set<UUID> kitProductIds,
      List<StockCardRangeSummaryDto> stockCardRangeSummaryDtos) {
    if (kitProductIds.isEmpty() || stockCardRangeSummaryDtos.isEmpty()) {
      return 0;
    }
    Integer valueByTag = 0;
    for (UUID kitProductId : kitProductIds) {
      StockCardRangeSummaryDto summaryDto = stockCardRangeSummaryDtos.stream()
          .filter(stockCardRangeSummaryDto ->
              stockCardRangeSummaryDto.getOrderable().getId().equals(kitProductId)).findFirst()
          .orElse(null);
      Integer value = summaryDto == null ? 0 : summaryDto.getTagAmount(collection.getTag());
      if (CONSUMED.equals(collection.getTag())) {
        value = Math.abs(value);
      }
      valueByTag += value;
    }
    return valueByTag;
  }

  private List<KitUsageLineItemDto> getKitUsageLineItemDtos(List<KitUsageLineItem> kitUsageLineUpdate) {
    List<KitUsageLineItemDto> kitDtos = new ArrayList<>();
    Map<String, List<KitUsageLineItem>> groupKitUsages = kitUsageLineUpdate.stream()
        .collect(Collectors.groupingBy(KitUsageLineItem::getCollection));
    for (Entry<String, List<KitUsageLineItem>> groupKitUsage : groupKitUsages.entrySet()) {
      KitUsageLineItemDto kitUsageLineItemDto = new KitUsageLineItemDto();
      kitUsageLineItemDto.setCollection(groupKitUsage.getKey());
      Map<String, KitUsageServiceLineItemDto> services = new HashMap<>();
      groupKitUsage.getValue().forEach(lineItem -> {
        KitUsageServiceLineItemDto dto = KitUsageServiceLineItemDto.builder()
            .id(lineItem.getId())
            .value(lineItem.getValue())
            .build();
        services.put(lineItem.getService(), dto);
      });
      kitUsageLineItemDto.setServices(services);
      kitDtos.add(kitUsageLineItemDto);
    }
    return kitDtos;
  }

  private void throwError(String messageKey, Object... params) {
    throw new ValidationMessageException(new Message(messageKey, params));
  }
}
