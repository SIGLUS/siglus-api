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

import ca.uhn.fhir.util.ObjectUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.openlmis.referencedata.domain.Lot;
import org.openlmis.referencedata.domain.Orderable;
import org.openlmis.stockmanagement.domain.physicalinventory.PhysicalInventory;
import org.openlmis.stockmanagement.domain.physicalinventory.PhysicalInventoryLineItem;
import org.openlmis.stockmanagement.domain.physicalinventory.PhysicalInventoryLineItemAdjustment;
import org.siglus.siglusapi.dto.SiglusPhysicalInventoryHistoryDto;
import org.siglus.siglusapi.dto.SiglusPhysicalInventoryHistoryLineItemDto;
import org.siglus.siglusapi.dto.SiglusPhysicalInventoryHistoryListDto;
import org.siglus.siglusapi.dto.UserDto;
import org.siglus.siglusapi.repository.OrderableRepository;
import org.siglus.siglusapi.repository.PhysicalInventoryHistoryRepository;
import org.siglus.siglusapi.repository.PhysicalInventoryLineItemsExtensionRepository;
import org.siglus.siglusapi.repository.SiglusLotRepository;
import org.siglus.siglusapi.repository.SiglusPhysicalInventoryRepository;
import org.siglus.siglusapi.util.SiglusAuthenticationHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

@Service
@Slf4j
public class SiglusPhysicalInventoryHistoryService {

  @Autowired
  private PhysicalInventoryLineItemsExtensionRepository lineItemsExtensionRepository;
  @Autowired
  private OrderableRepository orderableRepository;
  @Autowired
  private SiglusAuthenticationHelper authenticationHelper;
  @Autowired
  private PhysicalInventoryHistoryRepository physicalInventoryHistoryRepository;
  @Autowired
  private SiglusPhysicalInventoryRepository siglusPhysicalInventoryRepository;
  @Autowired
  private SiglusLotRepository siglusLotRepository;

  public List<SiglusPhysicalInventoryHistoryListDto> searchPhysicalInventoryHistories() {
    UserDto currentUser = authenticationHelper.getCurrentUser();
    if (ObjectUtils.isEmpty(currentUser) || ObjectUtils.isEmpty(currentUser.getHomeFacilityId())) {
      return new ArrayList<>();
    }
    UUID facilityId = currentUser.getHomeFacilityId();
    Map<UUID, List<SiglusPhysicalInventoryHistoryDto>> groupIdToHistoryDtosMap = physicalInventoryHistoryRepository
        .queryPhysicalInventoryHistories(facilityId)
        .stream()
        .collect(Collectors.groupingBy(SiglusPhysicalInventoryHistoryDto::getGroupId));
    return groupIdToHistoryDtosMap.entrySet().stream()
        .map(SiglusPhysicalInventoryHistoryListDto::from)
        .collect(Collectors.toList());
  }

  public List<SiglusPhysicalInventoryHistoryLineItemDto> searchPhysicalInventoryHistoriesLineItem(UUID groupId) {
    UserDto currentUser = authenticationHelper.getCurrentUser();
    if (ObjectUtils.isEmpty(currentUser)
        || ObjectUtils.isEmpty(currentUser.getHomeFacilityId())
        || ObjectUtils.isEmpty(groupId)) {
      return new ArrayList<>();
    }
    UUID facilityId = currentUser.getHomeFacilityId();
    Set<UUID> physicalInventoryIds = physicalInventoryHistoryRepository.queryPhysicalInventoryHistories(facilityId)
        .stream()
        .filter(dto -> ObjectUtil.equals(groupId, dto.getGroupId()))
        .map(SiglusPhysicalInventoryHistoryDto::getPhysicalInventoryId)
        .collect(Collectors.toSet());
    List<PhysicalInventory> physicalInventories = siglusPhysicalInventoryRepository.findAllByIdIn(physicalInventoryIds);
    List<PhysicalInventoryLineItem> lineItems = physicalInventories.stream()
        .map(PhysicalInventory::getLineItems)
        .flatMap(List::stream)
        .collect(Collectors.toList());
    Map<UUID, Orderable> idToOrderableMap = buildIdToOrderableMap(lineItems);
    Map<UUID, Lot> idToLotMap = buildIdToLotMap(lineItems);
    Map<UUID, String> lineItemIdToCommentMap = buildLineItemIdToCommentMap(physicalInventoryIds);

    return physicalInventories.stream()
        .map(inventory -> buildSiglusPhysicalInventoryHistoryLineItemDtoList(
            idToOrderableMap,
            idToLotMap,
            lineItemIdToCommentMap,
            inventory))
        .flatMap(List::stream)
        .collect(Collectors.toList());
  }

  private List<SiglusPhysicalInventoryHistoryLineItemDto> buildSiglusPhysicalInventoryHistoryLineItemDtoList(
      Map<UUID, Orderable> idToOrderableMap,
      Map<UUID, Lot> idToLotMap, Map<UUID, String> lineItemIdToCommentMap, PhysicalInventory inventory) {
    return inventory.getLineItems().stream()
        .map(lineItem -> {
          Orderable orderable = idToOrderableMap.get(lineItem.getOrderableId());
          Lot lot = idToLotMap.get(lineItem.getLotId());
          List<PhysicalInventoryLineItemAdjustment> stockAdjustments = lineItem.getStockAdjustments();
          return SiglusPhysicalInventoryHistoryLineItemDto.builder()
              .productCode(orderable == null ? null : orderable.getProductCode().toString())
              .productName(orderable == null ? null : orderable.getFullProductName())
              .lotCode(lot == null ? null : lot.getLotCode())
              .expiryDate(lot == null ? null : lot.getExpirationDate())
              .currentStock(lineItem.getQuantity())
              .reasons(CollectionUtils.isEmpty(stockAdjustments) ? null
                  : lineItem.getStockAdjustments().get(0).getReason().getName())
              .reasonQuantity(CollectionUtils.isEmpty(stockAdjustments) ? null
                  : lineItem.getStockAdjustments().get(0).getQuantity())
              .comments(lineItemIdToCommentMap.get(lineItem.getId()))
              .build();
        }).collect(Collectors.toList());
  }

  private Map<UUID, String> buildLineItemIdToCommentMap(Set<UUID> physicalInventoryIds) {
    Map<UUID, String> lineItemIdToCommentMap = new HashMap<>();
    lineItemsExtensionRepository.findByPhysicalInventoryIdIn(physicalInventoryIds).forEach(
        lineItemExtension -> lineItemIdToCommentMap.put(lineItemExtension.getPhysicalInventoryLineItemId(),
            lineItemExtension.getReasonFreeText())
    );
    return lineItemIdToCommentMap;
  }

  private Map<UUID, Lot> buildIdToLotMap(List<PhysicalInventoryLineItem> lineItems) {
    Set<UUID> lotIds = lineItems.stream()
        .map(PhysicalInventoryLineItem::getLotId)
        .collect(Collectors.toSet());
    List<Lot> lots = siglusLotRepository.findAllByIdIn(lotIds);
    return lots.stream()
        .collect(Collectors.toMap(Lot::getId, Function.identity()));
  }

  private Map<UUID, Orderable> buildIdToOrderableMap(List<PhysicalInventoryLineItem> lineItems) {
    Set<UUID> orderableIds = lineItems.stream()
        .map(PhysicalInventoryLineItem::getOrderableId)
        .collect(Collectors.toSet());
    List<Orderable> orderables = orderableRepository.findLatestByIds(orderableIds);
    return orderables.stream()
        .collect(Collectors.toMap(Orderable::getId, Function.identity()));
  }
}
