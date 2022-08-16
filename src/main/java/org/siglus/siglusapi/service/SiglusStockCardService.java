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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.commons.collections4.CollectionUtils;
import org.openlmis.stockmanagement.domain.card.StockCard;
import org.openlmis.stockmanagement.domain.card.StockCardLineItem;
import org.openlmis.stockmanagement.domain.event.CalculatedStockOnHand;
import org.openlmis.stockmanagement.domain.reason.ReasonCategory;
import org.openlmis.stockmanagement.domain.reason.ReasonType;
import org.openlmis.stockmanagement.domain.reason.StockCardLineItemReason;
import org.openlmis.stockmanagement.dto.StockCardDto;
import org.openlmis.stockmanagement.dto.StockCardLineItemDto;
import org.openlmis.stockmanagement.dto.StockCardLineItemReasonDto;
import org.openlmis.stockmanagement.dto.referencedata.OrderableDto;
import org.openlmis.stockmanagement.repository.CalculatedStockOnHandRepository;
import org.openlmis.stockmanagement.util.AuthenticationHelper;
import org.siglus.siglusapi.domain.StockCardExtension;
import org.siglus.siglusapi.dto.LotLocationSohDto;
import org.siglus.siglusapi.dto.StockMovementResDto;
import org.siglus.siglusapi.repository.CalculatedStocksOnHandLocationsRepository;
import org.siglus.siglusapi.repository.SiglusStockCardRepository;
import org.siglus.siglusapi.repository.StockCardExtensionRepository;
import org.siglus.siglusapi.service.client.SiglusStockManagementService;
import org.siglus.siglusapi.util.AndroidHelper;
import org.siglus.siglusapi.util.SiglusDateHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SiglusStockCardService {

  @Autowired
  SiglusStockCardRepository stockCardRepository;

  @Autowired
  SiglusStockManagementService stockCardStockManagementService;

  @Autowired
  private AuthenticationHelper authenticationHelper;

  @Autowired
  private StockCardExtensionRepository stockCardExtensionRepository;

  @Autowired
  private SiglusUnpackService unpackService;

  @Autowired
  private SiglusArchiveProductService archiveProductService;

  @Autowired
  private CalculatedStockOnHandRepository calculatedStockOnHandRepository;

  @Autowired
  private AndroidHelper androidHelper;

  @Autowired
  private SiglusDateHelper dateHelper;

  @Autowired
  private StockMovementService stockMovementService;

  @Autowired
  private CalculatedStocksOnHandLocationsRepository calculatedStocksOnHandLocationsRepository;

  private static String LOCATION_KEY = "locationCode";

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

  public StockCardDto findStockCardWithLocationById(UUID stockCardId) {
    StockCard stockCard = stockCardRepository.findOne(stockCardId);
    if (null == stockCard) {
      return null;
    }
    StockCardDto aggregateStockCards = findAggregateStockCards(Collections.singletonList(stockCard), true);
    List<LotLocationSohDto> locationSoh = calculatedStocksOnHandLocationsRepository.getLocationSoh(
        Arrays.asList(stockCard.getLotId()));
    String locationCodes =
        locationSoh.stream().map(LotLocationSohDto::getLocationCode).collect(Collectors.joining(","));
    HashMap<String, String> locationMap = new HashMap<>();
    locationMap.put(LOCATION_KEY, locationCodes);
    aggregateStockCards.setExtraData(locationMap);
    return aggregateStockCards;
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
    if (!androidHelper.isAndroid()) {
      addCreateInventory(calculateNewLineItemDtos, stockCards);
    }
    List<StockCardLineItemDto> reasonFilter = calculateNewLineItemDtos.stream().map(dto -> {
      if (dto.getSource() != null || dto.getDestination() != null) {
        dto.setReason(null);
      }
      return dto;
    }).collect(Collectors.toList());

    return createStockCardDto(stockCardDtos, reasonFilter, byLot);
  }

  private List<StockCardLineItemDto> calculateStockOnHandByOrderable(
      List<StockCardLineItemDto> lineItemDtos,
      Map<UUID, StockCardLineItem> lineItemsSource) {
    lineItemDtos.forEach(stockCardLineItemDto -> {
      StockCardLineItem lineitemSource = lineItemsSource.get(stockCardLineItemDto.getId());
      stockCardLineItemDto.setProcessedDate(lineitemSource.getProcessedDate());
      stockCardLineItemDto.setStockCard(lineitemSource.getStockCard());
    });
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
    StockCardDto stockCardDto = stockCardDtos.get(0);
    OrderableDto orderableDto = stockCardDto.getOrderable();
    orderableDto.setInKit(unpackService.orderablesInKit().contains(stockCardDto.getOrderableId()));
    orderableDto.setArchived(archiveProductService.isArchived(stockCardDto.getId()));
    StockCardDto resultStockCardDto = StockCardDto.builder()
        .id(stockCardDto.getId())
        .lineItems(lineItemDtos)
        .stockOnHand(getStockOnHand(stockCardDtos))
        .facility(stockCardDto.getFacility())
        .program(stockCardDto.getProgram())
        .lastUpdate(stockCardDto.getLastUpdate())
        .orderable(orderableDto)
        .build();
    if (byLot) {
      resultStockCardDto.setLot(stockCardDto.getLot());
    }
    return resultStockCardDto;
  }

  private Integer getStockOnHand(List<StockCardDto> stockCardDtos) {
    int stockOnHand = 0;
    for (StockCardDto stockCardDto : stockCardDtos) {
      Optional<CalculatedStockOnHand> calculatedStockOnHandOptional =
          calculatedStockOnHandRepository
              .findFirstByStockCardIdAndOccurredDateLessThanEqualOrderByOccurredDateDesc(
                  stockCardDto.getId(), dateHelper.getCurrentDate());
      stockOnHand += calculatedStockOnHandOptional.isPresent()
          ? calculatedStockOnHandOptional.get().getStockOnHand() : 0;

    }
    return stockOnHand;
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
        .name("inventário")
        .reasonType(ReasonType.BALANCE_ADJUSTMENT)
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

  public List<StockMovementResDto> getProductMovements(UUID facilityId, UUID orderableId,
      LocalDate startTime, LocalDate endTime) {
    HashSet<UUID> orderableIdsSet = new HashSet<>();
    if (orderableId != null) {
      orderableIdsSet.add(orderableId);
    }
    List<StockMovementResDto> productMovements =
        stockMovementService.getProductMovements(orderableIdsSet, facilityId, startTime, endTime);
    return productMovements;
  }
}