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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.openlmis.referencedata.domain.SupervisoryNode;
import org.openlmis.referencedata.dto.OrderableDto;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.domain.requisition.RequisitionLineItem;
import org.openlmis.requisition.dto.BaseRequisitionLineItemDto;
import org.openlmis.requisition.dto.ProcessingPeriodDto;
import org.openlmis.requisition.dto.RequisitionLineItemV2Dto;
import org.siglus.siglusapi.domain.CmmDomain;
import org.siglus.siglusapi.domain.CpDomain;
import org.siglus.siglusapi.dto.FacilityDto;
import org.siglus.siglusapi.repository.CmmRepository;
import org.siglus.siglusapi.repository.CpRepository;
import org.siglus.siglusapi.repository.SiglusRequisitionRepository;
import org.siglus.siglusapi.repository.SupervisoryNodeRepository;
import org.siglus.siglusapi.service.client.SiglusFacilityReferenceDataService;
import org.siglus.siglusapi.service.client.SiglusOrderableReferenceDataService;
import org.siglus.siglusapi.service.client.SiglusProcessingPeriodReferenceDataService;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class FcCmmCpService {

  public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

  private final CmmRepository cmmRepository;
  private final CpRepository cpRepository;
  private final SiglusOrderableReferenceDataService orderableReferenceDataService;
  private final SiglusFacilityReferenceDataService facilityReferenceDataService;
  private final SiglusProcessingPeriodReferenceDataService processingPeriodReferenceDataService;
  private final SupervisoryNodeRepository supervisoryNodeRepository;
  private final SiglusRequisitionRepository siglusRequisitionRepository;

  public void initiateSuggestedQuantityByCmm(List<BaseRequisitionLineItemDto> lineItems,
      UUID facilityId, UUID processingPeriodId) {
    if (CollectionUtils.isEmpty(lineItems)) {
      return;
    }
    Map<UUID, String> orderableIdCodeMap = getOrderableIdCodeMap(lineItems);
    FacilityDto requestingFacility = facilityReferenceDataService.findOne(facilityId);
    ProcessingPeriodDto processingPeriodDto = processingPeriodReferenceDataService.findOne(processingPeriodId);
    LocalDate endDate = processingPeriodDto.getEndDate();
    Map<String, CmmDomain> productCodeCmmMap = cmmRepository
        .findAllByFacilityCodeAndProductCodeInAndQueryDate(requestingFacility.getCode(),
            orderableIdCodeMap.values(), endDate.format(DateTimeFormatter.ofPattern("MM-yyyy")))
        .stream().collect(toMap(CmmDomain::getProductCode, Function.identity()));
    lineItems.forEach(lineItem -> {
      RequisitionLineItemV2Dto lineItemV2Dto = (RequisitionLineItemV2Dto) lineItem;
      String productCode = orderableIdCodeMap.get(lineItemV2Dto.getOrderable().getId());
      CmmDomain cmm = productCodeCmmMap.get(productCode);
      int suggestedQuantity = 0;
      if (null != cmm) {
        suggestedQuantity = cmm.getCmm() * cmm.getMax() - lineItem.getStockOnHand();
        suggestedQuantity = Math.max(suggestedQuantity, 0);
      }
      lineItem.setSuggestedQuantity(suggestedQuantity);
    });
  }

  public void initiateSuggestedQuantityByCp(List<BaseRequisitionLineItemDto> lineItems,
      UUID facilityId, UUID processingPeriodId, UUID programId) {
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
    Map<UUID, String> orderableIdCodeMap = getOrderableIdCodeMap(lineItems);
    Map<String, CpDomain> productCodeCpMap = cpRepository
        .findAllByFacilityCodeAndProductCodeInAndQueryDate(requestingFacility.getCode(),
            orderableIdCodeMap.values(), endDate.format(DateTimeFormatter.ofPattern("MM-yyyy")))
        .stream().collect(toMap(CpDomain::getProductCode, Function.identity()));
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
    lineItems.forEach(lineItem -> {
      RequisitionLineItemV2Dto lineItemV2Dto = (RequisitionLineItemV2Dto) lineItem;
      UUID orderableId = lineItemV2Dto.getOrderable().getId();
      Integer sumStock = finalOrderableIdSumStockMap.get(orderableId);
      sumStock = sumStock == null ? 0 : sumStock;
      String productCode = orderableIdCodeMap.get(orderableId);
      CpDomain cp = productCodeCpMap.get(productCode);
      int suggestedQuantity = 0;
      if (null != cp) {
        suggestedQuantity = cp.getCp() * cp.getMax() - sumStock - lineItem.getStockOnHand();
        suggestedQuantity = Math.max(suggestedQuantity, 0);
      }
      lineItem.setSuggestedQuantity(suggestedQuantity);
    });
  }

  private Map<UUID, String> getOrderableIdCodeMap(List<BaseRequisitionLineItemDto> lineItems) {
    Set<UUID> orderableIds = lineItems.stream()
        .map(lineItem -> {
          RequisitionLineItemV2Dto lineItemV2Dto = (RequisitionLineItemV2Dto) lineItem;
          return lineItemV2Dto.getOrderable().getId();
        })
        .collect(Collectors.toSet());
    return orderableReferenceDataService.findByIds(orderableIds).stream()
        .collect(toMap(OrderableDto::getId, OrderableDto::getProductCode));
  }

}
