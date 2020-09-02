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

package org.siglus.siglusapi.service.fc;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.domain.requisition.RequisitionLineItem;
import org.openlmis.requisition.dto.BaseRequisitionLineItemDto;
import org.openlmis.requisition.dto.ProcessingPeriodDto;
import org.openlmis.requisition.dto.RequisitionLineItemV2Dto;
import org.siglus.common.domain.referencedata.SupervisoryNode;
import org.siglus.common.dto.referencedata.FacilityDto;
import org.siglus.common.dto.referencedata.OrderableDto;
import org.siglus.common.service.client.SiglusFacilityReferenceDataService;
import org.siglus.siglusapi.domain.Cp;
import org.siglus.siglusapi.domain.RequisitionLineItemExtension;
import org.siglus.siglusapi.dto.fc.CpDto;
import org.siglus.siglusapi.repository.CpRepository;
import org.siglus.siglusapi.repository.SiglusRequisitionLineItemExtensionRepository;
import org.siglus.siglusapi.repository.SiglusRequisitionRepository;
import org.siglus.siglusapi.repository.SupervisoryNodeRepository;
import org.siglus.siglusapi.service.client.SiglusOrderableReferenceDataService;
import org.siglus.siglusapi.service.client.SiglusProcessingPeriodReferenceDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class FcIntegrationCpService {

  public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

  @Autowired
  private CpRepository cpRepository;

  @Autowired
  private SiglusOrderableReferenceDataService orderableReferenceDataService;

  @Autowired
  private SiglusFacilityReferenceDataService facilityReferenceDataService;

  @Autowired
  private SiglusProcessingPeriodReferenceDataService processingPeriodReferenceDataService;

  @Autowired
  private SupervisoryNodeRepository supervisoryNodeRepository;

  @Autowired
  private SiglusRequisitionRepository siglusRequisitionRepository;

  @Autowired
  private SiglusRequisitionLineItemExtensionRepository lineItemExtensionRepository;

  public boolean dealCpData(List<CpDto> dtos) {
    try {
      List<Cp> cps = Cp.from(dtos);
      cps.forEach(cp -> {
        Cp existCp = cpRepository
            .findCpByFacilityCodeAndProductCodeAndPeriodAndYear(cp.getFacilityCode(),
                cp.getProductCode(), cp.getPeriod(), cp.getYear());
        if (null != existCp) {
          cp.setId(existCp.getId());
        }
        cpRepository.save(cp);
      });
      log.info("save cp successfully, size: {}", cps.size());
      return true;
    } catch (Exception e) {
      log.error("deal cp data error", e);
      return false;
    }
  }

  public void initiateSuggestedQuantity(List<BaseRequisitionLineItemDto> lineItems, UUID facilityId,
      UUID processingPeriodId, UUID programId) {
    if (CollectionUtils.isEmpty(lineItems)) {
      return;
    }
    FacilityDto requestingFacility = facilityReferenceDataService.findOne(facilityId);
    ProcessingPeriodDto processingPeriodDto = processingPeriodReferenceDataService
        .findOne(processingPeriodId);
    LocalDate endDate = processingPeriodDto.getEndDate();
    LocalDate firstDayOfPeriodMonth = endDate.withDayOfMonth(1);
    String firstDayOfThisMonth = firstDayOfPeriodMonth.format(FORMATTER);
    String firstDayOfNextMonth = firstDayOfPeriodMonth.plusMonths(1).format(FORMATTER);
    Set<UUID> orderableIds = lineItems.stream().map(lineItem -> {
      RequisitionLineItemV2Dto lineItemV2Dto = (RequisitionLineItemV2Dto) lineItem;
      return lineItemV2Dto.getOrderable().getId();
    }).collect(Collectors.toSet());
    Map<UUID, String> orderableIdCodeMap = orderableReferenceDataService.findByIds(orderableIds)
        .stream().collect(toMap(OrderableDto::getId, OrderableDto::getProductCode));
    Map<String, Cp> productCodeCpMap = cpRepository
        .findAllByFacilityCodeAndProductCodeInAndPeriodAndYear(requestingFacility.getCode(),
            orderableIdCodeMap.values(), "M" + endDate.getMonthValue(), endDate.getYear())
        .stream().collect(toMap(Cp::getProductCode, Function.identity()));
    Map<UUID, Integer> orderableIdSumStockMap = newHashMap();
    Set<UUID> supervisoryNodeIds = supervisoryNodeRepository.findAllByFacilityId(facilityId)
        .stream().map(SupervisoryNode::getId).collect(toSet());
    if (CollectionUtils.isNotEmpty(supervisoryNodeIds)) {
      List<Requisition> requisitions = siglusRequisitionRepository
          .searchForSuggestedQuantity(programId, supervisoryNodeIds, firstDayOfThisMonth,
              firstDayOfNextMonth);
      orderableIdSumStockMap = requisitions.stream()
          .map(Requisition::getNonSkippedRequisitionLineItems)
          .flatMap(Collection::stream)
          .collect(Collectors.groupingBy(lineItem -> lineItem.getOrderable().getId(),
              Collectors.summingInt(RequisitionLineItem::getStockOnHand)));
    }
    Map<UUID, Integer> finalOrderableIdSumStockMap = orderableIdSumStockMap;
    List<RequisitionLineItemExtension> extensions = newArrayList();
    lineItems.forEach(lineItem -> {
      RequisitionLineItemV2Dto lineItemV2Dto = (RequisitionLineItemV2Dto) lineItem;
      UUID orderableId = lineItemV2Dto.getOrderable().getId();
      Integer sumStock = finalOrderableIdSumStockMap.get(orderableId);
      sumStock = sumStock == null ? 0 : sumStock;
      String productCode = orderableIdCodeMap.get(orderableId);
      Cp cp = productCodeCpMap.get(productCode);
      int suggestedQuantity = 0;
      if (null != cp) {
        suggestedQuantity = cp.getCp() * cp.getMax() - sumStock - lineItem.getStockOnHand();
        suggestedQuantity = Math.max(suggestedQuantity, 0);
      }
      lineItem.setSuggestedQuantity(suggestedQuantity);
      if (null != lineItem.getId()) {
        RequisitionLineItemExtension extension = new RequisitionLineItemExtension();
        extension.setRequisitionLineItemId(lineItem.getId());
        extension.setAuthorizedQuantity(lineItem.getAuthorizedQuantity());
        extension.setSuggestedQuantity(suggestedQuantity);
        extensions.add(extension);
      }
    });
    if (CollectionUtils.isNotEmpty(extensions)) {
      log.info("save line item extension, size: {}", extensions.size());
      lineItemExtensionRepository.save(extensions);
    }
  }
}
