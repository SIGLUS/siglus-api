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

package org.siglus.siglusapi.service.android;

import static java.util.stream.Collectors.toMap;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.NotEmpty;
import org.openlmis.requisition.dto.OrderableDto;
import org.openlmis.stockmanagement.domain.card.StockCardLineItem;
import org.openlmis.stockmanagement.domain.physicalinventory.PhysicalInventory;
import org.openlmis.stockmanagement.domain.physicalinventory.PhysicalInventoryLineItem;
import org.openlmis.stockmanagement.domain.physicalinventory.PhysicalInventoryLineItemAdjustment;
import org.openlmis.stockmanagement.repository.CalculatedStockOnHandRepository;
import org.openlmis.stockmanagement.repository.PhysicalInventoriesRepository;
import org.siglus.siglusapi.domain.StockCardDeletedBackup;
import org.siglus.siglusapi.dto.UserDto;
import org.siglus.siglusapi.dto.android.request.StockCardDeleteRequest;
import org.siglus.siglusapi.dto.android.response.FacilityProductMovementsResponse;
import org.siglus.siglusapi.dto.android.response.ProductMovementResponse;
import org.siglus.siglusapi.exception.NotFoundException;
import org.siglus.siglusapi.repository.FacilityCmmsRepository;
import org.siglus.siglusapi.repository.PhysicalInventoryLineItemAdjustmentRepository;
import org.siglus.siglusapi.repository.PhysicalInventoryLineItemRepository;
import org.siglus.siglusapi.repository.SiglusStockCardLineItemRepository;
import org.siglus.siglusapi.repository.SiglusStockCardRepository;
import org.siglus.siglusapi.repository.StockCardDeletedBackupRepository;
import org.siglus.siglusapi.util.SiglusAuthenticationHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("PMD.TooManyMethods")
public class StockCardDeleteService {

  private final SiglusAuthenticationHelper authHelper;

  private final StockCardCreateService stockCardCreateService;

  private final StockCardSearchService stockCardSearchService;

  private final SiglusStockCardLineItemRepository stockCardLineItemRepository;

  private final CalculatedStockOnHandRepository calculatedStockOnHandRepository;

  private final SiglusStockCardRepository siglusStockCardRepository;

  private final PhysicalInventoriesRepository physicalInventoriesRepository;

  private final PhysicalInventoryLineItemRepository physicalInventoryLineItemRepository;

  private final PhysicalInventoryLineItemAdjustmentRepository physicalInventoryLineItemAdjustmentRepository;

  private final StockCardDeletedBackupRepository stockCardDeletedBackupRepository;

  private final FacilityCmmsRepository facilityCmmsRepository;

  @Transactional
  public void deleteStockCardByProduct(@Valid @NotEmpty List<StockCardDeleteRequest> stockCardDeleteRequests) {
    List<org.openlmis.requisition.dto.OrderableDto> orderableDtos = stockCardCreateService.getAllApprovedProducts();
    UserDto userDto = authHelper.getCurrentUser();
    Map<String, UUID> orderableCodeToId = orderableCodeToIdMap(orderableDtos);
    Set<UUID> orderableIds = stockCardDeleteRequests.stream()
        .map(r -> orderableCodeToId.get(r.getProductCode()))
        .collect(Collectors.toSet());
    if (orderableIds.contains(null)) {
      throw new NotFoundException("There are products that do not exist in the approved product list");
    }
    FacilityProductMovementsResponse productMovementsResponse = stockCardSearchService
        .getProductMovementsByOrderables(orderableIds);
    Map<String, ProductMovementResponse> productCodeToMovements = productMovementsResponse.getProductMovements()
        .stream()
        .collect(Collectors.toMap(ProductMovementResponse::getProductCode, Function.identity()));
    List<StockCardDeletedBackup> stockCardDeletedBackups = stockCardDeleteRequests.stream()
        .map(r -> buildStockCardBackup(r, productCodeToMovements.get(r.getProductCode()),
            orderableCodeToId.get(r.getProductCode()), userDto.getHomeFacilityId(), userDto.getId()))
        .collect(Collectors.toList());
    log.info("save stock card deleted backup info: {}", stockCardDeletedBackups);
    stockCardDeletedBackupRepository.save(stockCardDeletedBackups);
    deleteStockCardMovement(userDto.getHomeFacilityId(), orderableIds);
  }

  private Map<String, UUID> orderableCodeToIdMap(
      List<OrderableDto> orderableDtos) {
    return orderableDtos.stream()
        .collect(toMap(OrderableDto::getProductCode,
            OrderableDto::getId));
  }

  private void deleteStockCardMovement(UUID facilityId, Set<UUID> orderableIds) {
    List<PhysicalInventory> physicalInventories = physicalInventoriesRepository
        .findByFacilityIdAndOrderableIds(facilityId, orderableIds);
    List<PhysicalInventoryLineItem> physicalInventoryLineItems = getPhysicalInventoryLineItems(physicalInventories);
    List<PhysicalInventoryLineItemAdjustment> phycicalAdjustments = getPhysicalInventoryLineItemAdjustments(
        physicalInventoryLineItems);
    List<StockCardLineItem> stockCardLineItems = stockCardLineItemRepository
        .findByFacilityIdAndOrderableIdIn(facilityId, orderableIds);
    List<PhysicalInventoryLineItemAdjustment> stockCardAdjustments = getStockCardLineItemAdjustments(
        stockCardLineItems);
    if (!phycicalAdjustments.isEmpty()) {
      log.info("delete phycical inventory line item adjustments: {}", phycicalAdjustments);
      physicalInventoryLineItemAdjustmentRepository.delete(phycicalAdjustments);
    }
    if (!stockCardAdjustments.isEmpty()) {
      log.info("delete stock card line item adjustments: {}", stockCardAdjustments);
      physicalInventoryLineItemAdjustmentRepository.delete(stockCardAdjustments);
    }
    if (!physicalInventoryLineItems.isEmpty()) {
      log.info("delete phycical inventory line item: {}", logPhysicalInventoryLineItems(physicalInventoryLineItems));
      physicalInventoryLineItemRepository.delete(physicalInventoryLineItems);
    }
    if (!physicalInventories.isEmpty()) {
      log.info("delete phycical inventory: {}", logPhysicalInventories(physicalInventories));
      physicalInventoriesRepository.delete(physicalInventories);
    }
    log.info("delete calculated stockOnHand by facilityId: {}, orderableIds: {}", facilityId, orderableIds);
    calculatedStockOnHandRepository.deleteByFacilityIdAndOrderableIds(facilityId, orderableIds);
    if (!physicalInventories.isEmpty()) {
      stockCardLineItemRepository.delete(stockCardLineItems);
    }
    log.info("delete hfCmms by facilityId : {}, orderableIds: {}", facilityId, orderableIds);
    facilityCmmsRepository.deleteHfCmmsByFacilityIdAndProductCode(facilityId, orderableIds);
    log.info("delete calculated stockOnHand by facilityId: {}, orderableIds: {}", facilityId, orderableIds);
    siglusStockCardRepository.deleteStockCardsByFacilityIdAndOrderableIdIn(facilityId, orderableIds);
  }

  private String logPhysicalInventoryLineItems(List<PhysicalInventoryLineItem> physicalInventoryLineItems) {
    StringBuilder stringBuilder = new StringBuilder(50);
    physicalInventoryLineItems.forEach(lineItem -> {
      stringBuilder.append("[lotId:");
      stringBuilder.append(lineItem.getLotId());
      stringBuilder.append(",orderableId:");
      stringBuilder.append(lineItem.getOrderableId());
      stringBuilder.append(",quantity:");
      stringBuilder.append(lineItem.getQuantity());
      stringBuilder.append("],");
    });
    return stringBuilder.toString();
  }

  private String logPhysicalInventories(List<PhysicalInventory> physicalInventories) {
    StringBuilder stringBuilder = new StringBuilder(50);
    physicalInventories.forEach(physicalInventory -> {
      stringBuilder.append("[facilityId:");
      stringBuilder.append(physicalInventory.getFacilityId());
      stringBuilder.append(",programId:");
      stringBuilder.append(physicalInventory.getProgramId());
      stringBuilder.append(",occurredDate:");
      stringBuilder.append(physicalInventory.getOccurredDate());
      stringBuilder.append("],");
    });
    return stringBuilder.toString();
  }

  private List<PhysicalInventoryLineItem> getPhysicalInventoryLineItems(List<PhysicalInventory> physicalInventories) {
    if (physicalInventories.isEmpty()) {
      return Collections.emptyList();
    }
    return physicalInventories.stream()
        .map(PhysicalInventory::getLineItems)
        .flatMap(Collection::stream)
        .collect(Collectors.toList());
  }

  private List<PhysicalInventoryLineItemAdjustment> getPhysicalInventoryLineItemAdjustments(
      List<PhysicalInventoryLineItem> physicalInventoryLineItems) {
    if (physicalInventoryLineItems.isEmpty()) {
      return Collections.emptyList();
    }
    return physicalInventoryLineItems.stream()
        .map(PhysicalInventoryLineItem::getStockAdjustments)
        .flatMap(Collection::stream)
        .collect(Collectors.toList());
  }

  private List<PhysicalInventoryLineItemAdjustment> getStockCardLineItemAdjustments(
      List<StockCardLineItem> stockCardLineItems) {
    if (stockCardLineItems.isEmpty()) {
      return Collections.emptyList();
    }
    return stockCardLineItems.stream()
        .map(StockCardLineItem::getStockAdjustments)
        .flatMap(Collection::stream)
        .collect(Collectors.toList());
  }

  private StockCardDeletedBackup buildStockCardBackup(StockCardDeleteRequest stockCardDeleteRequest,
      ProductMovementResponse productMovementResponse, UUID productId, UUID facilityId, UUID userId) {
    return StockCardDeletedBackup.builder()
        .clientmovements(stockCardDeleteRequest.getClientMovements())
        .productid(productId)
        .productMovementResponse(productMovementResponse)
        .facilityid(facilityId)
        .createdby(userId)
        .build();
  }
}