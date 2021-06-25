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

import static org.openlmis.stockmanagement.domain.reason.StockCardLineItemReason.physicalBalance;
import static org.openlmis.stockmanagement.domain.reason.StockCardLineItemReason.physicalCredit;
import static org.openlmis.stockmanagement.domain.reason.StockCardLineItemReason.physicalDebit;

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
import org.openlmis.stockmanagement.domain.reason.StockCardLineItemReason;
import org.openlmis.stockmanagement.dto.CalculatedStockOnHandDto;
import org.openlmis.stockmanagement.dto.StockCardLineItemDto;
import org.openlmis.stockmanagement.dto.StockCardLineItemReasonDto;
import org.siglus.siglusapi.dto.LotMovementItemDto;
import org.siglus.siglusapi.dto.ProductMovementDto;
import org.siglus.siglusapi.dto.SiglusStockMovementItemDto;
import org.siglus.siglusapi.repository.SiglusCalculatedStockOnHandRepository;
import org.siglus.siglusapi.repository.SiglusStockCardLineItemRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@SuppressWarnings("PMD.TooManyMethods")
public class SiglusStockCardLineItemService {

  private final SiglusStockCardLineItemRepository stockCardLineItemRepository;

  private final SiglusCalculatedStockOnHandRepository calculatedStockOnHandRepository;

  public List<ProductMovementDto> findProductMovements(UUID facilityId, String startTime,
      String endTime) {
    Map<UUID, Map<LocalDate, CalculatedStockOnHandDto>> stockOnHandDtoMap = mapStockOnHandByStockCardIdAndOccurredDate(
        facilityId, startTime, endTime);
    Map<UUID, List<StockCardLineItem>> lineItemByOrderableIdMap = mapStockCardLineItemByOrderableId(facilityId,
        startTime, endTime);
    return convertProductMovementDtoList(stockOnHandDtoMap, lineItemByOrderableIdMap);
  }

  private List<ProductMovementDto> convertProductMovementDtoList(
      Map<UUID, Map<LocalDate, CalculatedStockOnHandDto>> stockOnHandDtoMap,
      Map<UUID, List<StockCardLineItem>> lineItemByOrderableIdMap) {
    if (stockOnHandDtoMap.isEmpty() || lineItemByOrderableIdMap.isEmpty()) {
      return Collections.emptyList();
    }
    List<ProductMovementDto> productMovementDtoList = new ArrayList<>();
    lineItemByOrderableIdMap.forEach((orderableId, lineItems) -> {
      Map<ZonedDateTime, List<StockCardLineItemDto>> itemDtoByProcessedDateMap = itemDtoByProcessedDateMap(
          stockOnHandDtoMap, lineItemByOrderableIdMap, orderableId);
      productMovementDtoList.add(convertProductMovementDto(itemDtoByProcessedDateMap, orderableId));
    });
    return productMovementDtoList;
  }

  private ProductMovementDto convertProductMovementDto(
      Map<ZonedDateTime, List<StockCardLineItemDto>> itemDtoByProcessedDateMap, UUID orderableId) {
    //TODO productMovementDto add productCode stockOnHand lotsOnHand
    return ProductMovementDto.builder()
        .stockMovementItems(convertSiglusStockMovementItemDtos(itemDtoByProcessedDateMap))
        .id(orderableId).build();
  }

  private List<SiglusStockMovementItemDto> convertSiglusStockMovementItemDtos(
      Map<ZonedDateTime, List<StockCardLineItemDto>> itemDtoByProcessedDateMap) {
    List<SiglusStockMovementItemDto> siglusStockMovementItemDtos = new ArrayList<>();
    itemDtoByProcessedDateMap.forEach((k, v) -> {
      Optional<StockCardLineItemDto> first = v.stream().findFirst();
      if (first.isPresent()) {
        StockCardLineItemDto firstItem = first.get();
        SiglusStockMovementItemDto stockMovementItemDto = getFirstStockMovementItemDto(firstItem);
        List<LotMovementItemDto> lotMovementItems = new ArrayList<>();
        v.forEach(itemDto -> {
          stockMovementItemDto
              .setMovementQuantity(Math.addExact(stockMovementItemDto.getMovementQuantity(), itemDto.getQuantity()));
          stockMovementItemDto.setStockOnHand(
              Math.addExact(stockMovementItemDto.getStockOnHand(),
                  itemDto.getStockOnHand() == null ? 0 : itemDto.getStockOnHand()));
          lotMovementItems.add(convertLotMovementItemDto(itemDto));
        });
        stockMovementItemDto.setLotMovementItems(lotMovementItems);
        siglusStockMovementItemDtos.add(stockMovementItemDto);
      }
    });
    return siglusStockMovementItemDtos;
  }

  private Map<ZonedDateTime, List<StockCardLineItemDto>> itemDtoByProcessedDateMap(
      Map<UUID, Map<LocalDate, CalculatedStockOnHandDto>> stockOnHandDtoMap,
      Map<UUID, List<StockCardLineItem>> lineItemByOrderableIdMap, UUID orderableId) {
    Map<ZonedDateTime, List<StockCardLineItemDto>> stockCardLineItemDtoByProcessedDateMap = new HashMap<>();
    List<StockCardLineItem> stockCardLineItems = lineItemByOrderableIdMap.get(orderableId);
    stockCardLineItems.sort(Comparator.comparing(StockCardLineItem::getProcessedDate).reversed());
    stockCardLineItems.forEach(item -> {
      List<StockCardLineItemDto> stockCardLineItemDtos = stockCardLineItemDtoByProcessedDateMap
          .get(item.getProcessedDate());
      if (stockCardLineItemDtos == null) {
        stockCardLineItemDtos = new ArrayList<>();
      }
      UUID stockCardId = item.getStockCard().getId();
      CalculatedStockOnHandDto stockOnHand = stockOnHandDtoMap
          .get(stockCardId) == null ? null : stockOnHandDtoMap
          .get(stockCardId).get(item.getOccurredDate());
      if (stockOnHand != null) {
        Integer currentSoh = stockOnHand.getStockOnHand();
        if (currentSoh != null) {
          int quantity = getPositiveOrNegativeQuantity(item);
          stockOnHand.setStockOnHand(currentSoh - quantity);
          stockOnHandDtoMap.get(stockCardId).put(item.getOccurredDate(), stockOnHand);
          stockCardLineItemDtos.add(convertLotMovementDto(item, quantity, currentSoh));
        }
      }
      stockCardLineItemDtoByProcessedDateMap.put(item.getProcessedDate(), stockCardLineItemDtos);
    });
    return stockCardLineItemDtoByProcessedDateMap;
  }

  private LotMovementItemDto convertLotMovementItemDto(StockCardLineItemDto itemDto) {
    //TODO add lotNumber
    return LotMovementItemDto.builder()
        .documentNumber(itemDto.getDocumentNumber())
        .reason(itemDto.getReason() == null ? null : itemDto.getReason().getName())
        .quantity(itemDto.getQuantity())
        .stockOnHand(itemDto.getStockOnHand()).id(itemDto.getStockCard().getId()).build();
  }

  private SiglusStockMovementItemDto getFirstStockMovementItemDto(StockCardLineItemDto firstItem) {
    return SiglusStockMovementItemDto.builder()
        .type(firstItem.getReason().getReasonCategory().toString())
        .signature(firstItem.getSignature()).processedDate(firstItem.getProcessedDate())
        .occurredDate(firstItem.getOccurredDate()).movementQuantity(0).stockOnHand(0).build();
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
      UUID facilityId,
      String startTime, String endTime) {
    List<CalculatedStockOnHand> calculatedStockOnHands = calculatedStockOnHandRepository
        .findByFacilityIdAndIdStartTimeEndTime(facilityId, startTime, endTime);
    if (calculatedStockOnHands == null) {
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

  private int getPositiveOrNegativeQuantity(StockCardLineItem item) {
    if (item.isPhysicalInventory()) {
      return getPhysicalInventoryQuantity(item);
    } else if (isAdjustment(item)) {
      return getAdjustmentQuantity(item);
    } else {
      return getIssueOrEntryQuantity(item);
    }
  }

  private StockCardLineItemDto convertLotMovementDto(StockCardLineItem item, int quantity, int currentSoh) {
    StockCardLineItemReason reason;
    if (item.isPhysicalInventory()) {
      reason = determineReasonByQuantity(quantity);
    } else {
      reason = item.getReason();
    }
    return StockCardLineItemDto.builder().signature(item.getSignature()).processedDate(item.getProcessedDate())
        .occurredDate(item.getOccurredDate()).stockOnHand(currentSoh).quantity(item.getQuantity())
        .documentNumber(item.getDocumentNumber()).reason(StockCardLineItemReasonDto.newInstance(reason))
        .stockCard(item.getStockCard()).build();
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

  private int getAdjustmentQuantity(StockCardLineItem item) {
    if (item.getReason().getReasonType() == ReasonType.CREDIT) {
      return item.getQuantity();
    }
    return Math.multiplyExact(item.getQuantity(), -1);
  }

  private int getIssueOrEntryQuantity(StockCardLineItem item) {
    if (item.isPositive()) {
      return item.getQuantity();
    }
    return Math.multiplyExact(item.getQuantity(), -1);
  }

  private boolean isAdjustment(StockCardLineItem item) {
    return item.getSource() == null && item.getDestination() == null && item.getReason() != null;
  }

  private StockCardLineItemReason determineReasonByQuantity(int quantity) {
    if (quantity < 0) {
      return physicalDebit();
    } else if (quantity > 0) {
      return physicalCredit();
    } else {
      return physicalBalance();
    }
  }
}