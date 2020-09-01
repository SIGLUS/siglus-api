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
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.domain.requisition.RequisitionLineItem;
import org.openlmis.requisition.dto.ProcessingPeriodDto;
import org.openlmis.requisition.dto.ProgramDto;
import org.openlmis.requisition.dto.RequisitionV2Dto;
import org.openlmis.requisition.service.referencedata.ProgramReferenceDataService;
import org.siglus.common.domain.RequisitionTemplateExtension;
import org.siglus.common.domain.referencedata.Facility;
import org.siglus.common.domain.referencedata.SupervisoryNode;
import org.siglus.common.dto.referencedata.FacilityDto;
import org.siglus.common.dto.referencedata.OrderableDto;
import org.siglus.common.repository.RequisitionTemplateExtensionRepository;
import org.siglus.common.service.client.SiglusFacilityReferenceDataService;
import org.siglus.common.util.SiglusDateHelper;
import org.siglus.common.util.referencedata.Pagination;
import org.siglus.siglusapi.domain.RequisitionLineItemExtension;
import org.siglus.siglusapi.dto.FcRequisitionDto;
import org.siglus.siglusapi.dto.FcRequisitionLineItemDto;
import org.siglus.siglusapi.dto.RealProgramDto;
import org.siglus.siglusapi.dto.RegimenLineDto;
import org.siglus.siglusapi.dto.SiglusRequisitionDto;
import org.siglus.siglusapi.dto.UsageTemplateColumnDto;
import org.siglus.siglusapi.dto.UsageTemplateSectionDto;
import org.siglus.siglusapi.repository.ProgramOrderablesExtensionRepository;
import org.siglus.siglusapi.repository.SiglusRequisitionLineItemExtensionRepository;
import org.siglus.siglusapi.repository.SiglusRequisitionRepository;
import org.siglus.siglusapi.repository.SupervisoryNodeRepository;
import org.siglus.siglusapi.service.client.SiglusOrderableReferenceDataService;
import org.siglus.siglusapi.service.client.SiglusProcessingPeriodReferenceDataService;
import org.siglus.siglusapi.service.client.SiglusRequisitionRequisitionService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class SiglusFcIntegrationService {

  @Autowired
  private SupervisoryNodeRepository supervisoryNodeRepository;

  @Autowired
  private SiglusRequisitionRepository siglusRequisitionRepository;

  @Autowired
  private SiglusRequisitionExtensionService siglusRequisitionExtensionService;

  @Autowired
  private SiglusFacilityReferenceDataService facilityReferenceDataService;

  @Autowired
  private ProgramReferenceDataService programReferenceDataService;

  @Autowired
  private SiglusProcessingPeriodReferenceDataService processingPeriodReferenceDataService;

  @Autowired
  private SiglusOrderableReferenceDataService orderableReferenceDataService;

  @Autowired
  private ProgramOrderablesExtensionRepository programOrderablesExtensionRepository;

  @Autowired
  private SiglusRequisitionLineItemExtensionRepository lineItemExtensionRepository;

  @Autowired
  private RequisitionTemplateExtensionRepository requisitionTemplateExtensionRepository;

  @Autowired
  private SiglusUsageReportService siglusUsageReportService;

  @Autowired
  private SiglusRequisitionRequisitionService siglusRequisitionRequisitionService;

  @Autowired
  private SiglusDateHelper dateHelper;

  @Value("${dpm.facilityTypeId}")
  private UUID dpmFacilityTypeId;

  @Value("${fc.facilityTypeId}")
  private UUID fcFacilityTypeId;

  public Page<FcRequisitionDto> searchRequisitions(String date, Pageable pageable) {
    Set<UUID> dpmSupervisoryNodeIds = supervisoryNodeRepository
        .findAllByFacilityTypeId(dpmFacilityTypeId).stream().map(SupervisoryNode::getId)
        .collect(toSet());
    Set<UUID> fcSupervisoryNodeIds = supervisoryNodeRepository
        .findAllByFacilityTypeId(fcFacilityTypeId).stream().map(SupervisoryNode::getId)
        .collect(toSet());
    Page<Requisition> requisitions;
    String today = dateHelper.getTodayDateStr();
    if (fcSupervisoryNodeIds.isEmpty()) {
      requisitions = siglusRequisitionRepository.searchForFc(date, today, dpmSupervisoryNodeIds,
          pageable);
    } else {
      requisitions = siglusRequisitionRepository.searchForFc(date, today, dpmSupervisoryNodeIds,
          fcSupervisoryNodeIds, pageable);
    }
    List<FcRequisitionDto> fcRequisitionDtos = newArrayList();
    requisitions.getContent().forEach(requisition -> fcRequisitionDtos.add(buildDto(requisition,
        fcSupervisoryNodeIds)));
    return Pagination.getPage(fcRequisitionDtos, pageable, requisitions.getTotalElements());
  }

  private FcRequisitionDto buildDto(Requisition requisition, Set<UUID> fcSupervisoryNodeIds) {
    FcRequisitionDto fcRequisitionDto = new FcRequisitionDto();
    BeanUtils.copyProperties(requisition, fcRequisitionDto);
    fcRequisitionDto.setRequisitionNumber(
        siglusRequisitionExtensionService.formatRequisitionNumber(requisition.getId()));
    setFacilityInfo(requisition, fcSupervisoryNodeIds, fcRequisitionDto);
    setProgramInfo(requisition.getProgramId(), fcRequisitionDto);
    setPeriodInfo(requisition.getProcessingPeriodId(), fcRequisitionDto);
    setProductInfo(requisition.getNonSkippedRequisitionLineItems(), fcRequisitionDto);
    setRegimenInfo(requisition, fcRequisitionDto);
    return fcRequisitionDto;
  }

  private void setRegimenInfo(Requisition requisition, FcRequisitionDto fcRequisitionDto) {
    RequisitionTemplateExtension templateExtension = requisitionTemplateExtensionRepository
        .findByRequisitionTemplateId(requisition.getTemplate().getId());
    if (Boolean.FALSE.equals(templateExtension.getEnableRegimen())) {
      return;
    }
    RequisitionV2Dto requisitionDto = siglusRequisitionRequisitionService
        .searchRequisition(requisition.getId());
    SiglusRequisitionDto siglusRequisitionDto = siglusUsageReportService
        .searchUsageReport(requisitionDto);
    List<RegimenLineDto> regimenLineItems = siglusRequisitionDto.getRegimenLineItems();
    Map<String, List<UsageTemplateColumnDto>> usageTemplateMap = siglusRequisitionDto
        .getUsageTemplate()
        .getRegimen()
        .stream()
        .collect(Collectors.toMap(UsageTemplateSectionDto::getName,
            UsageTemplateSectionDto::getColumns));
    Map<String, String> regimenLabelMap = usageTemplateMap.get("regimen").stream().collect(
        Collectors.toMap(UsageTemplateColumnDto::getName, UsageTemplateColumnDto::getLabel));
    List<Map<String, Object>> regimens = newArrayList();
    regimenLineItems.forEach(regimenLineItem -> {
      Map<String, Object> regimenMap = newHashMap();
      regimens.add(regimenMap);
      regimenMap.put("code", regimenLineItem.getRegimen().getCode());
      regimenMap.put("name", regimenLineItem.getRegimen().getFullProductName());
      regimenLineItem.getColumns().forEach((key, value) -> regimenMap.put(regimenLabelMap.get(key),
          value.getValue()));
    });
    fcRequisitionDto.setRegimens(regimens);
  }

  private void setProductInfo(List<RequisitionLineItem> lineItems,
      FcRequisitionDto fcRequisitionDto) {
    Set<UUID> lineItemIds = lineItems.stream().map(RequisitionLineItem::getId)
        .collect(Collectors.toSet());
    if (CollectionUtils.isNotEmpty(lineItemIds)) {
      List<FcRequisitionLineItemDto> products = newArrayList();
      Map<UUID, Integer> authorizedQuantityMap = lineItemExtensionRepository
          .findLineItems(lineItemIds).stream().collect(toMap(
              RequisitionLineItemExtension::getRequisitionLineItemId, lineItemExtension ->
                  Optional.ofNullable(lineItemExtension.getAuthorizedQuantity()).orElse(0)));
      lineItems.forEach(lineItem -> products.add(buildLineItemDto(lineItem,
          authorizedQuantityMap)));
      fcRequisitionDto.setProducts(products);
    }
  }

  private void setPeriodInfo(UUID processingPeriodId, FcRequisitionDto fcRequisitionDto) {
    ProcessingPeriodDto processingPeriodDto = processingPeriodReferenceDataService
        .findOne(processingPeriodId);
    fcRequisitionDto.setPeriodStartDate(processingPeriodDto.getStartDate());
    fcRequisitionDto.setPeriodEndDate(processingPeriodDto.getEndDate());
  }

  private void setProgramInfo(UUID programId, FcRequisitionDto fcRequisitionDto) {
    ProgramDto programDto = programReferenceDataService.findOne(programId);
    fcRequisitionDto.setProgramCode(programDto.getCode());
    fcRequisitionDto.setProgramName(programDto.getName());
  }

  private void setFacilityInfo(Requisition requisition, Set<UUID> fcSupervisoryNodeIds,
      FcRequisitionDto fcRequisitionDto) {
    FacilityDto requestingFacility = facilityReferenceDataService
        .findOne(requisition.getFacilityId());
    fcRequisitionDto.setRequestingFacilityCode(requestingFacility.getCode());
    fcRequisitionDto.setRequestingFacilityName(requestingFacility.getName());
    fcRequisitionDto.setRequestingFacilityDescription(requestingFacility.getDescription());
    if (fcSupervisoryNodeIds.contains(requisition.getSupervisoryNodeId())) {
      fcRequisitionDto.setFacilityCode(requestingFacility.getCode());
      fcRequisitionDto.setFacilityName(requestingFacility.getName());
      fcRequisitionDto.setFacilityDescription(requestingFacility.getDescription());
    } else {
      Facility supervisoryNodeFacility = requisition.getSupervisoryNode().getFacility();
      fcRequisitionDto.setFacilityCode(supervisoryNodeFacility.getCode());
      fcRequisitionDto.setFacilityName(supervisoryNodeFacility.getName());
      fcRequisitionDto.setFacilityDescription(supervisoryNodeFacility.getDescription());
    }
  }

  private FcRequisitionLineItemDto buildLineItemDto(RequisitionLineItem lineItem,
      Map<UUID, Integer> authorizedQuantityMap) {
    FcRequisitionLineItemDto fcLineItem = new FcRequisitionLineItemDto();
    BeanUtils.copyProperties(lineItem, fcLineItem);
    OrderableDto orderable = orderableReferenceDataService.findOne(lineItem.getOrderable().getId());
    fcLineItem.setProductCode(orderable.getProductCode());
    fcLineItem.setProductName(orderable.getFullProductName());
    fcLineItem.setProductDescription(orderable.getDescription());
    List<RealProgramDto> realPrograms = newArrayList();
    programOrderablesExtensionRepository.findAllByOrderableId(lineItem.getOrderable().getId())
        .forEach(orderablesExtension ->
            realPrograms.add(new RealProgramDto(orderablesExtension.getRealProgramCode(),
                orderablesExtension.getRealProgramName())));
    fcLineItem.setRealPrograms(realPrograms);
    fcLineItem.setAuthorizedQuantity(authorizedQuantityMap.get(lineItem.getId()));
    return fcLineItem;
  }
}
