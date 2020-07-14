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
import static org.openlmis.stockmanagement.i18n.MessageKeys.ERROR_REPORTING_TEMPLATE_NOT_FOUND_WITH_NAME;
import static org.siglus.common.constant.FieldConstants.ACTUAL_END_DATE;
import static org.siglus.common.constant.FieldConstants.ACTUAL_START_DATE;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.siglus.common.domain.referencedata.Orderable;
import org.siglus.common.dto.referencedata.OrderableDto;
import org.openlmis.requisition.dto.ObjectReferenceDto;
import org.openlmis.requisition.dto.RequisitionV2Dto;
import org.openlmis.requisition.dto.VersionIdentityDto;
import org.openlmis.requisition.dto.stockmanagement.StockCardRangeSummaryDto;
import org.openlmis.requisition.service.stockmanagement.StockCardRangeSummaryStockManagementService;
import org.openlmis.stockmanagement.exception.ValidationMessageException;
import org.openlmis.stockmanagement.util.Message;
import org.siglus.common.domain.ProgramExtension;
import org.siglus.common.repository.OrderableKitRepository;
import org.siglus.common.repository.ProgramExtensionRepository;
import org.siglus.siglusapi.domain.KitUsageLineItem;
import org.siglus.siglusapi.domain.UsageCategory;
import org.siglus.siglusapi.domain.UsageTemplateColumn;
import org.siglus.siglusapi.domain.UsageTemplateColumnSection;
import org.siglus.siglusapi.dto.KitUsageLineItemDto;
import org.siglus.siglusapi.dto.KitUsageServiceLineItemDto;
import org.siglus.siglusapi.dto.SiglusRequisitionDto;
import org.siglus.siglusapi.dto.SiglusUsageTemplateDto;
import org.siglus.siglusapi.dto.UsageTemplateSectionDto;
import org.siglus.siglusapi.repository.KitUsageLineItemRepository;
import org.siglus.siglusapi.repository.UsageTemplateColumnSectionRepository;
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
  ProgramExtensionRepository programExtensionRepository;

  @Autowired
  KitUsageLineItemRepository kitUsageRepository;

  @Autowired
  private List<UsageReportDataProcessor> usageReportDataProcessors;

  public SiglusRequisitionDto searchUsageReport(RequisitionV2Dto requisitionV2Dto) {
    SiglusRequisitionDto siglusRequisitionDto = SiglusRequisitionDto.from(requisitionV2Dto);
    usageReportDataProcessors.forEach(processor -> processor.get(siglusRequisitionDto));
    log.info("get all kit line items: {}", requisitionV2Dto.getId());
    List<KitUsageLineItem> items = kitUsageRepository.findByRequisitionId(requisitionV2Dto.getId());
    siglusRequisitionDto.setKitUsageLineItems(getKitUsageLineItemDtos(items));
    setUsageTemplateDto(requisitionV2Dto, siglusRequisitionDto);
    return siglusRequisitionDto;
  }

  public void deleteUsageReport(UUID requisitionId) {
    usageReportDataProcessors.forEach(processor -> processor.delete(requisitionId));
    log.info("find kit requisition line item: {}", requisitionId);
    List<KitUsageLineItem> items = kitUsageRepository.findByRequisitionId(requisitionId);
    if (!items.isEmpty()) {
      log.info("delete kit requisition line item: {}", items);
      kitUsageRepository.delete(items);
    }
  }

  public SiglusRequisitionDto saveUsageReport(SiglusRequisitionDto requisitionDto,
      RequisitionV2Dto updatedDto) {
    SiglusRequisitionDto siglusUpdatedDto = SiglusRequisitionDto.from(updatedDto);
    usageReportDataProcessors.forEach(processor -> processor.update(requisitionDto,
        siglusUpdatedDto));
    updateKitUsageLineItem(requisitionDto, siglusUpdatedDto);
    return siglusUpdatedDto;
  }

  private void updateKitUsageLineItem(SiglusRequisitionDto requisitionDto,
      SiglusRequisitionDto updatedDto) {
    List<KitUsageLineItemDto> kitUsageLineItemDtos = requisitionDto.getKitUsageLineItems();
    List<KitUsageLineItem> lineItems = KitUsageLineItem.from(kitUsageLineItemDtos, requisitionDto);
    log.info("save all kit line item: {}", lineItems);
    List<KitUsageLineItem> kitUsageLineUpdate = kitUsageRepository.save(lineItems);
    updatedDto.setKitUsageLineItems(getKitUsageLineItemDtos(kitUsageLineUpdate));
  }


  public SiglusRequisitionDto initiateUsageReport(RequisitionV2Dto requisitionV2Dto) {
    List<UsageTemplateColumnSection> templateColumnSections =
        columnSectionRepository.findByRequisitionTemplateId(requisitionV2Dto.getTemplate().getId());
    SiglusRequisitionDto siglusRequisitionDto = SiglusRequisitionDto.from(requisitionV2Dto);
    if (templateColumnSections.isEmpty()) {
      return siglusRequisitionDto;
    }
    usageReportDataProcessors
        .forEach(processor -> processor.initiate(siglusRequisitionDto, templateColumnSections));
    updateKitUsage(requisitionV2Dto, templateColumnSections, siglusRequisitionDto);
    setUsageTemplateDto(siglusRequisitionDto, templateColumnSections);
    return siglusRequisitionDto;
  }

  public void setUsageTemplateDto(RequisitionV2Dto requisitionV2Dto,
      SiglusRequisitionDto siglusRequisitionDto) {
    List<UsageTemplateColumnSection> templateColumnSections =
        columnSectionRepository.findByRequisitionTemplateId(requisitionV2Dto.getTemplate().getId());
    setUsageTemplateDto(siglusRequisitionDto, templateColumnSections);
  }

  private void setUsageTemplateDto(SiglusRequisitionDto requisitionDto,
      List<UsageTemplateColumnSection> columnSections) {
    SiglusUsageTemplateDto templateDto = new SiglusUsageTemplateDto();
    SiglusRequisitionTemplateService templateService = new SiglusRequisitionTemplateService();
    Map<UsageCategory, List<UsageTemplateSectionDto>> categoryListMap =
        templateService.getUsageTempateDto(columnSections);
    templateDto
        .setKitUsage(templateService.getCategoryDto(categoryListMap, UsageCategory.KITUSAGE));
    templateDto.setPatient(templateService.getCategoryDto(categoryListMap, UsageCategory.PATIENT));
    templateDto.setRegimen(templateService.getCategoryDto(categoryListMap, UsageCategory.REGIMEN));
    templateDto
        .setConsultationNumber(
            templateService.getCategoryDto(categoryListMap, UsageCategory.CONSULTATIONNUMBER));
    templateDto.setRapidTestConsumption(
        templateService.getCategoryDto(categoryListMap, UsageCategory.RAPIDTESTCONSUMPTION));
    templateDto
        .setUsageInformation(
            templateService.getCategoryDto(categoryListMap, UsageCategory.USAGEINFORMATION));
    requisitionDto.setUsageTemplate(templateDto);
  }

  private void updateKitUsage(RequisitionV2Dto requisitionV2Dto,
      List<UsageTemplateColumnSection> templateColumnSections,
      SiglusRequisitionDto siglusRequisitionDto) {
    if (isEnableKit(requisitionV2Dto, templateColumnSections)) {
      log.info("get all kit products");
      List<Orderable> allKitProducts = orderableKitRepository.findAllKitProduct();
      List<StockCardRangeSummaryDto> stockCardRangeSummaryDtos =
          stockCardRangeSummaryDtos(templateColumnSections,
              requisitionV2Dto, allKitProducts);
      List<KitUsageLineItem> kitUsageLineItems = getKitUsageLineItems(requisitionV2Dto,
          templateColumnSections, allKitProducts,
          stockCardRangeSummaryDtos);
      log.info("save all kit line item: {}", kitUsageLineItems);
      List<KitUsageLineItem> kitUsageLineUpdate = kitUsageRepository.save(kitUsageLineItems);
      List<KitUsageLineItemDto> kitDtos = getKitUsageLineItemDtos(kitUsageLineUpdate);
      siglusRequisitionDto.setKitUsageLineItems(kitDtos);
    }
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
      List<UUID> supportPrograms = reportSupportedProgram(requisitionV2Dto);
      log.info("get all program extension");
      List<ProgramExtension> programExtensions = programExtensionRepository.findAll();
      Map<UUID, List<Orderable>> groupKitProducts = allKitProducts.stream()
          .collect(Collectors.groupingBy(kitProduct -> {
            OrderableDto kitProductDto = new OrderableDto();
            kitProduct.export(kitProductDto);
            final UUID programId = kitProductDto.getPrograms().stream().findFirst().get()
                .getProgramId();
            return getVirtualProgram(programExtensions, programId);
          }));
      for (Map.Entry<UUID, List<Orderable>> gropuKit : groupKitProducts.entrySet()) {
        updateSupportProgramStockCardRange(requisitionV2Dto, supportPrograms, summaryDtos,
            gropuKit);
      }
    }
    return summaryDtos;
  }

  private boolean isExistCalculateStockCard(
      List<UsageTemplateColumnSection> templateColumnSections) {
    UsageTemplateColumnSection section = getColumnSection(templateColumnSections,
        UsageCategory.KITUSAGE, KIT_COLLECTION);
    if (section == null) {
      return false;
    }
    return section.getColumns()
        .stream()
        .anyMatch(column -> column.getIsDisplayed() && column.getSource()
            .equals(CALCULATE_FROM_STOCK_CARD));
  }

  public UsageTemplateColumnSection getColumnSection(
      List<UsageTemplateColumnSection> templateColumnSections,
      UsageCategory category, String sectionName) {
    return templateColumnSections.stream()
        .filter(templateColumnSection ->
            templateColumnSection.getCategory().equals(category)
                && templateColumnSection.getName().equals(sectionName))
        .findFirst().orElse(null);
  }

  private void updateSupportProgramStockCardRange(RequisitionV2Dto requisitionV2Dto,
      List<UUID> supportPrograms, List<StockCardRangeSummaryDto> summaryDtos,
      Entry<UUID, List<Orderable>> gropuKit) {
    if (supportPrograms.contains(gropuKit.getKey())) {
      Set<VersionIdentityDto> kitProducts = gropuKit.getValue()
          .stream()
          .map(orderable -> new VersionIdentityDto(orderable.getId(),
              orderable.getVersionNumber()))
          .collect(toSet());
      List<StockCardRangeSummaryDto> stockCardRangeSummaryDtos =
          stockCardRangeSummaryStockManagementService.search(gropuKit.getKey(),
              requisitionV2Dto.getFacility().getId(),
              kitProducts, null,
              getActualDate(requisitionV2Dto.getExtraData(), ACTUAL_START_DATE),
              getActualDate(requisitionV2Dto.getExtraData(), ACTUAL_END_DATE));
      summaryDtos.addAll(stockCardRangeSummaryDtos);
    }
  }

  private List<UUID> reportSupportedProgram(RequisitionV2Dto requisitionV2Dto) {
    List<UUID> reportSupportProgram = new ArrayList<>();
    reportSupportProgram.add(requisitionV2Dto.getProgramId());
    reportSupportProgram.addAll(requisitionV2Dto.getTemplate()
        .getAssociatePrograms()
        .stream()
        .map(ObjectReferenceDto::getId)
        .collect(Collectors.toList()));
    return reportSupportProgram;
  }

  private LocalDate getActualDate(Map<String, Object> extraData,
      String field) {
    if (extraData != null && extraData.get(field) != null) {
      DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
      return LocalDate.parse((String) extraData.get(field), dateTimeFormatter);
    }
    return null;
  }

  private UUID getVirtualProgram(List<ProgramExtension> programExtensions, UUID program) {
    ProgramExtension extension = programExtensions.stream()
        .filter(programExtension -> programExtension.getProgramId().equals(program)).findFirst()
        .orElse(null);
    if (extension == null) {
      return program;
    }
    return extension.getIsVirtual().equals(Boolean.TRUE) ? program : extension.getParentId();
  }

  private List<KitUsageLineItem> getKitUsageLineItems(RequisitionV2Dto requisitionV2Dto,
      List<UsageTemplateColumnSection> templateColumnSections, List<Orderable> allKitProducts,
      List<StockCardRangeSummaryDto> stockCardRangeSummaryDtos) {
    UsageTemplateColumnSection collection = getColumnSection(templateColumnSections,
        UsageCategory.KITUSAGE, KIT_COLLECTION);
    UsageTemplateColumnSection service = getColumnSection(templateColumnSections,
        UsageCategory.KITUSAGE, KIT_SERVICE);
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

  private Integer getInitialValue(UsageTemplateColumn collection,
      UsageTemplateColumn service,
      List<Orderable> kitProducts,
      List<StockCardRangeSummaryDto> stockCardRangeSummaryDtos) {
    Set<UUID> kitProductIds = kitProducts.stream().map(Orderable::getId).collect(toSet());
    if (service.getName().equals(SERVICE_HF) && collection.getSource()
        .equals(CALCULATE_FROM_STOCK_CARD)) {
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
      valueByTag += value;
    }
    return valueByTag;
  }

  private List<KitUsageLineItemDto> getKitUsageLineItemDtos(
      List<KitUsageLineItem> kitUsageLineUpdate) {
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
