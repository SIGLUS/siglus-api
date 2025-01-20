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

import static com.google.common.collect.Lists.newArrayList;
import static java.util.stream.Collectors.toMap;
import static org.siglus.siglusapi.i18n.MessageKeys.SHIPMENT_LINE_ITEMS_INVALID;
import static org.siglus.siglusapi.i18n.MessageKeys.SHIPMENT_ORDER_STATUS_INVALID;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.validation.ValidationException;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openlmis.fulfillment.domain.Order;
import org.openlmis.fulfillment.domain.OrderLineItem;
import org.openlmis.fulfillment.domain.OrderStatus;
import org.openlmis.fulfillment.domain.ShipmentDraft;
import org.openlmis.fulfillment.domain.ShipmentLineItem.Importer;
import org.openlmis.fulfillment.repository.OrderRepository;
import org.openlmis.fulfillment.repository.ShipmentDraftRepository;
import org.openlmis.fulfillment.web.shipment.LocationDto;
import org.openlmis.fulfillment.web.shipment.ShipmentLineItemDto;
import org.openlmis.fulfillment.web.shipmentdraft.ShipmentDraftController;
import org.openlmis.fulfillment.web.shipmentdraft.ShipmentDraftDto;
import org.openlmis.fulfillment.web.util.ObjectReferenceDto;
import org.openlmis.fulfillment.web.util.OrderDto;
import org.openlmis.fulfillment.web.util.OrderLineItemDto;
import org.openlmis.fulfillment.web.util.OrderObjectReferenceDto;
import org.openlmis.fulfillment.web.util.VersionObjectReferenceDto;
import org.openlmis.stockmanagement.domain.card.StockCard;
import org.siglus.siglusapi.domain.OrderLineItemExtension;
import org.siglus.siglusapi.domain.ShipmentDraftLineItemsExtension;
import org.siglus.siglusapi.dto.LotDto;
import org.siglus.siglusapi.dto.Message;
import org.siglus.siglusapi.dto.SiglusOrderDto;
import org.siglus.siglusapi.dto.SiglusShipmentDraftDto;
import org.siglus.siglusapi.dto.SiglusShipmentDraftLineItemDto;
import org.siglus.siglusapi.exception.ValidationMessageException;
import org.siglus.siglusapi.repository.OrderLineItemExtensionRepository;
import org.siglus.siglusapi.repository.OrderLineItemRepository;
import org.siglus.siglusapi.repository.ShipmentDraftLineItemsExtensionRepository;
import org.siglus.siglusapi.repository.ShipmentDraftLineItemsRepository;
import org.siglus.siglusapi.repository.SiglusStockCardRepository;
import org.siglus.siglusapi.repository.dto.OrderableVersionDto;
import org.siglus.siglusapi.repository.dto.StockCardReservedDto;
import org.siglus.siglusapi.repository.dto.StockCardStockDto;
import org.siglus.siglusapi.service.client.SiglusShipmentDraftFulfillmentService;
import org.siglus.siglusapi.util.FacilityConfigHelper;
import org.siglus.siglusapi.util.FormatHelper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

@Service
@Slf4j
@AllArgsConstructor
@NoArgsConstructor
public class SiglusShipmentDraftService {
  @Autowired
  private SiglusShipmentDraftFulfillmentService siglusShipmentDraftFulfillmentService;
  @Autowired
  private ShipmentDraftController draftController;
  @Autowired
  private OrderLineItemExtensionRepository lineItemExtensionRepository;
  @Autowired
  private OrderLineItemRepository orderLineItemRepository;
  @Autowired
  private OrderRepository orderRepository;
  @Autowired
  private SiglusOrderService siglusOrderService;
  @Autowired
  private ShipmentDraftLineItemsExtensionRepository shipmentDraftLineItemsExtensionRepository;
  @Autowired
  private ShipmentDraftLineItemsRepository shipmentDraftLineItemsRepository;
  @Autowired
  private SiglusStockCardSummariesService siglusStockCardSummariesService;
  @Autowired
  private FacilityConfigHelper facilityConfigHelper;
  @Autowired
  private SiglusStockCardRepository siglusStockCardRepository;
  @Autowired
  private SiglusStockCardService siglusStockCardService;
  @Autowired
  private ShipmentDraftRepository shipmentDraftRepository;
  @Autowired
  private SiglusOrderableService siglusOrderableService;
  @Autowired
  private SiglusLotService siglusLotService;

  @Transactional
  public SiglusShipmentDraftDto createShipmentDraft(UUID orderId) {
    SiglusOrderDto orderDto = siglusOrderService.searchOrderByIdWithoutProducts(orderId);
    if (!orderDto.canStartFulfillment()) {
      throw new IllegalArgumentException("order id : " + orderId + " can't start fulfillment");
    }
    ShipmentDraftDto draftDto = createShipmentDraftForOrder(orderDto.getOrder());
    ShipmentDraftDto savedDraftDto = draftController.createShipmentDraft(draftDto);
    boolean hasLocation =
        facilityConfigHelper.isLocationManagementEnabled(orderDto.getOrder().getSupplyingFacility().getId());
    if (hasLocation) {
      fillLocationForCreatedShipmentDraft(savedDraftDto, draftDto);
      saveShipmentDraftLineItemsExtension(savedDraftDto);
    }
    return convertFromShipmentDraftDto(savedDraftDto);
  }

  @Transactional
  public ShipmentDraftDto createShipmentDraft(ShipmentDraftDto draftDto) {
    checkStockOnHandQuantity(null, draftDto);
    return draftController.createShipmentDraft(draftDto);
  }

  @Transactional
  public ShipmentDraftDto createShipmentDraftWithoutCheck(ShipmentDraftDto draftDto) {
    return draftController.createShipmentDraft(draftDto);
  }

  @Transactional
  public ShipmentDraftDto updateShipmentDraft(UUID id, ShipmentDraftDto draftDto) {
    UUID orderId = draftDto.getOrder().getId();
    Order order = orderRepository.findOne(orderId);
    if (order != null && !OrderStatus.FULFILLING.equals(order.getStatus())) {
      throw new ValidationMessageException(SHIPMENT_ORDER_STATUS_INVALID);
    }
    checkStockOnHandQuantity(id, draftDto);
    updateOrderLineItemsWithExtension(draftDto);
    boolean hasLocation =
        facilityConfigHelper.isLocationManagementEnabled(draftDto.getOrder().getSupplyingFacility().getId());
    if (hasLocation) {
      return updateShipmentDraftByLocation(id, draftDto);
    }
    return draftController.updateShipmentDraft(id, draftDto);
  }

  @Transactional
  public void deleteShipmentDraft(UUID id) {
    Order draftOrder = getDraftOrder(id);
    deleteOrderLineItemAndInitialedExtension(draftOrder);
    boolean hasLocation = facilityConfigHelper.isLocationManagementEnabled(draftOrder.getSupplyingFacilityId());
    if (hasLocation) {
      deleteShipmentDraftLineItemsExtension(id);
    }
    draftController.deleteShipmentDraft(id);
  }

  public SiglusShipmentDraftDto getShipmentDraft(UUID id) {
    ShipmentDraftDto shipmentDraftDto = siglusShipmentDraftFulfillmentService.searchShipmentDraft(id);
    return convertFromShipmentDraftDto(shipmentDraftDto);
  }

  public List<SiglusShipmentDraftDto> getShipmentDraftByOrderId(UUID orderId) {
    Page<ShipmentDraftDto> shipmentDraftDto = siglusShipmentDraftFulfillmentService.getShipmentDraftByOrderId(orderId);
    if (CollectionUtils.isEmpty(shipmentDraftDto.getContent())) {
      return Collections.emptyList();
    }
    return Collections.singletonList(convertFromShipmentDraftDto(shipmentDraftDto.getContent().get(0)));
  }

  private ShipmentDraftDto updateShipmentDraftByLocation(UUID shipmentDraftId, ShipmentDraftDto draftDto) {
    Map<UUID, ShipmentLineItemDto> originMap = new HashMap<>();
    Multimap<String, ShipmentLineItemDto> addedMap = ArrayListMultimap.create();
    draftDto.lineItems().forEach(shipmentLineItemDto -> {
      if (shipmentLineItemDto.getLotId() == null && shipmentLineItemDto.getId() != null) {
        shipmentLineItemDto.setId(null);
      }
      if (shipmentLineItemDto.getId() != null) {
        originMap.put(shipmentLineItemDto.getId(), shipmentLineItemDto);
      } else {
        String uniqueKey = buildUniqueKey(shipmentLineItemDto);
        addedMap.put(uniqueKey, shipmentLineItemDto);
      }
    });
    deleteShipmentDraftLineItemsExtension(shipmentDraftId);
    ShipmentDraftDto updatedShipmentDraftDto = draftController.updateShipmentDraft(shipmentDraftId, draftDto);
    updatedShipmentDraftDto.lineItems().forEach(lineItemDto -> {
      if (!originMap.containsKey(lineItemDto.getId())) {
        String newKey = buildUniqueKey(lineItemDto);
        Optional<ShipmentLineItemDto> find = addedMap.get(newKey).stream().findFirst();
        if (find.isPresent()) {
          addedMap.remove(newKey, find.get());
          find.get().setId(lineItemDto.getId());
        }
      }
    });
    saveShipmentDraftLineItemsExtension(draftDto);
    return draftDto;
  }

  private String buildUniqueKey(ShipmentLineItemDto shipmentLineItemDto) {
    if (null == shipmentLineItemDto.getLot()) {
      return shipmentLineItemDto.getOrderable().getId().toString();
    }
    return shipmentLineItemDto.getLot().getId() + "&" + shipmentLineItemDto.getOrderable().getId();
  }

  public void deleteOrderLineItemAndInitialedExtension(Order order) {
    List<OrderLineItemExtension> extensions = lineItemExtensionRepository
        .findByOrderId(order.getId());
    deleteAddedOrderLineItemsInOrder(extensions, order);
    Set<OrderLineItemExtension> addedLineItemExtensions = deleteAddedLineItemsInExtension(
        extensions);
    initialedExtension(extensions, addedLineItemExtensions);
  }

  private String buildStockCardReservedDtoUniqueKey(StockCardReservedDto dto) {
    return buildUniqueKeyForReserved(dto.getOrderableId(), dto.getOrderableVersionNumber(),
        dto.getLotId(), dto.getLocationCode());
  }

  private String buildShipmentLineItemDtoUniqueKey(ShipmentLineItemDto dto) {
    String locationCode = dto.getLocation() == null ? null : dto.getLocation().getLocationCode();
    return buildUniqueKeyForReserved(dto.getOrderable().getId(), dto.getOrderable().getVersionNumber().intValue(),
        dto.getLotId(), locationCode);
  }

  private static String buildUniqueKeyForReserved(UUID orderableId, Integer orderableVersionNumber,
                                       UUID lotId, String locationCode) {
    List<String> items = new ArrayList<>();
    items.add(orderableId.toString());
    items.add(orderableVersionNumber == null ? "" : String.valueOf(orderableVersionNumber));
    items.add(lotId == null ? "" : lotId.toString());
    items.add(locationCode == null ? "" : locationCode);
    return String.join("_", items);
  }

  public List<StockCardReservedDto> reservedCount(UUID facilityId,
          UUID shipmentDraftId, List<ShipmentLineItemDto> lineItems) {
    List<StockCardReservedDto> allReserved = queryReservedCount(facilityId, shipmentDraftId);
    if (ObjectUtils.isEmpty(lineItems)) {
      return allReserved;
    }
    Map<String, Integer> reservedMap = allReserved
            .stream()
            .collect(Collectors.toMap(this::buildStockCardReservedDtoUniqueKey, StockCardReservedDto::getReserved));
    return lineItems.stream().map(item -> {
      String key = buildShipmentLineItemDtoUniqueKey(item);
      return StockCardReservedDto.builder()
              .orderableId(item.getOrderable().getId())
              .orderableVersionNumber(item.getOrderable().getVersionNumber().intValue())
              .lotId(item.getLotId())
              .reserved(reservedMap.getOrDefault(key, 0))
              .locationCode(item.getLocation() == null ? null : item.getLocation().getLocationCode())
              .build();
    }).collect(Collectors.toList());
  }

  public void checkStockOnHandQuantity(UUID shipmentDraftId, ShipmentDraftDto draftDto) {
    // check quantityShipped
    if (draftDto.getLineItems().stream()
        .anyMatch(item -> item.getQuantityShipped() != null && item.getQuantityShipped() < 0)) {
      throw new ValidationException(SHIPMENT_LINE_ITEMS_INVALID);
    }
    UUID facilityId = draftDto.getOrder().getSupplyingFacility().getId();
    Set<String> orderableLotIdPairs = draftDto.lineItems().stream()
        .filter(item -> (item.getOrderable() != null) && (item.getLot() != null) && (item.getQuantityShipped() != null))
        .map(this::getOrderableLotIdPair)
        .collect(Collectors.toSet());
    if (orderableLotIdPairs.isEmpty()) {
      return;
    }
    List<StockCard> stockCards = siglusStockCardRepository.findByFacilityIdAndOrderableLotIdPairs(
                    facilityId, orderableLotIdPairs);
    boolean hasLocation = facilityConfigHelper.isLocationManagementEnabled(facilityId);
    // get available soh
    List<StockCardStockDto> sohDtos =
            siglusStockCardSummariesService.getLatestStockOnHand(stockCards, hasLocation);
    // get reserved soh
    List<StockCardReservedDto> reservedDtos = queryReservedCount(facilityId, shipmentDraftId);
    // check soh
    if (canNotFulfillShipmentQuantity(sohDtos, reservedDtos, draftDto)) {
      throw new ValidationMessageException(new Message(SHIPMENT_LINE_ITEMS_INVALID));
    }
  }

  private String getOrderableLotIdPair(ShipmentLineItemDto itemDto) {
    return itemDto.getOrderable().getId().toString()
        + Optional.ofNullable(itemDto.getLotId()).map(UUID::toString).orElse("");
  }

  public List<StockCardReservedDto> queryReservedCount(UUID facilityId, UUID shipmentDraftId) {
    List<StockCardReservedDto> reservedDtos;
    if (shipmentDraftId == null) {
      reservedDtos = shipmentDraftLineItemsRepository.reservedCount(facilityId);
    } else {
      reservedDtos = shipmentDraftLineItemsRepository.reservedCount(facilityId, shipmentDraftId);
    }
    return reservedDtos;
  }

  private boolean canNotFulfillShipmentQuantity(List<StockCardStockDto> sohDtos,
                                                List<StockCardReservedDto> reservedDtos,
                                                ShipmentDraftDto draftDto) {
    Map<String, Integer> sohMap = sohDtos.stream().collect(Collectors.toMap(
        dto -> FormatHelper.buildStockCardUniqueKey(
                dto.getOrderableId(), dto.getLotId(), dto.getLocationCode()),
        StockCardStockDto::getStockOnHand
    ));
    Map<String, Integer> reservedMap = reservedDtos
        .stream().collect(Collectors.toMap(
          dto -> FormatHelper.buildStockCardUniqueKey(
                  dto.getOrderableId(), dto.getLotId(), dto.getLocationCode()),
          StockCardReservedDto::getReserved,
            Integer::sum
        ));
    return draftDto.lineItems().stream()
        .filter(item -> (item.getOrderable() != null) && (item.getLot() != null) && (item.getQuantityShipped() != null))
        .anyMatch(item -> {
          String key = FormatHelper.buildStockCardUniqueKey(
              item.getOrderable().getId(), item.getLot().getId(),
              item.getLocation() == null ? null : item.getLocation().getLocationCode());
          int soh = sohMap.getOrDefault(key, 0);
          int reserved = reservedMap.getOrDefault(key, 0);
          return soh - reserved < item.getQuantityShipped().intValue();
        });
  }

  public UUID getDraftIdByOrderId(UUID orderId) {
    if (orderId == null) {
      return null;
    }
    List<ShipmentDraft> drafts = new ArrayList<>(shipmentDraftRepository.findByOrder(new Order(orderId)));
    if (ObjectUtils.isEmpty(drafts)) {
      return null;
    }
    return drafts.get(0).getId();
  }

  private Order getDraftOrder(UUID draftId) {
    ShipmentDraftDto draftDto = siglusShipmentDraftFulfillmentService.searchShipmentDraft(draftId);
    return orderRepository.findOne(draftDto.getOrder().getId());
  }

  private void updateOrderLineItemsWithExtension(ShipmentDraftDto draftDto) {
    Set<UUID> addedLineItemIds = siglusOrderService.updateOrderLineItems(draftDto);

    List<OrderLineItemDto> lineItems = draftDto.getOrder().getOrderLineItems();
    Set<UUID> lineItemIds = lineItems.stream().map(OrderLineItemDto::getId).collect(Collectors.toSet());
    Map<UUID, OrderLineItemExtension> lineItemExtensionMap = lineItemExtensionRepository
        .findByOrderLineItemIdIn(lineItemIds).stream()
        .collect(toMap(OrderLineItemExtension::getOrderLineItemId, extension -> extension));
    Map<UUID, Boolean> lineItemSkippedMap = lineItems.stream()
        .collect(toMap(OrderLineItemDto::getId, OrderLineItemDto::isSkipped));
    List<OrderLineItemExtension> extensionsToUpdate = newArrayList();
    UUID orderId = draftDto.getOrder().getId();
    lineItems.forEach(lineItem -> {
      OrderLineItemExtension extension = lineItemExtensionMap.get(lineItem.getId());
      boolean skipped = lineItemSkippedMap.get(lineItem.getId());
      lineItem.setSkipped(skipped);
      if (null != extension) {
        extension.setSkipped(lineItem.isSkipped());
        extension.setOrderId(orderId);
        extensionsToUpdate.add(extension);
      } else {
        extensionsToUpdate.add(
            OrderLineItemExtension.builder()
                .orderId(orderId)
                .orderLineItemId(lineItem.getId())
                .skipped(lineItem.isSkipped())
                .added(addedLineItemIds.contains(lineItem.getId()))
                .partialFulfilledQuantity(lineItem.getPartialFulfilledQuantity())
                .build());
      }
    });
    log.info("save order line item extension with orderId: {}", orderId);
    lineItemExtensionRepository.save(extensionsToUpdate);
  }

  private void initialedExtension(List<OrderLineItemExtension> extensions,
      Set<OrderLineItemExtension> addedLineItemExtensions) {
    extensions.removeAll(addedLineItemExtensions);
    extensions.forEach(extension -> extension.setSkipped(false));
    if (!extensions.isEmpty()) {
      lineItemExtensionRepository.save(extensions);
    }
  }

  private void deleteAddedOrderLineItemsInOrder(List<OrderLineItemExtension> extensions,
      Order order) {
    Set<UUID> addedIds = extensions.stream()
        .filter(OrderLineItemExtension::isAdded)
        .map(OrderLineItemExtension::getOrderLineItemId)
        .collect(Collectors.toSet());

    log.info("orderId: {}, deleteAddedOrderLineItemIds: {}", order.getId(), addedIds);

    if (!CollectionUtils.isEmpty(addedIds)) {
      List<OrderLineItem> deleteLineItems = orderLineItemRepository
          .findByOrderId(order.getId())
          .stream().filter(orderLineItem -> addedIds.contains(orderLineItem.getId()))
          .collect(Collectors.toList());
      List<OrderLineItem> orderLineItems = orderLineItemRepository
          .findByOrderId(order.getId())
          .stream().filter(orderLineItem -> !addedIds.contains(orderLineItem.getId()))
          .collect(Collectors.toList());
      try {
        order.getOrderLineItems().clear();
        order.getOrderLineItems().addAll(orderLineItems);
      } catch (Exception e) {
        order.setOrderLineItems(orderLineItems);
      }
      orderLineItemRepository.delete(deleteLineItems);
    }
  }

  private Set<OrderLineItemExtension> deleteAddedLineItemsInExtension(
      List<OrderLineItemExtension> extensions) {
    Set<OrderLineItemExtension> addedExtension = extensions.stream()
        .filter(OrderLineItemExtension::isAdded)
        .collect(Collectors.toSet());

    if (!addedExtension.isEmpty()) {
      log.info("delete extension line item: {}", addedExtension);
      lineItemExtensionRepository.delete(addedExtension);
    }
    return addedExtension;
  }

  private void deleteShipmentDraftLineItemsExtension(ShipmentDraftDto shipmentDraft) {
    List<UUID> shipmentDraftLineItemIds = shipmentDraft.getLineItems()
        .stream()
        .map(Importer::getId)
        .collect(Collectors.toList());
    shipmentDraftLineItemsExtensionRepository.deleteByShipmentDraftLineItemIdIn(shipmentDraftLineItemIds);
    shipmentDraftLineItemsExtensionRepository.flush();
  }

  private void deleteShipmentDraftLineItemsExtension(UUID shipmentDraftId) {
    ShipmentDraftDto shipmentDraft = draftController.getShipmentDraft(shipmentDraftId, Collections.emptySet());
    deleteShipmentDraftLineItemsExtension(shipmentDraft);
  }

  public void deleteShipmentDraftLineItemsExtensionByOrderId(UUID orderId) {
    Page<ShipmentDraftDto> shipmentDraftDto = siglusShipmentDraftFulfillmentService.getShipmentDraftByOrderId(orderId);
    if (CollectionUtils.isEmpty(shipmentDraftDto.getContent())) {
      return;
    }
    deleteShipmentDraftLineItemsExtension(shipmentDraftDto.getContent().get(0));
  }

  private void saveShipmentDraftLineItemsExtension(ShipmentDraftDto shipmentDraftDto) {
    List<ShipmentLineItemDto> lineItemDtos = shipmentDraftDto.lineItems();
    List<ShipmentDraftLineItemsExtension> shipmentDraftLineItemsByLocationList = lineItemDtos.stream()
        .map(lineItemDto -> ShipmentDraftLineItemsExtension.builder()
            .locationCode(lineItemDto.getLocation() == null ? null : lineItemDto.getLocation().getLocationCode())
            .area(lineItemDto.getLocation() == null ? null : lineItemDto.getLocation().getArea())
            .shipmentDraftLineItemId(lineItemDto.getId())
            .quantityShipped(
                lineItemDto.getQuantityShipped() == null ? null : lineItemDto.getQuantityShipped().intValue())
            .build())
        .collect(Collectors.toList());
    shipmentDraftLineItemsExtensionRepository.save(shipmentDraftLineItemsByLocationList);
  }

  private SiglusShipmentDraftDto convertFromShipmentDraftDto(ShipmentDraftDto shipmentDraftDto) {
    SiglusShipmentDraftDto dto = new SiglusShipmentDraftDto();
    dto.setId(shipmentDraftDto.getId());
    dto.setNotes(shipmentDraftDto.getNotes());
    if (ObjectUtils.isEmpty(shipmentDraftDto.getLineItems())) {
      dto.setLineItems(new ArrayList<>());
      return dto;
    }
    Order order = orderRepository.findOne(shipmentDraftDto.getOrder().getId());
    UUID supplyFacility = order.getSupplyingFacilityId();
    boolean withLocation = facilityConfigHelper.isLocationManagementEnabled(supplyFacility);
    if (withLocation) {
      fulfillShipmentDraftLineItem(shipmentDraftDto.lineItems());
    }
    Set<UUID> orderableSet = shipmentDraftDto.getLineItems().stream()
        .map(lineItem -> lineItem.getOrderableIdentity().getId()).collect(Collectors.toSet());
    Map<String, Integer> reservedMap = getReservedMap(supplyFacility, shipmentDraftDto);
    Map<String, StockCardStockDto> latestSohMap = getLatestSohMap(supplyFacility, orderableSet, withLocation);
    Map<UUID, OrderableVersionDto> orderableMap = siglusOrderableService.findLatestVersionByIds(orderableSet)
        .stream().collect(toMap(OrderableVersionDto::getId, Function.identity()));
    Map<UUID, LotDto> lotMap = getLotsMap(shipmentDraftDto.lineItems());

    List<SiglusShipmentDraftLineItemDto> lineItems =
        buildShipmentDraftLineItemDtos(shipmentDraftDto.lineItems(), reservedMap, latestSohMap, orderableMap, lotMap);
    dto.setLineItems(lineItems);
    return dto;
  }

  private Map<String, StockCardStockDto> getLatestSohMap(UUID supplyFacility,
                                                         Set<UUID> orderableSet, boolean withLocation) {
    Map<UUID, StockCard> stockCardMap = siglusStockCardService.findStockCardsByFacilityAndOrderables(
        supplyFacility, orderableSet)
        .stream().collect(toMap(StockCard::getId, Function.identity()));
    return siglusStockCardSummariesService.getLatestStockOnHandByIds(stockCardMap.keySet(), withLocation).stream()
        .peek(dto -> {
          StockCard stockCard = stockCardMap.get(dto.getStockCardId());
          dto.setLotId(stockCard.getLotId());
          dto.setOrderableId(stockCard.getOrderableId());
        })
        .collect(toMap(dto -> FormatHelper.buildStockCardUniqueKey(
                dto.getOrderableId(), dto.getLotId(), dto.getLocationCode()),
            Function.identity()));
  }

  private Map<String, Integer> getReservedMap(UUID supplyFacility, ShipmentDraftDto shipmentDraftDto) {
    return reservedCount(
        supplyFacility, shipmentDraftDto.getId(), shipmentDraftDto.lineItems())
        .stream()
        .collect(toMap(reservedDto -> FormatHelper.buildStockCardUniqueKey(
                reservedDto.getOrderableId(), reservedDto.getLotId(), reservedDto.getLocationCode()),
            StockCardReservedDto::getReserved));
  }

  private Map<UUID, LotDto> getLotsMap(List<ShipmentLineItemDto> lineItemDtos) {
    List<UUID> lotIds = lineItemDtos.stream().map(ShipmentLineItemDto::getLotId)
        .distinct().collect(Collectors.toList());
    return siglusLotService.getLotList(lotIds).stream().collect(toMap(LotDto::getId, Function.identity()));
  }

  private void fulfillShipmentDraftLineItem(List<ShipmentLineItemDto> lineItems) {
    List<UUID> lineItemIds = lineItems.stream().map(ShipmentLineItemDto::getId).collect(Collectors.toList());
    Map<UUID, ShipmentDraftLineItemsExtension> extensionMap = shipmentDraftLineItemsExtensionRepository
        .findByShipmentDraftLineItemIdIn(lineItemIds).stream()
        .collect(toMap(ShipmentDraftLineItemsExtension::getShipmentDraftLineItemId, Function.identity()));
    lineItems.forEach(lineItem -> {
      ShipmentDraftLineItemsExtension extension = extensionMap.get(lineItem.getId());
      if (extension != null) {
        LocationDto locationDto = LocationDto
            .builder()
            .locationCode(extension.getLocationCode())
            .area(extension.getArea())
            .build();
        lineItem.setLocation(locationDto);
        lineItem.setQuantityShipped(extension.getQuantityShipped() == null
            ? null : extension.getQuantityShipped().longValue());
      }
    });
  }

  private List<SiglusShipmentDraftLineItemDto> buildShipmentDraftLineItemDtos(
      List<ShipmentLineItemDto> lineItemDtos,
      Map<String, Integer> reservedMap,
      Map<String, StockCardStockDto> latestSohMap,
      Map<UUID, OrderableVersionDto> orderableMap,
      Map<UUID, LotDto> lotMap) {
    return lineItemDtos.stream().map(
        dto -> {
          SiglusShipmentDraftLineItemDto itemDto = SiglusShipmentDraftLineItemDto.from(dto);
          itemDto.setOrderable(orderableMap.get(dto.getOrderable().getId()));
          String location = dto.getLocation() == null ? null : dto.getLocation().getLocationCode();
          String key = FormatHelper.buildStockCardUniqueKey(dto.getOrderable().getId(), dto.getLotId(), location);
          StockCardStockDto stockCardStockDto = latestSohMap.get(key);
          if (stockCardStockDto != null) {
            itemDto.setStockCardId(stockCardStockDto.getStockCardId());
            if (stockCardStockDto.getStockOnHand() != null) {
              itemDto.setStockOnHand(Long.valueOf(stockCardStockDto.getStockOnHand()));
            }
          }
          itemDto.setReservedStock(reservedMap.get(key));
          itemDto.setLot(lotMap.get(dto.getLotId()));
          return itemDto;
        })
        .filter(dto -> dto.getStockOnHand() != null && dto.getStockOnHand() > 0)
        .collect(Collectors.toList());
  }

  private ShipmentDraftDto createShipmentDraftForOrder(OrderDto order) {
    UUID supplyFacility = order.getSupplyingFacility().getId();
    boolean withLocation = facilityConfigHelper.isLocationManagementEnabled(supplyFacility);
    Set<UUID> orderableIdSet = order.orderLineItems().stream()
        .map(lineItem -> lineItem.getOrderableIdentity().getId()).collect(Collectors.toSet());
    Map<String, StockCardStockDto> latestSohMap = getLatestSohMap(supplyFacility, orderableIdSet, withLocation);
    Map<UUID, List<StockCardStockDto>> orderableGroup = latestSohMap.values().stream()
        .collect(Collectors.groupingBy(StockCardStockDto::getOrderableId));
    List<ShipmentLineItemDto> draftLineItems = order.orderLineItems().stream().map(
        orderLineItemDto -> createShipmentDraftLineItemDtos(orderLineItemDto, orderableGroup)
        )
        .flatMap(Collection::stream).collect(Collectors.toList());
    OrderObjectReferenceDto orderObjectReferenceDto = new OrderObjectReferenceDto(order.getId());
    BeanUtils.copyProperties(order, orderObjectReferenceDto);
    return new ShipmentDraftDto(null, null, orderObjectReferenceDto, null, draftLineItems);
  }

  private List<ShipmentLineItemDto> createShipmentDraftLineItemDtos(OrderLineItemDto orderLineItem,
      Map<UUID, List<StockCardStockDto>> orderableGroup) {
    List<StockCardStockDto> stockCardStockDtos = orderableGroup.get(orderLineItem.getOrderable().getId());
    if (ObjectUtils.isEmpty(stockCardStockDtos)) {
      return Collections.emptyList();
    }
    return stockCardStockDtos.stream().filter(dto -> dto.getStockOnHand() != null && dto.getStockOnHand() > 0)
        .map(dto -> {
          VersionObjectReferenceDto orderDto =
              new VersionObjectReferenceDto(orderLineItem.getOrderable().getId(), null, null,
                  orderLineItem.getOrderable().getVersionNumber());
          ObjectReferenceDto lotDto = new ObjectReferenceDto(dto.getLotId());
          LocationDto locationDto = null;
          if (dto.getArea() != null && dto.getLocationCode() != null) {
            locationDto = new LocationDto(dto.getArea(), dto.getLocationCode());
          }
          return new ShipmentLineItemDto(null, null, orderDto, lotDto, locationDto,
              dto.getStockOnHand().longValue(), 0L, null);
        }).collect(Collectors.toList());
  }

  /**
   * the location data will lose after save the draft, so need to get it back.
   */
  private void fillLocationForCreatedShipmentDraft(ShipmentDraftDto savedDraftDto, ShipmentDraftDto originDraftDto) {
    Multimap<String, ShipmentLineItemDto> uniqueKeyMap = ArrayListMultimap.create();
    originDraftDto.lineItems().forEach(shipmentLineItemDto -> {
      String uniqueKey = buildUniqueKey(shipmentLineItemDto);
      uniqueKeyMap.put(uniqueKey, shipmentLineItemDto);
    });
    savedDraftDto.lineItems().forEach(
        lineItem -> {
          String uniqueKey = buildUniqueKey(lineItem);
          Optional<ShipmentLineItemDto> first = uniqueKeyMap.get(uniqueKey).stream().findFirst();
          if (first.isPresent()) {
            lineItem.setLocation(first.get().getLocation());
            uniqueKeyMap.remove(uniqueKey, first.get());
          }
        }
    );
  }
}
