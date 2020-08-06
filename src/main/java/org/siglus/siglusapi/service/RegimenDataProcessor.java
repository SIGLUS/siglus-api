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

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.openlmis.requisition.service.RequisitionService;
import org.siglus.siglusapi.domain.Regimen;
import org.siglus.siglusapi.domain.RegimenLineItem;
import org.siglus.siglusapi.domain.RegimenSummaryLineItem;
import org.siglus.siglusapi.domain.UsageCategory;
import org.siglus.siglusapi.domain.UsageTemplateColumn;
import org.siglus.siglusapi.domain.UsageTemplateColumnSection;
import org.siglus.siglusapi.dto.RegimenDispatchLineDto;
import org.siglus.siglusapi.dto.RegimenDto;
import org.siglus.siglusapi.dto.RegimenLineDto;
import org.siglus.siglusapi.dto.RegimenSummaryLineDto;
import org.siglus.siglusapi.dto.SiglusRequisitionDto;
import org.siglus.siglusapi.repository.RegimenDispatchLineRepository;
import org.siglus.siglusapi.repository.RegimenLineItemRepository;
import org.siglus.siglusapi.repository.RegimenRepository;
import org.siglus.siglusapi.repository.RegimenSummaryLineItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class RegimenDataProcessor implements UsageReportDataProcessor {

  private static final String REGIMEN = "regimen";

  private static final String SUMMARY = "summary";

  private static final String REFERENCE_DATA = "REFERENCE_DATA";

  @Autowired
  private SiglusUsageReportService siglusUsageReportService;

  @Autowired
  private RegimenRepository regimenRepository;

  @Autowired
  private RegimenLineItemRepository regimenLineItemRepository;

  @Autowired
  private RegimenDispatchLineRepository regimenDispatchLineRepository;

  @Autowired
  private RegimenSummaryLineItemRepository regimenSummaryLineItemRepository;

  @Autowired
  private RequisitionService requisitionService;

  @Override
  public void doInitiate(SiglusRequisitionDto siglusRequisitionDto,
      List<UsageTemplateColumnSection> templateColumnSections) {

    List<RegimenDto> defaultRegimenDtos =
        regimenRepository.findAllByProgramIdInAndActiveTrueAndIsCustomIsFalse(
        getProgramIds(siglusRequisitionDto))
        .stream()
        .map(RegimenDto::from)
        .collect(Collectors.toList());
    Set<RegimenDispatchLineDto> regimenDispatchLineDtos =
        getValidRegimenDispatchLines(siglusRequisitionDto);

    List<RegimenLineItem> regimenLineItems = createRegimenLineItems(siglusRequisitionDto,
        templateColumnSections, defaultRegimenDtos);
    List<RegimenSummaryLineItem> regimenSummaryLineItems = createRegimenSummaryLineItems(
        siglusRequisitionDto, templateColumnSections, regimenDispatchLineDtos);

    log.info("save regimen line items by requisition id: {}",
        siglusRequisitionDto.getId());

    List<RegimenLineItem> saved = regimenLineItemRepository.save(regimenLineItems);
    List<RegimenLineDto> lineDtos = RegimenLineDto.from(saved, getRegimenDtoMap());

    List<RegimenSummaryLineItem> savedSummaryLineItems =
        regimenSummaryLineItemRepository.save(regimenSummaryLineItems);
    List<RegimenSummaryLineDto> summaryLineDtos =
        RegimenSummaryLineDto.from(savedSummaryLineItems, getRegimenDispatchLineDtoMap());


    siglusRequisitionDto.setRegimenLineItems(lineDtos);
    siglusRequisitionDto.setRegimenDispatchLineItems(summaryLineDtos);
    setCustomRegimen(siglusRequisitionDto);
  }

  @Override
  public void get(SiglusRequisitionDto siglusRequisitionDto) {
    List<RegimenLineItem> regimenLineItems =
        regimenLineItemRepository.findByRequisitionId(siglusRequisitionDto.getId());
    List<RegimenLineDto> lineDtos = RegimenLineDto.from(regimenLineItems, getRegimenDtoMap());

    List<RegimenSummaryLineItem> summaryLineItems =
        regimenSummaryLineItemRepository.findByRequisitionId(siglusRequisitionDto.getId());
    List<RegimenSummaryLineDto> summaryLineDtos =
        RegimenSummaryLineDto.from(summaryLineItems, getRegimenDispatchLineDtoMap());

    siglusRequisitionDto.setRegimenLineItems(lineDtos);
    siglusRequisitionDto.setRegimenDispatchLineItems(summaryLineDtos);
    setCustomRegimen(siglusRequisitionDto);
  }

  @Override
  public void update(SiglusRequisitionDto siglusRequisitionDto,
      SiglusRequisitionDto siglusRequisitionUpdatedDto) {
    List<RegimenLineDto> lineDtos = siglusRequisitionDto.getRegimenLineItems();
    siglusRequisitionUpdatedDto.setRegimenLineItems(lineDtos);

    List<RegimenSummaryLineDto> summaryLineDtos =
        siglusRequisitionDto.getRegimenDispatchLineItems();
    siglusRequisitionUpdatedDto.setRegimenDispatchLineItems(summaryLineDtos);

    List<RegimenLineItem> regimenLineItemsFromRequest =
        RegimenLineItem.from(lineDtos, siglusRequisitionDto.getId());
    List<RegimenLineItem> regimenLineItemsFromDb =
        regimenLineItemRepository.findByRequisitionId(siglusRequisitionDto.getId());

    List<RegimenLineItem> regimenLineItemsToRemove =
        getLineItemToRemove(regimenLineItemsFromRequest, regimenLineItemsFromDb);
    regimenLineItemRepository.delete(regimenLineItemsToRemove);

    List<RegimenSummaryLineItem> summaryLineItems =
        RegimenSummaryLineItem.from(summaryLineDtos, siglusRequisitionDto.getId());
    log.info("update regimen line items by requisition id: {}",
        siglusRequisitionDto.getId());
    regimenLineItemRepository.save(regimenLineItemsFromRequest);
    regimenSummaryLineItemRepository.save(summaryLineItems);
  }

  @Override
  public void delete(UUID requisitionId) {
    List<RegimenLineItem> regimenLineItems =
        regimenLineItemRepository.findByRequisitionId(requisitionId);
    List<RegimenSummaryLineItem> summaryLineItems =
        regimenSummaryLineItemRepository.findByRequisitionId(requisitionId);
    log.info("delete regimen line items by requisition id: {}", requisitionId);
    regimenLineItemRepository.delete(regimenLineItems);
    regimenSummaryLineItemRepository.delete(summaryLineItems);
  }

  @Override
  public boolean isDisabled(SiglusRequisitionDto siglusRequisitionDto) {
    return !siglusRequisitionDto.getTemplate().getExtension().isEnableRegimen();
  }

  private List<RegimenLineItem> getLineItemToRemove(
      List<RegimenLineItem> regimenLineItemsFromRequest,
      List<RegimenLineItem> regimenLineItemsFromDb
  ) {
    Set<UUID> ids = regimenLineItemsFromRequest
        .stream()
        .map(RegimenLineItem::getId)
        .filter(id -> id != null)
        .collect(Collectors.toSet());

    return regimenLineItemsFromDb
        .stream()
        .filter(lineItem -> !ids.contains(lineItem.getId()))
        .collect(Collectors.toList());
  }

  private Set<RegimenDispatchLineDto> getValidRegimenDispatchLines(
      SiglusRequisitionDto requisitionDto) {
    return regimenRepository
        .findAllByProgramIdInAndActiveTrue(getProgramIds(requisitionDto))
        .stream()
        .map(Regimen::getRegimenDispatchLine)
        .map(RegimenDispatchLineDto::from)
        .collect(Collectors.toSet());
  }

  private List<RegimenLineItem> createRegimenLineItems(
      SiglusRequisitionDto siglusRequisitionDto,
      List<UsageTemplateColumnSection> templateColumnSections,
      List<RegimenDto> defaultRegimenDtos
  ) {

    UsageTemplateColumnSection regimen = siglusUsageReportService
        .getColumnSection(templateColumnSections, UsageCategory.REGIMEN, REGIMEN);


    List<UsageTemplateColumn> templateColumns = getValidColumn(regimen);

    return defaultRegimenDtos.stream().flatMap(
        regimenDto -> templateColumns.stream().map(
            templateColumn -> RegimenLineItem.builder()
              .requisitionId(siglusRequisitionDto.getId())
              .regimenId(regimenDto.getId())
              .column(templateColumn.getName())
              .build()))
        .collect(Collectors.toList());
  }

  private List<UsageTemplateColumn> getValidColumn(UsageTemplateColumnSection section) {
    return section.getColumns()
        .stream()
        .filter(column -> Boolean.TRUE.equals(column.getIsDisplayed()))
        .filter(column -> !REFERENCE_DATA.equals(column.getSource()))
        .collect(Collectors.toList());
  }

  private List<RegimenSummaryLineItem> createRegimenSummaryLineItems(
      SiglusRequisitionDto siglusRequisitionDto,
      List<UsageTemplateColumnSection> templateColumnSections,
      Set<RegimenDispatchLineDto> regimenDispatchLineDtos
  ) {
    UsageTemplateColumnSection regimenDispatchLine = siglusUsageReportService
        .getColumnSection(templateColumnSections, UsageCategory.REGIMEN, SUMMARY);

    List<UsageTemplateColumn> templateColumns = getValidColumn(regimenDispatchLine);

    return regimenDispatchLineDtos.stream().flatMap(
        regimenDispatchLineDto -> templateColumns.stream().map(
            templateColumn -> RegimenSummaryLineItem.builder()
                .requisitionId(siglusRequisitionDto.getId())
                .regimenDispatchLineId(regimenDispatchLineDto.getId())
                .column(templateColumn.getName())
                .build()
        )
    ).collect(Collectors.toList());

  }

  public Map<UUID, RegimenDto> getRegimenDtoMap() {
    return regimenRepository.findAll()
        .stream()
        .map(RegimenDto::from)
        .collect(Collectors.toMap(RegimenDto::getId, regimenDto -> regimenDto));
  }

  public Map<UUID, RegimenDispatchLineDto> getRegimenDispatchLineDtoMap() {
    return regimenDispatchLineRepository.findAll()
        .stream()
        .map(RegimenDispatchLineDto::from)
        .collect(Collectors.toMap(RegimenDispatchLineDto::getId, dto -> dto));
  }

  public void setCustomRegimen(SiglusRequisitionDto siglusRequisitionDto) {
    List<RegimenDto> customRegimenDtos = regimenRepository
        .findAllByProgramIdInAndActiveTrueAndIsCustomIsTrue(
            getProgramIds(siglusRequisitionDto))
        .stream()
        .map(RegimenDto::from)
        .collect(Collectors.toList());
    siglusRequisitionDto.setCustomRegimens(customRegimenDtos);
  }

  private Set<UUID> getProgramIds(SiglusRequisitionDto siglusRequisitionDto) {
    Set<UUID> ids = requisitionService
        .getAssociateProgram(siglusRequisitionDto.getTemplate().getId());
    ids.add(siglusRequisitionDto.getProgramId());
    return ids;
  }

}
