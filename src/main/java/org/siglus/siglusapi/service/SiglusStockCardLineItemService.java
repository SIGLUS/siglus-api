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
import org.openlmis.stockmanagement.domain.reason.ReasonType;
import org.openlmis.stockmanagement.domain.sourcedestination.Organization;
import org.openlmis.stockmanagement.dto.CalculatedStockOnHandDto;
import org.openlmis.stockmanagement.dto.StockCardLineItemDto;
import org.openlmis.stockmanagement.dto.StockCardLineItemReasonDto;
import org.openlmis.stockmanagement.repository.CalculatedStockOnHandRepository;
import org.openlmis.stockmanagement.repository.OrganizationRepository;
import org.siglus.siglusapi.domain.StockEventExtension;
import org.siglus.siglusapi.dto.LotMovementItemDto;
import org.siglus.siglusapi.dto.SiglusLotDto;
import org.siglus.siglusapi.dto.SiglusStockMovementItemDto;
import org.siglus.siglusapi.repository.SiglusStockCardLineItemRepository;
import org.siglus.siglusapi.service.android.SiglusMeService;
import org.siglus.siglusapi.service.android.SiglusMeService.MovementType;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@SuppressWarnings("PMD.TooManyMethods")
public class SiglusStockCardLineItemService {

  private static final String INVENTORY_NEGATIVE = "INVENTORY_NEGATIVE";
  private static final String INVENTORY_POSITIVE = "INVENTORY_POSITIVE";
  private static final String INVENTORY = "INVENTORY";
  private static final String UNPACK_KIT = "Unpack Kit";

  private Map<UUID, String> organizationNameToId;

  private final SiglusStockCardLineItemRepository stockCardLineItemRepository;

  private final CalculatedStockOnHandRepository calculatedStockOnHandRepository;

  private final OrganizationRepository organizationRepository;

  private final SiglusStockEventExtensionService requestQuantityService;

  public Map<UUID, List<SiglusStockMovementItemDto>> getStockMovementByOrderableId(UUID facilityId, String startTime,
      String endTime, Map<UUID, SiglusLotDto> siglusLotDtoByLotId) {
    organizationNameToId = mapOrganizationNameToId(facilityId);
    Map<UUID, Map<LocalDate, CalculatedStockOnHandDto>> stockOnHandDtoMap = mapStockOnHandByStockCardIdAndOccurredDate(
        facilityId, startTime, endTime);
    Map<UUID, List<StockCardLineItem>> lineItemByOrderableIdMap = mapStockCardLineItemByOrderableId(facilityId,
        startTime, endTime);
    return getStockMovementItemDtosMap(stockOnHandDtoMap, lineItemByOrderableIdMap, siglusLotDtoByLotId);
  }

  private Map<UUID, List<SiglusStockMovementItemDto>> getStockMovementItemDtosMap(
      Map<UUID, Map<LocalDate, CalculatedStockOnHandDto>> stockOnHandDtoMap,
      Map<UUID, List<StockCardLineItem>> lineItemByOrderableIdMap, Map<UUID, SiglusLotDto> siglusLotDtoByLotId) {
    if (stockOnHandDtoMap.isEmpty() || lineItemByOrderableIdMap.isEmpty()) {
      return Collections.emptyMap();
    }
    Map<UUID, List<SiglusStockMovementItemDto>> stockMovementItemDtosMap = new HashMap<>();
    lineItemByOrderableIdMap.forEach((orderableId, lineItems) -> {
      Map<ZonedDateTime, List<StockCardLineItemDto>> itemDtoByProcessedDateMap = itemDtoByProcessedDateMap(
          stockOnHandDtoMap, lineItemByOrderableIdMap, orderableId);
      stockMovementItemDtosMap.put(orderableId,
          convertSiglusStockMovementItemDtos(itemDtoByProcessedDateMap, siglusLotDtoByLotId));
    });
    return stockMovementItemDtosMap;
  }

  private List<SiglusStockMovementItemDto> convertSiglusStockMovementItemDtos(
      Map<ZonedDateTime, List<StockCardLineItemDto>> itemDtoByProcessedDateMap,
      Map<UUID, SiglusLotDto> siglusLotDtoByLotId) {
    List<SiglusStockMovementItemDto> siglusStockMovementItemDtos = new ArrayList<>();
    itemDtoByProcessedDateMap.forEach((processedDate, itemDtos) -> {
      Optional<StockCardLineItemDto> first = itemDtos.stream().findFirst();
      if (first.isPresent()) {
        siglusStockMovementItemDtos
            .add(convertStockMovementItemDto(first.get(), itemDtos, siglusLotDtoByLotId));
      }
    });
    return siglusStockMovementItemDtos;
  }

  private SiglusStockMovementItemDto convertStockMovementItemDto(StockCardLineItemDto firstItemDto,
      List<StockCardLineItemDto> itemDtos, Map<UUID, SiglusLotDto> siglusLotDtoByLotId) {
    SiglusStockMovementItemDto stockMovementItemDto = getFirstStockMovementItemDto(firstItemDto);
    if (firstItemDto.getStockCard().getLotId() == null) {
      convertStockMovementItemDtoWhenNoLot(stockMovementItemDto, firstItemDto);
    } else {
      List<LotMovementItemDto> lotMovementItems = new ArrayList<>();
      itemDtos.forEach(itemDto -> {
        stockMovementItemDto
            .setMovementQuantity(Math.addExact(stockMovementItemDto.getMovementQuantity(), itemDto.getQuantity()));
        stockMovementItemDto
            .setStockOnHand(Math.addExact(stockMovementItemDto.getStockOnHand(), itemDto.getStockOnHand()));
        lotMovementItems.add(convertLotMovementItemDto(itemDto, siglusLotDtoByLotId));
      });
      stockMovementItemDto.setLotMovementItems(lotMovementItems);
    }
    return stockMovementItemDto;
  }

  private void convertStockMovementItemDtoWhenNoLot(SiglusStockMovementItemDto stockMovementItemDto,
      StockCardLineItemDto itemDto) {
    stockMovementItemDto.setMovementQuantity(itemDto.getQuantity());
    stockMovementItemDto.setStockOnHand(itemDto.getStockOnHand());
    stockMovementItemDto.setDocumentNumber(itemDto.getDocumentNumber());
    if (itemDto.getReason() != null && UNPACK_KIT.equals(itemDto.getReason().getName())) {
      stockMovementItemDto.setType(MovementType.UNPACK_KIT.name());
    } else {
      stockMovementItemDto.setReason(itemDto.getReason().getName());
      stockMovementItemDto.setType(itemDto.getReason().getType());
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
    int quantity = item.getQuantityWithSign();
    stockOnHand.setStockOnHand(currentSoh - quantity);
    stockOnHandDtoMap.get(stockCardId).put(item.getOccurredDate(), stockOnHand);
    stockCardLineItemDtos.add(convertStockCardLineItemDto(item, quantity, currentSoh));
    stockCardLineItemDtoByProcessedDateMap.put(item.getProcessedDate(), stockCardLineItemDtos);
  }

  private LotMovementItemDto convertLotMovementItemDto(StockCardLineItemDto itemDto,
      Map<UUID, SiglusLotDto> siglusLotDtoByLotId) {
    return LotMovementItemDto.builder()
        .lotCode(siglusLotDtoByLotId.get(itemDto.getStockCard().getLotId()).getLotCode())
        .documentNumber(itemDto.getDocumentNumber())
        .reason(itemDto.getReason() == null ? null : itemDto.getReason().getName())
        .quantity(itemDto.getQuantity())
        .stockOnHand(itemDto.getStockOnHand()).build();
  }

  private SiglusStockMovementItemDto getFirstStockMovementItemDto(StockCardLineItemDto firstItem) {
    return SiglusStockMovementItemDto.builder()
        .requested(getRequested(firstItem))
        .type(firstItem.getReason().getType()).signature(firstItem.getSignature())
        .processedDate(firstItem.getProcessedDate().toInstant().toEpochMilli())
        .documentNumber(firstItem.getDocumentNumber())
        .occurredDate(firstItem.getOccurredDate()).movementQuantity(0).stockOnHand(0).build();
  }

  private Integer getRequested(StockCardLineItemDto item) {
    StockEventExtension stockEventExtension = requestQuantityService
        .findOneByOrderableIdAndStockeventId(item.getStockCard().getOrderableId(),
            item.getStockCard().getOriginEvent().getId());
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

  private boolean isIssue(StockCardLineItem item) {
    return (item.getDestination() != null) || (item.getReason() != null
        && item.getReason().getReasonType() == ReasonType.DEBIT);
  }

  private boolean isAdjustment(StockCardLineItem item) {
    return item.getSource() == null && item.getDestination() == null && item.getReason() != null;
  }

  private StockCardLineItemDto convertStockCardLineItemDto(StockCardLineItem item, int quantity, int currentSoh) {
    return StockCardLineItemDto.builder().signature(item.getSignature()).processedDate(item.getProcessedDate())
        .occurredDate(item.getOccurredDate()).stockOnHand(currentSoh).quantity(quantity)
        .documentNumber(item.getDocumentNumber())
        .reason(getStockCardLineItemReasonDto(item))
        .stockCard(item.getStockCard()).build();
  }

  private StockCardLineItemReasonDto getStockCardLineItemReasonDto(StockCardLineItem item) {
    StockCardLineItemReasonDto reasonDto = new StockCardLineItemReasonDto();
    String reason = null;
    String type = null;
    if (item.isPhysicalInventory()) {
      type = MovementType.PHYSICAL_INVENTORY.name();
      if (item.getQuantity() < 0) {
        reason = INVENTORY_NEGATIVE;
      } else if (item.getQuantity() > 0) {
        reason = INVENTORY_POSITIVE;
      } else {
        reason = INVENTORY;
      }
    } else if (isAdjustment(item)) {
      type = MovementType.ADJUSTMENT.name();
      if (UNPACK_KIT.equals(item.getReason().getName())) {
        reason = item.getReason().getName();
      } else {
        reason = SiglusMeService.AdjustmentReason.findByValue(item.getReason().getName());
      }
    } else if (item.isPositive()) {
      type = MovementType.RECEIVE.name();
      reason = SiglusMeService.Source.findByValue(organizationNameToId.get(item.getSource().getReferenceId()));
    } else if (isIssue(item)) {
      type = MovementType.ISSUE.name();
      reason = SiglusMeService.Destination
          .findByValue(organizationNameToId.get(item.getDestination().getReferenceId()));
    }
    reasonDto.setType(type);
    reasonDto.setName(reason);
    return reasonDto;
  }

  private Map<UUID, String> mapOrganizationNameToId(UUID facilityId) {
    return organizationRepository.findAll().stream()
        .collect(Collectors.toMap(Organization::getId, Organization::getName));
  }
}