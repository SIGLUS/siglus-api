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

import static org.siglus.siglusapi.dto.StockCardLineItemDtoComparators.byOccurredDate;
import static org.siglus.siglusapi.dto.StockCardLineItemDtoComparators.byProcessedDate;
import static org.siglus.siglusapi.dto.StockCardLineItemDtoComparators.byReasonPriority;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.commons.collections4.CollectionUtils;

import org.openlmis.stockmanagement.domain.card.StockCard;
import org.openlmis.stockmanagement.domain.card.StockCardLineItem;
import org.openlmis.stockmanagement.domain.reason.ReasonCategory;
import org.openlmis.stockmanagement.domain.reason.StockCardLineItemReason;
import org.openlmis.stockmanagement.dto.StockCardDto;
import org.openlmis.stockmanagement.dto.StockCardLineItemDto;
import org.openlmis.stockmanagement.dto.StockCardLineItemReasonDto;
import org.openlmis.stockmanagement.dto.referencedata.OrderableDto;
import org.openlmis.stockmanagement.util.StockmanagementAuthenticationHelper;
import org.siglus.siglusapi.domain.StockCardExtension;
import org.siglus.siglusapi.repository.SiglusStockCardRepository;
import org.siglus.siglusapi.repository.StockCardExtensionRepository;
import org.siglus.siglusapi.service.client.SiglusStockManagementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SiglusStockCardService {

  @Autowired
  SiglusStockCardRepository stockCardRepository;

  @Autowired
  SiglusStockManagementService stockCardStockManagementService;

  @Autowired
  private StockmanagementAuthenticationHelper authenticationHelper;

  @Autowired
  private StockCardExtensionRepository stockCardExtensionRepository;

  @Autowired
  private SiglusUnpackService unpackService;

  public StockCardDto findStockCardByOrderable(UUID orderableId) {
    UUID facilityId = authenticationHelper.getCurrentUser().getHomeFacilityId();
    List<StockCard> stockCards = stockCardRepository
        .findByFacilityIdAndOrderableId(facilityId, orderableId);

    if (stockCards.isEmpty()) {
      return null;
    }
    return findAggregateStockCards(stockCards, false);
  }

  public StockCardDto findStockCardById(UUID stockCardId) {
    StockCard stockCard = stockCardRepository.findOne(stockCardId);
    if (null == stockCard) {
      return null;
    }
    return findAggregateStockCards(Collections.singletonList(stockCard), true);
  }

  private StockCardDto findAggregateStockCards(List<StockCard> stockCards, boolean byLot) {
    List<StockCardDto> stockCardDtos = new ArrayList<>();
    List<StockCardLineItemDto> stockCardLineItemDtos = new ArrayList<>();
    Map<UUID, StockCardLineItem> lineItemsSource = new HashMap<>();
    for (StockCard stockCard : stockCards) {
      stockCard.getLineItems()
          .forEach(stockCardLineItem ->
              lineItemsSource.put(stockCardLineItem.getId(), stockCardLineItem));
      StockCardDto stockCardDto = stockCardStockManagementService.getStockCard(stockCard.getId());
      if (stockCardDto != null) {
        stockCardDtos.add(stockCardDto);
        stockCardLineItemDtos.addAll(stockCardDto.getLineItems());
      }
    }
    List<StockCardLineItemDto> calculateNewLineItemDtos =
        calculateStockOnHandByOrderable(stockCardLineItemDtos, lineItemsSource);
    addCreateInventory(calculateNewLineItemDtos, stockCards);

    return createStockCardDto(stockCardDtos, calculateNewLineItemDtos, byLot);
  }

  private List<StockCardLineItemDto> calculateStockOnHandByOrderable(
      List<StockCardLineItemDto> stockCardLineItems,
      Map<UUID, StockCardLineItem> lineItemsSource) {
    List<StockCardLineItemDto> lineItemDtos =
        stockCardLineItems.stream().peek(stockCardLineItemDto -> {
          StockCardLineItem lineitemSource = lineItemsSource.get(stockCardLineItemDto.getId());
          stockCardLineItemDto.setProcessedDate(lineitemSource.getProcessedDate());
          stockCardLineItemDto.setStockCard(lineitemSource.getStockCard());
        }).collect(Collectors.toList());
    Comparator<StockCardLineItemDto> comparator = byOccurredDate()
        .thenComparing(byProcessedDate())
        .thenComparing(byReasonPriority());
    lineItemDtos.sort(comparator);
    recalculateStockCardLineItem(lineItemDtos);
    return lineItemDtos;
  }

  private void recalculateStockCardLineItem(
      List<StockCardLineItemDto> lineItemDtos) {
    if (CollectionUtils.isEmpty(lineItemDtos)) {
      return;
    }
    int previousSoh = 0;
    for (StockCardLineItemDto lineItemDto : lineItemDtos) {
      previousSoh += lineItemDto.getQuantityWithSign();
      lineItemDto.setStockOnHand(previousSoh);
    }
  }

  private StockCardDto createStockCardDto(
      List<StockCardDto> stockCardDtos,
      List<StockCardLineItemDto> lineItemDtos,
      boolean byLot) {
    StockCardDto stockCardDto = (StockCardDto) stockCardDtos.get(0);
    OrderableDto orderableDto = stockCardDto.getOrderable();
    orderableDto.setInKit(unpackService.orderablesInKit().contains(stockCardDto.getOrderableId()));
    StockCardDto resultStockCardDto = (StockCardDto) StockCardDto.builder()
        .id(stockCardDto.getOrderableId())
        .lineItems(lineItemDtos)
        .stockOnHand(0)
        .facility(stockCardDto.getFacility())
        .program(stockCardDto.getProgram())
        .lastUpdate(stockCardDto.getLastUpdate())
        .orderable(orderableDto)
        .build();
    if (byLot) {
      resultStockCardDto.setLot(stockCardDto.getLot());
      resultStockCardDto.setStockOnHand(stockCardDto.getStockOnHand());
    }
    return resultStockCardDto;
  }

  private void addCreateInventory(List<StockCardLineItemDto> lineItemDtos,
      List<StockCard> stockCards) {
    stockCards.forEach(stockCard -> {
      StockCardExtension extension =
          stockCardExtensionRepository.findByStockCardId(stockCard.getId());
      addInventoryLineItemToDto(lineItemDtos, extension);
    });
  }

  private void addInventoryLineItemToDto(
      List<StockCardLineItemDto> lineItemDtos,
      StockCardExtension stockCardExtension) {
    StockCardLineItemDto inventoryLineItemDto = createFirstInventoryByDate(stockCardExtension);
    Optional<StockCardLineItemDto> lineItemDto = lineItemDtos.stream()
        .filter(itemDto ->
            itemDto.getOccurredDate().equals(stockCardExtension.getCreateDate())
                && null != itemDto.getStockCard()
                && itemDto.getStockCard().getId().equals(stockCardExtension.getStockCardId())
        ).findFirst();
    if (lineItemDto.isPresent()) {
      int index = lineItemDtos.indexOf(lineItemDto.get());
      lineItemDtos.add(index, inventoryLineItemDto);
    }
  }

  private StockCardLineItemDto createFirstInventoryByDate(
      StockCardExtension stockCard) {
    StockCardLineItemReason reason = StockCardLineItemReason.builder()
        .name("Inventory")
        .reasonCategory(ReasonCategory.ADJUSTMENT)
        .build();
    StockCardLineItemReasonDto resonDto = StockCardLineItemReasonDto.newInstance(reason);
    return StockCardLineItemDto.builder()
        .occurredDate(stockCard.getCreateDate())
        .stockOnHand(0)
        .stockAdjustments(new ArrayList<>())
        .reason(resonDto)
        .build();
  }
}