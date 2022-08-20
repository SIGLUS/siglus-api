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

import static java.util.stream.Collectors.toMap;
import static org.openlmis.stockmanagement.i18n.MessageKeys.ERROR_EVENT_DEBIT_QUANTITY_EXCEED_SOH;
import static org.siglus.siglusapi.constant.FieldConstants.SEPARATOR;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.transaction.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.openlmis.stockmanagement.domain.BaseEntity;
import org.openlmis.stockmanagement.domain.card.StockCard;
import org.openlmis.stockmanagement.domain.card.StockCardLineItem;
import org.openlmis.stockmanagement.domain.event.CalculatedStockOnHand;
import org.openlmis.stockmanagement.domain.reason.StockCardLineItemReason;
import org.openlmis.stockmanagement.dto.StockEventDto;
import org.openlmis.stockmanagement.dto.StockEventLineItemDto;
import org.openlmis.stockmanagement.dto.referencedata.OrderableDto;
import org.openlmis.stockmanagement.exception.ValidationMessageException;
import org.openlmis.stockmanagement.repository.CalculatedStockOnHandRepository;
import org.openlmis.stockmanagement.repository.StockCardLineItemRepository;
import org.openlmis.stockmanagement.service.referencedata.OrderableReferenceDataService;
import org.openlmis.stockmanagement.util.Message;

import org.siglus.siglusapi.domain.CalculatedStockOnHandByLocation;
import org.siglus.siglusapi.domain.StockCardLineItemExtension;
import org.siglus.siglusapi.domain.StockCardLocationMovementLineItem;
import org.siglus.siglusapi.repository.CalculatedStockOnHandByLocationRepository;
import org.siglus.siglusapi.repository.SiglusStockCardRepository;
import org.siglus.siglusapi.repository.StockCardLineItemExtensionRepository;
import org.siglus.siglusapi.repository.StockCardLocationMovementLineItemRepository;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class CalculatedStocksOnHandLocationsService {
  private final CalculatedStockOnHandByLocationRepository calculatedStockOnHandByLocationRepository;
  private final CalculatedStockOnHandRepository calculatedStocksOnHandRepository;
  private final SiglusStockCardRepository siglusStockCardRepository;
  private final StockCardLineItemExtensionRepository stockCardLineItemExtensionRepository;
  private final StockCardLocationMovementLineItemRepository locationMovementRepository;
  private final OrderableReferenceDataService orderableService;
  private final StockCardLineItemRepository stockCardLineItemRepository;

  public void calculateStockOnHandByLocation(StockEventDto eventDto) {
    if (eventDto.isPhysicalInventory()) {
      calculateLocationStockOnHandForPhysicalInventory(eventDto);
    }
  }

  @Transactional
  public void calculateLocationStockOnHandForPhysicalInventory(StockEventDto eventDto) {
    UUID facilityId = eventDto.getFacilityId();
    List<StockEventLineItemDto> lineItemDtos = eventDto.getLineItems();
    List<StockCard> stockCards = siglusStockCardRepository
            .findByFacilityIdAndLineItems(facilityId, lineItemDtos);

    Map<String, StockCard> uniKeyToStockCard = stockCards.stream()
        .collect(Collectors.toMap(this::getUniqueKey, Function.identity()));

    List<StockCardLineItem> allLineItems = stockCardLineItemRepository.findAllByStockCardIn(stockCards);
    Map<UUID, List<StockCardLineItem>> stockCardIdToLineItems = allLineItems.stream()
            .collect(Collectors.groupingBy(StockCardLineItem::getStockCardId));
    Map<StockCard, List<StockCardLineItem>> stockCardToLineItems =
        mapStockCardsWithLineItems(uniKeyToStockCard, lineItemDtos, stockCardIdToLineItems);
    Map<UUID, StockCardLineItemExtension> lineItemIdToExtension = buildExtensionMap(allLineItems);

    LocalDate occurredDate = lineItemDtos.get(0).getOccurredDate();
    Set<UUID> stockCardIds = stockCardToLineItems.keySet().stream().map(BaseEntity::getId).collect(Collectors.toSet());
    Map<String, Integer> stockCardIdAndLocationCodeToPreviousStockOnHandMap =
            this.getPreviousStockOnHandMap(stockCardIds, occurredDate);

    deleteFollowingStockOnHands(allLineItems, lineItemIdToExtension, occurredDate);

    List<CalculatedStockOnHandByLocation> toSaveList = new ArrayList<>();
    stockCardToLineItems.forEach((key, value) -> {
      value.sort(StockCard.getLineItemsComparator());
      value.stream().findFirst()
          .ifPresent(item -> recalculateLocationStockOnHand(toSaveList, lineItemIdToExtension, item,
              stockCardIdAndLocationCodeToPreviousStockOnHandMap
          ));
    });

    saveAll(toSaveList);
  }

  private void saveAll(List<CalculatedStockOnHandByLocation> toSaveList) {
    if (CollectionUtils.isEmpty(toSaveList)) {
      return;
    }
    // must have same OccurredDate
    LocalDate date = toSaveList.get(0).getOccurreddate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    List<UUID> stockCardIds = toSaveList.stream()
            .map(CalculatedStockOnHandByLocation::getStockCardId).collect(Collectors.toList());
    Map<UUID, UUID> stockCardIdToId = calculatedStocksOnHandRepository
            .findByOccurredDateAndStockCardIdIn(date, stockCardIds)
            // TODO soh.getStockCard().getId()
            .stream().collect(Collectors.toMap(soh -> soh.getStockCard().getId(), CalculatedStockOnHand::getId));
    toSaveList.forEach(location ->
            location.setCalculatedStocksOnHandId(stockCardIdToId.get(location.getStockCardId())));
    log.info(String.format("save CalculatedStocksOnHandLocations %s", toSaveList.size()));
    calculatedStockOnHandByLocationRepository.save(toSaveList);
  }

  private Map<UUID, StockCardLineItemExtension> buildExtensionMap(List<StockCardLineItem> lineItems) {
    List<UUID> ids = lineItems.stream().map(StockCardLineItem::getId).collect(Collectors.toList());
    List<StockCardLineItemExtension> extensions = stockCardLineItemExtensionRepository
            .findAllByStockCardLineItemIdIn(ids);
    return extensions.stream().collect(Collectors.toMap(
        StockCardLineItemExtension::getStockCardLineItemId, Function.identity()));
  }


  private void deleteFollowingStockOnHands(List<StockCardLineItem> allLineItems,
                       Map<UUID, StockCardLineItemExtension> lineItemIdToExtension,
                       LocalDate occurredDate) {
    ZoneId zoneId = ZoneId.systemDefault();
    Date date = Date.from(occurredDate.atStartOfDay(zoneId).toInstant());
    List<CalculatedStockOnHandByLocation> toDelete = calculatedStockOnHandByLocationRepository
            .getFollowingStockOnHands(allLineItems, lineItemIdToExtension, date);
    calculatedStockOnHandByLocationRepository.delete(toDelete);
  }

  public void recalculateLocationStockOnHand(List<CalculatedStockOnHandByLocation> toSaveList,
                        Map<UUID, StockCardLineItemExtension> lineItemIdToExtension,
                        StockCardLineItem lineItem,
                        Map<String, Integer> stockCardIdAndLocationCodeToPreviousStockOnHandMap) {
    List<CalculatedStockOnHandByLocation> toSaveForThisLocation = new ArrayList<>();

    StockCardLineItemExtension extension = lineItemIdToExtension.get(lineItem.getId());
    StockCard stockCard = lineItem.getStockCard();
    Integer previousSoh = stockCardIdAndLocationCodeToPreviousStockOnHandMap.get(stockCard.getId()
            + extension.getLocationCode());
    previousSoh = previousSoh == null ? 0 : previousSoh;

    List<StockCardLineItem> followingLineItems = getFollowingLineItems(lineItemIdToExtension, stockCard, lineItem);

    int lineItemsAtTheSameDay = countLineItemsBefore(followingLineItems, lineItem);
    followingLineItems.add(lineItemsAtTheSameDay, lineItem);

    // handle location movement
    List<StockCardLocationMovementLineItem> movements = getFollowingLocationMovements(
            stockCard.getId(), extension.getLocationCode(), lineItem.getOccurredDate());
    if (CollectionUtils.isNotEmpty(movements)) {
      List<StockCardLineItem> followingMovementLineItems = movements.stream()
              .map(movement -> convertMovementToStockCardLineItem(movement, extension.getLocationCode()))
              .collect(Collectors.toList());
      followingLineItems.addAll(followingMovementLineItems);
      followingLineItems = followingLineItems.stream()
              .sorted(Comparator.comparing(StockCardLineItem::getOccurredDate)).collect(Collectors.toList());
    }

    LocalDate previousOccurredDate = lineItem.getOccurredDate();
    StockCardLineItem previousItem = null;
    for (StockCardLineItem item : followingLineItems) {
      Integer calculatedStockOnHand = calculateStockOnHand(item, previousSoh);
      LocalDate itemOccurredDate = item.getOccurredDate();
      if (!itemOccurredDate.equals(previousOccurredDate)) {
        toSaveForThisLocation.add(buildSoh(lineItemIdToExtension, previousItem, previousSoh, stockCard));
      }
      previousSoh = calculatedStockOnHand;
      previousItem = item;
      previousOccurredDate = itemOccurredDate;
    }
    toSaveForThisLocation.add(buildSoh(lineItemIdToExtension, previousItem, previousSoh, stockCard));

    toSaveList.addAll(toSaveForThisLocation);
  }

  private StockCardLineItem convertMovementToStockCardLineItem(StockCardLocationMovementLineItem movement,
                                                               String locationCode) {
    StockCardLineItem lineItem = new StockCardLineItem();
    lineItem.setQuantity(movement.getQuantity());
    lineItem.setOccurredDate(movement.getOccurredDate());
    if (locationCode.equals(movement.getSrcLocationCode())) {
      // credit means positive
      lineItem.setReason(StockCardLineItemReason.physicalDebit());
    } else {
      lineItem.setReason(StockCardLineItemReason.physicalCredit());
    }
    return lineItem;
  }

  // get movement src/des location match after occurredDate
  private List<StockCardLocationMovementLineItem> getFollowingLocationMovements(
          UUID stockCardId, String locationCode, LocalDate occurredDate) {
    return locationMovementRepository.findByStockCardId(stockCardId).stream()
            .filter(movement -> !occurredDate.isAfter(movement.getOccurredDate()))
            .filter(movement -> locationCode.equals(movement.getSrcLocationCode())
                    || locationCode.equals(movement.getDestLocationCode()))
            .collect(Collectors.toList());
  }


  private CalculatedStockOnHandByLocation buildSoh(
      Map<UUID, StockCardLineItemExtension> lineItemIdToExtension,
      StockCardLineItem lineItem,
      Integer stockOnHand,
      StockCard stockCard) {
    // Extension should find all for this stockCard, otherwise not found
    StockCardLineItemExtension extension = lineItemIdToExtension.get(lineItem.getId());
    ZoneId zoneId = ZoneId.systemDefault();
    return CalculatedStockOnHandByLocation.builder()
        .stockCardId(stockCard.getId())
        .occurreddate(Date.from(lineItem.getOccurredDate().atStartOfDay(zoneId).toInstant()))
        .stockonhand(stockOnHand)
        .locationCode(extension.getLocationCode())
        .area(extension.getArea())
        .build();
  }

  private Integer calculateStockOnHand(StockCardLineItem item, int prevSoH) {
    int quantity = item.isPhysicalInventory()
        ? item.getQuantity()
        : prevSoH + item.getQuantityWithSign();

    if (quantity < 0) {
      throwQuantityExceedException(item, prevSoH);
    }

    return quantity;
  }

  private int countLineItemsBefore(List<StockCardLineItem> followingLineItems,
                   StockCardLineItem lineItem) {
    return (int) followingLineItems.stream()
        .filter(i -> !i.getOccurredDate().isAfter(lineItem.getOccurredDate())
            && !i.getProcessedDate().isAfter(lineItem.getProcessedDate())).count();
  }

  private List<StockCardLineItem> getFollowingLineItems(
          Map<UUID, StockCardLineItemExtension> lineItemIdToExtension,
          StockCard stockCard,
          StockCardLineItem lineItem) {
    StockCardLineItemExtension extension = lineItemIdToExtension.get(lineItem.getId());
    return stockCard.getLineItems()
            .stream()
            .filter(item -> !item.getOccurredDate().isBefore(lineItem.getOccurredDate())
                    && item.getId() != lineItem.getId())
            // TODO item locationCode has to get one by one
            .filter(item -> Objects.equals(getLocationCode(item), extension.getLocationCode()))
            .sorted(StockCard.getLineItemsComparator())
            .collect(Collectors.toList());
  }

  private String getLocationCode(StockCardLineItem lineItem) {
    Optional<StockCardLineItemExtension> extension = stockCardLineItemExtensionRepository
            .findByStockCardLineItemId(lineItem.getId());
    return extension.map(StockCardLineItemExtension::getLocationCode).orElse(null);
  }

  private Map<String, Integer> getPreviousStockOnHandMap(Set<UUID> stockCardIds,
                               LocalDate occurredDate) {
    return calculatedStockOnHandByLocationRepository
        .findPreviousLocationStockOnHands(stockCardIds, occurredDate).stream()
        .collect(toMap(calculatedStockOnHand ->
                        calculatedStockOnHand.getStockCardId().toString() + calculatedStockOnHand.getLocationCode(),
            CalculatedStockOnHandByLocation::getStockonhand, (stockOnHand1, stockOnHand2) -> stockOnHand1));
  }

  private Map<StockCard, List<StockCardLineItem>> mapStockCardsWithLineItems(
      Map<String, StockCard> uniKeyToStockCard,
      List<StockEventLineItemDto> lineItemDtos,
      Map<UUID, List<StockCardLineItem>> stockCardIdToLineItems) {
    Map<StockCard, List<StockCardLineItem>> stockCardToLineItems = new HashMap<>();
    lineItemDtos.forEach(lineItemDto -> {
      StockCard stockCard = uniKeyToStockCard.get(getUniqueKey(lineItemDto));
      StockCardLineItem lineItem = stockCardIdToLineItems.get(stockCard.getId()).stream()
          .filter(item -> item.getOccurredDate().equals(lineItemDto.getOccurredDate()))
          .sorted(Comparator.comparing(StockCardLineItem::getProcessedDate).reversed())
          .findFirst()
          .orElseThrow(() -> new IllegalArgumentException("can't find target stock card line item"));
      if (stockCardToLineItems.containsKey(stockCard)) {
        stockCardToLineItems.get(stockCard).add(lineItem);
      } else {
        stockCardToLineItems.put(stockCard, Arrays.asList(lineItem));
      }
    });
    return stockCardToLineItems;
  }

  private String getString(Object o) {
    return o == null ? "" : o.toString();
  }

  private String getLocationKey(CalculatedStockOnHandByLocation locations) {
    return getString(locations.getStockCardId())
        + SEPARATOR
        + getString(locations.getLocationCode());
  }

  private String getUniqueKey(StockCard stockCard) {
    return getString(stockCard.getOrderableId())
        + SEPARATOR
        + getString(stockCard.getLotId());
  }

  private String getUniqueKey(StockEventLineItemDto lineItem) {
    return getString(lineItem.getOrderableId())
        + SEPARATOR
        + getString(lineItem.getLotId());
  }

  private String getUniqueKey(CalculatedStockOnHand calculatedStockOnHand) {
    return getString(calculatedStockOnHand.getStockCard().getId())
        + SEPARATOR
        + getString(calculatedStockOnHand.getOccurredDate());
  }

  private String getUniqueKey(CalculatedStockOnHandByLocation calculatedStocksOnHandLocation) {
    return getString(calculatedStocksOnHandLocation.getStockCardId())
        + SEPARATOR
        + getString(calculatedStocksOnHandLocation.getOccurreddate());
  }

  private void throwQuantityExceedException(StockCardLineItem item, int prevSoH) {
    OrderableDto orderable = orderableService.findOne(item.getStockCard().getOrderableId());
    String code = (orderable != null) ? orderable.getProductCode() : "";
    throw new ValidationMessageException(
        new Message(ERROR_EVENT_DEBIT_QUANTITY_EXCEED_SOH,
            item.getOccurredDate(), code, prevSoH, item.getQuantity()));
  }

}
