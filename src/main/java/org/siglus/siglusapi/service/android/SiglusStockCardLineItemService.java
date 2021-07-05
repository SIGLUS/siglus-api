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

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.openlmis.stockmanagement.domain.card.StockCardLineItem;
import org.openlmis.stockmanagement.domain.event.CalculatedStockOnHand;
import org.openlmis.stockmanagement.domain.physicalinventory.PhysicalInventoryLineItemAdjustment;
import org.openlmis.stockmanagement.domain.reason.ReasonType;
import org.openlmis.stockmanagement.domain.sourcedestination.Organization;
import org.openlmis.stockmanagement.dto.CalculatedStockOnHandDto;
import org.openlmis.stockmanagement.dto.StockCardLineItemDto;
import org.openlmis.stockmanagement.dto.StockCardLineItemReasonDto;
import org.openlmis.stockmanagement.repository.CalculatedStockOnHandRepository;
import org.openlmis.stockmanagement.repository.OrganizationRepository;
import org.siglus.siglusapi.domain.StockEventProductRequested;
import org.siglus.siglusapi.dto.android.response.LotMovementItemResponse;
import org.siglus.siglusapi.dto.android.response.SiglusLotResponse;
import org.siglus.siglusapi.dto.android.response.SiglusStockMovementItemResponse;
import org.siglus.siglusapi.repository.SiglusStockCardLineItemRepository;
import org.siglus.siglusapi.repository.StockEventProductRequestedRepository;
import org.siglus.siglusapi.service.android.SiglusMeService.MovementType;
import org.springframework.data.domain.Example;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@SuppressWarnings("PMD.TooManyMethods")
public class SiglusStockCardLineItemService {

  private static final String INVENTORY_NEGATIVE = "INVENTORY_NEGATIVE";
  private static final String INVENTORY_POSITIVE = "INVENTORY_POSITIVE";
  private static final String INVENTORY = "INVENTORY";
  private static final String UNPACK_KIT = "Unpack Kit";

  private Map<UUID, String> organizationIdToName;

  private final SiglusStockCardLineItemRepository stockCardLineItemRepository;

  private final CalculatedStockOnHandRepository calculatedStockOnHandRepository;

  private final OrganizationRepository organizationRepository;

  private final StockEventProductRequestedRepository requestQuantityRepository;

  public Map<UUID, List<SiglusStockMovementItemResponse>> getStockMovementByOrderableId(UUID facilityId,
      String startTime,
      String endTime, Map<UUID, SiglusLotResponse> siglusLotResponseByLotId) {
    organizationIdToName = mapOrganizationIdToName();
    Map<UUID, Map<LocalDate, CalculatedStockOnHandDto>> stockOnHandDtoMap = mapStockOnHandByStockCardIdAndOccurredDate(
        facilityId, startTime, endTime);
    Map<UUID, List<StockCardLineItem>> lineItemByOrderableIdMap = mapStockCardLineItemByOrderableId(facilityId,
        startTime, endTime);
    return getStockMovementItemDtosMap(stockOnHandDtoMap, lineItemByOrderableIdMap, siglusLotResponseByLotId);
  }

  private Map<UUID, List<SiglusStockMovementItemResponse>> getStockMovementItemDtosMap(
      Map<UUID, Map<LocalDate, CalculatedStockOnHandDto>> stockOnHandDtoMap,
      Map<UUID, List<StockCardLineItem>> lineItemByOrderableIdMap,
      Map<UUID, SiglusLotResponse> siglusLotResponseByLotId) {
    if (stockOnHandDtoMap.isEmpty() || lineItemByOrderableIdMap.isEmpty()) {
      return Collections.emptyMap();
    }
    Map<UUID, List<SiglusStockMovementItemResponse>> stockMovementItemResponsesMap = new HashMap<>();
    lineItemByOrderableIdMap.forEach((orderableId, lineItems) -> {
      Map<ZonedDateTime, List<StockCardLineItemDto>> itemDtoByProcessedDateMap = itemDtoByProcessedDateMap(
          stockOnHandDtoMap, lineItemByOrderableIdMap, orderableId);
      stockMovementItemResponsesMap.put(orderableId,
          convertSiglusStockMovementItemResponses(itemDtoByProcessedDateMap, siglusLotResponseByLotId));
    });
    return stockMovementItemResponsesMap;
  }

  private List<SiglusStockMovementItemResponse> convertSiglusStockMovementItemResponses(
      Map<ZonedDateTime, List<StockCardLineItemDto>> itemDtoByProcessedDateMap,
      Map<UUID, SiglusLotResponse> siglusLotResponseByLotId) {
    List<SiglusStockMovementItemResponse> siglusStockMovementItemResponses = new ArrayList<>();
    itemDtoByProcessedDateMap.forEach((processedDate, itemDtos) -> {
      Optional<StockCardLineItemDto> first = itemDtos.stream().findFirst();
      first.ifPresent(itemDto -> siglusStockMovementItemResponses
          .add(convertStockMovementItemResponse(itemDto, itemDtos, siglusLotResponseByLotId)));
    });
    return siglusStockMovementItemResponses;
  }

  private SiglusStockMovementItemResponse convertStockMovementItemResponse(StockCardLineItemDto firstItemDto,
      List<StockCardLineItemDto> itemDtos, Map<UUID, SiglusLotResponse> siglusLotResponseByLotId) {
    SiglusStockMovementItemResponse stockMovementItemResponse = getFirstStockMovementItemResponse(firstItemDto);
    if (firstItemDto.getStockCard().getLotId() == null) {
      convertStockMovementItemResponseWhenNoLot(stockMovementItemResponse, firstItemDto);
    } else {
      List<LotMovementItemResponse> lotMovementItems = new ArrayList<>();
      itemDtos.forEach(itemDto -> {
        stockMovementItemResponse
            .setMovementQuantity(Math.addExact(stockMovementItemResponse.getMovementQuantity(), itemDto.getQuantity()));
        stockMovementItemResponse
            .setStockOnHand(Math.addExact(stockMovementItemResponse.getStockOnHand(), itemDto.getStockOnHand()));
        lotMovementItems.add(convertLotMovementItemDto(itemDto, siglusLotResponseByLotId));
      });
      stockMovementItemResponse.setLotMovementItems(lotMovementItems);
    }
    return stockMovementItemResponse;
  }

  private void convertStockMovementItemResponseWhenNoLot(SiglusStockMovementItemResponse stockMovementItemResponse,
      StockCardLineItemDto itemDto) {
    stockMovementItemResponse.setMovementQuantity(itemDto.getQuantity());
    stockMovementItemResponse.setStockOnHand(itemDto.getStockOnHand());
    stockMovementItemResponse.setDocumentNumber(itemDto.getDocumentNumber());
    if (itemDto.getReason() != null) {
      if (UNPACK_KIT.equals(itemDto.getReason().getName())) {
        stockMovementItemResponse.setType(MovementType.UNPACK_KIT.name());
      } else {
        stockMovementItemResponse.setReason(itemDto.getReason().getName());
        stockMovementItemResponse.setType(itemDto.getReason().getType());
      }
    }
  }

  private Map<ZonedDateTime, List<StockCardLineItemDto>> itemDtoByProcessedDateMap(
      Map<UUID, Map<LocalDate, CalculatedStockOnHandDto>> stockOnHandDtoMap,
      Map<UUID, List<StockCardLineItem>> lineItemByOrderableIdMap, UUID orderableId) {
    Map<ZonedDateTime, List<StockCardLineItemDto>> stockCardLineItemDtoByProcessedDateMap = new HashMap<>();
    lineItemByOrderableIdMap.get(orderableId).stream()
        .sorted(Comparator.comparing(StockCardLineItem::getProcessedDate).reversed())
        .collect(Collectors.toList())
        .forEach(item -> calculateSohAndConvertItemDtoByProcessedDate(item, stockCardLineItemDtoByProcessedDateMap,
            stockOnHandDtoMap));
    return stockCardLineItemDtoByProcessedDateMap;
  }

  private void calculateSohAndConvertItemDtoByProcessedDate(StockCardLineItem item,
      Map<ZonedDateTime, List<StockCardLineItemDto>> stockCardLineItemDtoByProcessedDateMap,
      Map<UUID, Map<LocalDate, CalculatedStockOnHandDto>> stockOnHandDtoMap) {
    List<StockCardLineItemDto> stockCardLineItemDtos = stockCardLineItemDtoByProcessedDateMap
        .get(item.getProcessedDate());
    if (stockCardLineItemDtos == null) {
      stockCardLineItemDtos = new ArrayList<>();
    }
    UUID stockCardId = item.getStockCard().getId();
    CalculatedStockOnHandDto stockOnHand = stockOnHandDtoMap.get(stockCardId).get(item.getOccurredDate());
    Integer currentSoh = stockOnHand.getStockOnHand();
    int quantity = getQuantityWithSign(item);
    stockOnHand.setStockOnHand(currentSoh - quantity);
    stockOnHandDtoMap.get(stockCardId).put(item.getOccurredDate(), stockOnHand);
    stockCardLineItemDtos.add(convertStockCardLineItemDto(item, quantity, currentSoh));
    stockCardLineItemDtoByProcessedDateMap.put(item.getProcessedDate(), stockCardLineItemDtos);
  }

  private int getQuantityWithSign(StockCardLineItem item) {
    if (item.isPhysicalInventory()) {
      return getPhysicalInventoryQuantity(item);
    }
    return item.getQuantityWithSign();
  }

  private int getPhysicalInventoryQuantity(StockCardLineItem item) {
    int quantity = 0;
    List<PhysicalInventoryLineItemAdjustment> stockAdjustments = item.getStockAdjustments();
    if (stockAdjustments != null && !stockAdjustments.isEmpty()) {
      for (PhysicalInventoryLineItemAdjustment adjustment : stockAdjustments) {
        if (adjustment.getReason().getReasonType() == ReasonType.CREDIT) {
          quantity = quantity + adjustment.getQuantity();
        } else if (adjustment.getReason().getReasonType() == ReasonType.DEBIT) {
          quantity = quantity - adjustment.getQuantity();
        }
      }
    }
    return quantity;
  }

  private LotMovementItemResponse convertLotMovementItemDto(StockCardLineItemDto itemDto,
      Map<UUID, SiglusLotResponse> siglusLotDtoByLotId) {
    return LotMovementItemResponse.builder()
        .lotCode(siglusLotDtoByLotId.get(itemDto.getStockCard().getLotId()).getLotCode())
        .documentNumber(itemDto.getDocumentNumber())
        .reason(itemDto.getReason() == null ? null : itemDto.getReason().getName())
        .quantity(itemDto.getQuantity())
        .stockOnHand(itemDto.getStockOnHand()).build();
  }

  private SiglusStockMovementItemResponse getFirstStockMovementItemResponse(StockCardLineItemDto firstItem) {
    return SiglusStockMovementItemResponse.builder()
        .requested(getRequested(firstItem))
        .type(firstItem.getReason().getType()).signature(firstItem.getSignature())
        .processedDate(firstItem.getProcessedDate().toInstant())
        .documentNumber(firstItem.getDocumentNumber())
        .occurredDate(firstItem.getOccurredDate()).movementQuantity(0).stockOnHand(0).build();
  }

  private Integer getRequested(StockCardLineItemDto itemDto) {
    StockEventProductRequested stockEventExtension = requestQuantityRepository.findOne(
        Example.of(StockEventProductRequested.builder().orderableId(itemDto.getStockCard().getOrderableId())
            .stockeventId(itemDto.getLineItem().getOriginEvent().getId()).build()));
    if (stockEventExtension == null) {
      return null;
    }
    return stockEventExtension.getRequestedQuantity();
  }

  private Map<UUID, List<StockCardLineItem>> mapStockCardLineItemByOrderableId(UUID facilityId, String startTime,
      String endTime) {
    List<StockCardLineItem> stockCardLineItems = stockCardLineItemRepository
        .findByFacilityIdAndIdStartTimeEndTime(facilityId, startTime, endTime);
    if (stockCardLineItems == null) {
      return Collections.emptyMap();
    }
    return stockCardLineItems.stream()
        .collect(Collectors.groupingBy(item -> item.getStockCard().getOrderableId()));
  }

  private Map<UUID, Map<LocalDate, CalculatedStockOnHandDto>> mapStockOnHandByStockCardIdAndOccurredDate(
      UUID facilityId, String startTime, String endTime) {
    List<CalculatedStockOnHand> calculatedStockOnHands = calculatedStockOnHandRepository
        .findByFacilityIdAndIdStartTimeEndTime(facilityId, startTime, endTime);
    if (calculatedStockOnHands.isEmpty()) {
      return Collections.emptyMap();
    }
    return calculatedStockOnHands.stream()
        .collect(Collectors.groupingBy(stock -> stock.getStockCard().getId())).entrySet().stream()
        .collect(Collectors.toMap(Entry::getKey, e -> mapStockOnHandByOccurredDate(e.getValue())));
  }

  private Map<LocalDate, CalculatedStockOnHandDto> mapStockOnHandByOccurredDate(List<CalculatedStockOnHand> list) {
    return list.stream()
        .collect(Collectors.toMap(CalculatedStockOnHand::getOccurredDate, this::convertStockOnHandToDto));
  }

  private CalculatedStockOnHandDto convertStockOnHandToDto(CalculatedStockOnHand stockOnHand) {
    return CalculatedStockOnHandDto.newInstance(stockOnHand);
  }

  private StockCardLineItemDto convertStockCardLineItemDto(StockCardLineItem item, int quantity, int currentSoh) {
    return StockCardLineItemDto.builder().signature(item.getSignature()).processedDate(item.getProcessedDate())
        .occurredDate(item.getOccurredDate()).stockOnHand(currentSoh).quantity(quantity)
        .documentNumber(item.getDocumentNumber())
        .reason(getStockCardLineItemReasonDto(item, quantity))
        .stockCard(item.getStockCard())
        .lineItem(item)
        .build();
  }

  private StockCardLineItemReasonDto getStockCardLineItemReasonDto(StockCardLineItem item, Integer quantity) {
    StockCardLineItemReasonDto reasonDto = new StockCardLineItemReasonDto();
    String reason = null;
    String type = null;
    if (item.isPhysicalInventory()) {
      type = MovementType.PHYSICAL_INVENTORY.name();
      reason = getPhysicalInventoryReason(quantity);
    } else if (isAdjustment(item)) {
      type = MovementType.ADJUSTMENT.name();
      reason = getAdjustmentReason(item);
    } else if (isReceive(item)) {
      type = MovementType.RECEIVE.name();
      reason = SiglusMeService.Source.findByValue(organizationIdToName.get(item.getSource().getReferenceId()));
    } else if (isIssue(item)) {
      type = MovementType.ISSUE.name();
      reason = SiglusMeService.Destination
          .findByValue(organizationIdToName.get(item.getDestination().getReferenceId()));
    }
    reasonDto.setType(type);
    reasonDto.setName(reason);
    return reasonDto;
  }

  private boolean isReceive(StockCardLineItem item) {
    return item.isPositive();
  }

  private boolean isIssue(StockCardLineItem item) {
    return item.getDestination() != null && item.getSource() == null;
  }

  private boolean isAdjustment(StockCardLineItem item) {
    return item.getSource() == null && item.getDestination() == null && item.getReason() != null;
  }

  private String getPhysicalInventoryReason(Integer quantity) {
    if (quantity < 0) {
      return INVENTORY_NEGATIVE;
    } else if (quantity > 0) {
      return INVENTORY_POSITIVE;
    } else {
      return INVENTORY;
    }
  }

  private String getAdjustmentReason(StockCardLineItem item) {
    if (UNPACK_KIT.equals(item.getReason().getName())) {
      return item.getReason().getName();
    } else {
      return SiglusMeService.AdjustmentReason.findByValue(item.getReason().getName());
    }
  }

  private Map<UUID, String> mapOrganizationIdToName() {
    return organizationRepository.findAll().stream()
        .collect(Collectors.toMap(Organization::getId, Organization::getName));
  }
}