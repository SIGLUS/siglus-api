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

import static java.util.stream.Collectors.toSet;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_ORDER_EXPIRED;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_PERIOD_NOT_FOUND;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_SUB_ORDER_LINE_ITEM;
import static org.siglus.siglusapi.i18n.MessageKeys.SHIPMENT_ORDER_STATUS_INVALID;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openlmis.fulfillment.domain.Order;
import org.openlmis.fulfillment.domain.OrderLineItem;
import org.openlmis.fulfillment.domain.OrderStatus;
import org.openlmis.fulfillment.domain.ProofOfDelivery;
import org.openlmis.fulfillment.domain.ShipmentLineItem;
import org.openlmis.fulfillment.domain.ShipmentLineItem.Importer;
import org.openlmis.fulfillment.repository.OrderRepository;
import org.openlmis.fulfillment.web.OrderController;
import org.openlmis.fulfillment.web.shipment.ShipmentController;
import org.openlmis.fulfillment.web.shipment.ShipmentDto;
import org.openlmis.fulfillment.web.shipment.ShipmentLineItemDto;
import org.openlmis.fulfillment.web.util.OrderDto;
import org.openlmis.fulfillment.web.util.OrderLineItemDto;
import org.openlmis.fulfillment.web.util.OrderObjectReferenceDto;
import org.openlmis.stockmanagement.domain.card.StockCard;
import org.openlmis.stockmanagement.domain.card.StockCardLineItem;
import org.openlmis.stockmanagement.repository.StockCardLineItemRepository;
import org.openlmis.stockmanagement.repository.StockCardRepository;
import org.siglus.common.domain.ProcessingPeriodExtension;
import org.siglus.common.repository.ProcessingPeriodExtensionRepository;
import org.siglus.siglusapi.domain.OrderLineItemExtension;
import org.siglus.siglusapi.domain.PodExtension;
import org.siglus.siglusapi.domain.ShipmentLineItemsExtension;
import org.siglus.siglusapi.domain.StockCardLineItemExtension;
import org.siglus.siglusapi.dto.Message;
import org.siglus.siglusapi.exception.BusinessDataException;
import org.siglus.siglusapi.exception.NotFoundException;
import org.siglus.siglusapi.exception.ValidationMessageException;
import org.siglus.siglusapi.repository.OrderLineItemExtensionRepository;
import org.siglus.siglusapi.repository.PodExtensionRepository;
import org.siglus.siglusapi.repository.ShipmentLineItemsExtensionRepository;
import org.siglus.siglusapi.repository.SiglusProofOfDeliveryRepository;
import org.siglus.siglusapi.repository.StockCardLineItemExtensionRepository;
import org.siglus.siglusapi.util.SiglusAuthenticationHelper;
import org.siglus.siglusapi.web.request.ShipmentExtensionRequest;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class SiglusShipmentService {

  private final OrderRepository orderRepository;

  private final OrderLineItemExtensionRepository lineItemExtensionRepository;

  private final ShipmentController shipmentController;

  private final SiglusOrderService siglusOrderService;

  private final OrderController orderController;

  private final ShipmentLineItemsExtensionRepository shipmentLineItemsExtensionRepository;

  private final CalculatedStocksOnHandByLocationService calculatedStocksOnHandByLocationService;

  private final SiglusAuthenticationHelper authenticationHelper;

  private final SiglusProofOfDeliveryRepository siglusProofOfDeliveryRepository;

  private final PodExtensionRepository podExtensionRepository;

  private final StockCardLineItemRepository stockCardLineItemRepository;

  private final StockCardRepository stockCardRepository;

  private final StockCardLineItemExtensionRepository stockCardLineItemExtensionRepository;

  private final ProcessingPeriodExtensionRepository processingPeriodExtensionRepository;

  @Transactional
  public ShipmentDto createOrderAndShipment(boolean isSubOrder, ShipmentExtensionRequest shipmentExtensionRequest) {
    ShipmentDto shipmentDto = createOrderAndConfirmShipment(isSubOrder,
        shipmentExtensionRequest.getShipment());
    savePodExtension(shipmentDto.getId(), shipmentExtensionRequest);
    return shipmentDto;
  }

  public void checkFulfillOrderExpired(ShipmentExtensionRequest shipmentExtensionRequest) {
    int currentOrderFulfillMonth = shipmentExtensionRequest.getShipment().getOrder().getProcessingPeriod().getEndDate()
        .getMonthValue();
    UUID processingPeriodId = shipmentExtensionRequest.getShipment().getOrder().getProcessingPeriod().getId();
    ProcessingPeriodExtension processingPeriodExtension = processingPeriodExtensionRepository
        .findByProcessingPeriodId(processingPeriodId);
    if (processingPeriodExtension == null) {
      throw new NotFoundException(ERROR_PERIOD_NOT_FOUND);
    }
    List<Integer> calculatedFulfillOrderMonth = siglusOrderService
        .calculateFulfillOrderMonth(processingPeriodExtension);
    if (!calculatedFulfillOrderMonth.contains(currentOrderFulfillMonth)) {
      throw new BusinessDataException(new Message(ERROR_ORDER_EXPIRED));
    }
  }

  @Transactional
  public ShipmentDto createOrderAndShipmentByLocation(boolean isSubOrder,
      ShipmentExtensionRequest shipmentExtensionRequest) {
    List<ShipmentLineItemDto> shipmentLineItemDtos = shipmentExtensionRequest.getShipment().lineItems();
    Multimap<String, ShipmentLineItemDto> uniqueKeyMap = ArrayListMultimap.create();
    shipmentLineItemDtos.forEach(shipmentLineItemDto -> {
      String uniqueKey = buildForUniqueKey(shipmentLineItemDto);
      uniqueKeyMap.put(uniqueKey, shipmentLineItemDto);
    });
    mergeShipmentLineItems(shipmentExtensionRequest.getShipment());
    ShipmentDto confirmedShipmentDto = createOrderAndConfirmShipment(isSubOrder,
        shipmentExtensionRequest.getShipment());
    fulfillLocationInfo(uniqueKeyMap, confirmedShipmentDto);
    List<ShipmentLineItemDto> shipmentLineItems = new ArrayList<>(uniqueKeyMap.values());
    saveToShipmentLineItemExtension(shipmentLineItems);
    UUID facilityId = authenticationHelper.getCurrentUser().getHomeFacilityId();
    calculatedStocksOnHandByLocationService.calculateStockOnHandByLocationForShipment(shipmentLineItems, facilityId);
    savePodExtension(confirmedShipmentDto.getId(), shipmentExtensionRequest);
    saveShipmentLineItemsWithLocation(shipmentLineItems, facilityId);
    return confirmedShipmentDto;
  }

  private void fulfillLocationInfo(Multimap<String, ShipmentLineItemDto> uniqueKeyMap, ShipmentDto shipmentDto) {
    for (ShipmentLineItemDto lineItemDto : shipmentDto.lineItems()) {
      String newKey = buildForUniqueKey(lineItemDto);
      List<ShipmentLineItemDto> lineItemDtos = (List<ShipmentLineItemDto>) uniqueKeyMap.get(newKey);
      if (!CollectionUtils.isEmpty(lineItemDtos)) {
        lineItemDtos.forEach(m -> m.setId(lineItemDto.getId()));
      }
    }
  }

  private String buildForUniqueKey(ShipmentLineItemDto shipmentLineItemDto) {
    if (null == shipmentLineItemDto.getLot()) {
      return shipmentLineItemDto.getOrderable().getId().toString();
    }
    return shipmentLineItemDto.getLot().getId() + "&" + shipmentLineItemDto.getOrderable().getId();
  }

  @Transactional
  public ShipmentDto createSubOrderAndShipment(ShipmentDto shipmentDto) {
    createSubOrder(shipmentDto, false);
    return createShipment(shipmentDto);
  }

  ShipmentDto createSubOrderAndShipment(boolean isSubOrder, ShipmentDto shipmentDto) {
    if (isSubOrder) {
      createSubOrder(shipmentDto, true);
    }
    return createShipment(shipmentDto);
  }

  private ShipmentDto createOrderAndConfirmShipment(boolean isSubOrder, ShipmentDto shipmentDto) {
    OrderDto orderDto = orderController.getOrder(shipmentDto.getOrder().getId(), null);
    validateOrderStatus(orderDto);
    if (siglusOrderService.needCloseOrder(orderDto)) {
      if (siglusOrderService.isSuborder(orderDto.getExternalId())) {
        throw new ValidationMessageException(SHIPMENT_ORDER_STATUS_INVALID);
      }
      isSubOrder = false;
    }
    updateOrderLineItems(shipmentDto.getOrder());
    return createSubOrderAndShipment(isSubOrder, shipmentDto);
  }

  private void validateOrderStatus(OrderDto orderDto) {
    if (orderDto.getStatus().equals(OrderStatus.CLOSED)) {
      throw new ValidationMessageException(SHIPMENT_ORDER_STATUS_INVALID);
    }
  }

  private void createSubOrder(ShipmentDto shipmentDto, boolean needValidate) {
    Set<UUID> skippedOrderLineItemIds = getSkippedOrderLineItemIds(shipmentDto);
    Map<UUID, List<ShipmentLineItem.Importer>> groupShipment = shipmentDto.getLineItems().stream()
        .collect(Collectors.groupingBy(lineItem -> lineItem.getOrderableIdentity().getId()));
    OrderObjectReferenceDto order = shipmentDto.getOrder();
    List<OrderLineItemDto> orderLineItems = order.getOrderLineItems();
    List<OrderLineItemDto> subOrderLineItems = getSubOrderLineItemDtos(skippedOrderLineItemIds,
        groupShipment, orderLineItems);
    if (subOrderLineItems.isEmpty() && needValidate) {
      throw new ValidationMessageException(new Message(ERROR_SUB_ORDER_LINE_ITEM));
    } else if (!subOrderLineItems.isEmpty()) {
      siglusOrderService.createSubOrder(order, subOrderLineItems);
    }
  }

  private ShipmentDto createShipment(ShipmentDto shipmentDto) {
    Set<UUID> skippedOrderLineItemIds = getSkippedOrderLineItemIds(shipmentDto);
    removeSkippedOrderLineItemsAndExtensions(skippedOrderLineItemIds, shipmentDto.getOrder().getId());
    Set<UUID> skippedOrderableIds = getSkippedOrderableIds(shipmentDto);
    shipmentDto.lineItems().removeIf(lineItem -> skippedOrderableIds.contains(lineItem.getOrderable().getId()));
    return shipmentController.createShipment(shipmentDto);
  }

  private Set<UUID> getSkippedOrderableIds(ShipmentDto shipmentDto) {
    return shipmentDto.getOrder().getOrderLineItems().stream()
        .filter(OrderLineItemDto::isSkipped)
        .map(orderLineItemDto -> orderLineItemDto.getOrderable().getId())
        .collect(toSet());
  }

  private Set<UUID> getSkippedOrderLineItemIds(ShipmentDto shipmentDto) {
    return shipmentDto.getOrder().getOrderLineItems().stream()
        .filter(OrderLineItemDto::isSkipped)
        .map(OrderLineItemDto::getId)
        .filter(Objects::nonNull)
        .collect(toSet());
  }

  private void removeSkippedOrderLineItemsAndExtensions(Set<UUID> skippedOrderLineItemIds,
      UUID orderId) {
    Order order = orderRepository.findOne(orderId);
    order.getOrderLineItems().removeIf(
        orderLineItem -> skippedOrderLineItemIds.contains(orderLineItem.getId()));
    orderRepository.save(order);
    List<OrderLineItemExtension> extensions = lineItemExtensionRepository
        .findByOrderLineItemIdIn(skippedOrderLineItemIds);
    lineItemExtensionRepository.delete(extensions);
  }

  private List<OrderLineItemDto> getSubOrderLineItemDtos(Set<UUID> skippedOrderLineItemIds,
      Map<UUID, List<Importer>> groupShipment, List<OrderLineItemDto> orderLineItems) {
    List<OrderLineItemDto> subOrderLineItems = new ArrayList<>();
    for (OrderLineItemDto dto : orderLineItems) {
      if (!skippedOrderLineItemIds.contains(dto.getId())
          && dto.getOrderedQuantity() != null && dto.getOrderedQuantity() > 0) {
        calculateSubOrderPartialFulfilledValue(groupShipment, subOrderLineItems, dto);
      }
    }
    return subOrderLineItems;
  }

  private void calculateSubOrderPartialFulfilledValue(Map<UUID, List<Importer>> groupShipment,
      List<OrderLineItemDto> subOrderLineItems, OrderLineItemDto dto) {
    dto.setId(null);
    if (groupShipment.containsKey(dto.getOrderable().getId())) {
      Long shippedValue = getShippedValue(groupShipment, dto.getOrderable().getId());
      if (dto.getPartialFulfilledQuantity() + shippedValue < dto.getOrderedQuantity()) {
        Long partialFulfilledQuantity = dto.getPartialFulfilledQuantity() + shippedValue;
        dto.setPartialFulfilledQuantity(partialFulfilledQuantity);
        subOrderLineItems.add(dto);
      }
    } else {
      subOrderLineItems.add(dto);
    }
  }

  private Long getShippedValue(Map<UUID, List<ShipmentLineItem.Importer>> groupShipment,
      UUID orderableId) {
    List<ShipmentLineItem.Importer> shipments = groupShipment.get(orderableId);
    Long shipmentValue = Long.valueOf(0);
    for (ShipmentLineItem.Importer shipment : shipments) {
      shipmentValue += shipment.getQuantityShipped();
    }
    return shipmentValue;
  }

  private void updateOrderLineItems(OrderObjectReferenceDto orderDto) {
    Order order = orderRepository.findOne(orderDto.getId());
    List<OrderLineItemDto> orderLineItemDtos = orderDto.getOrderLineItems();

    List<OrderLineItem> original = order.getOrderLineItems();

    orderLineItemDtos.stream()
        .filter(orderLineItemDto -> orderLineItemDto.getId() == null)
        .filter(orderLineItemDto -> !orderLineItemDto.isSkipped())
        .map(OrderLineItem::newInstance)
        .forEach(orderLineItem -> {
          orderLineItem.setOrder(order);
          original.add(orderLineItem);
        });

    log.info("update orderId: {}, orderLineItem: {}", order.getId(), original);
    orderRepository.save(order);
  }

  private void saveShipmentLineItemsWithLocation(List<ShipmentLineItemDto> shipmentLineItems, UUID facilityId) {
    Map<UUID, UUID> shipmentLineItemIdToStockCardId = new HashMap<>();
    Map<UUID, List<ShipmentLineItemDto>> shipmentLineItemIdToDtos = new HashMap<>();
    shipmentLineItems.forEach(shipmentLineItem -> {
      UUID orderableId = shipmentLineItem.getOrderable().getId();
      UUID lotId = shipmentLineItem.getLotId();
      StockCard stockCard = stockCardRepository.findByFacilityIdAndOrderableIdAndLotId(
          facilityId, orderableId, lotId);
      shipmentLineItemIdToStockCardId.put(shipmentLineItem.getId(), stockCard.getId());
      if (shipmentLineItemIdToDtos.containsKey(shipmentLineItem.getId())) {
        List<ShipmentLineItemDto> shipmentLineItemDtos = shipmentLineItemIdToDtos.get(shipmentLineItem.getId());
        shipmentLineItemDtos.add(shipmentLineItem);
        shipmentLineItemIdToDtos.put(shipmentLineItem.getId(), shipmentLineItemDtos);
      } else {
        shipmentLineItemIdToDtos.put(shipmentLineItem.getId(), Lists.newArrayList(shipmentLineItem));
      }
    });

    List<StockCardLineItem> stockCardLineItems = stockCardLineItemRepository
        .findLatestByStockCardIds(shipmentLineItemIdToStockCardId.values());
    Map<UUID, StockCardLineItem> shipmentLineItemIdToStockCardLineItem = new HashMap<>();
    shipmentLineItemIdToStockCardId.forEach((shipmentLineItemId, stockCardId) ->
        stockCardLineItems.stream()
            .filter(m -> m.getStockCard().getId().equals(stockCardId))
            .findFirst()
            .ifPresent(
                stockCardLineItem -> shipmentLineItemIdToStockCardLineItem.put(shipmentLineItemId, stockCardLineItem)));

    List<StockCardLineItemExtension> stockCardLineItemExtensions = Lists.newArrayList();
    shipmentLineItemIdToDtos.forEach((shipmentLineItemId, shipmentLineItemDtoList) -> {
      if (shipmentLineItemIdToStockCardLineItem.containsKey(shipmentLineItemId)) {
        shipmentLineItemDtoList.forEach(shipmentLineItemDto -> {
          StockCardLineItemExtension stockCardLineItemExtension = StockCardLineItemExtension
              .builder()
              .stockCardLineItemId(shipmentLineItemIdToStockCardLineItem.get(shipmentLineItemId).getId())
              .area(shipmentLineItemDto.getLocation().getArea())
              .locationCode(shipmentLineItemDto.getLocation().getLocationCode())
              .build();
          stockCardLineItemExtensions.add(stockCardLineItemExtension);
        });
      }
    });
    log.info("save to stock card line item by location; size: {}", stockCardLineItemExtensions.size());
    stockCardLineItemExtensionRepository.save(stockCardLineItemExtensions);
  }

  private void savePodExtension(UUID shipmentId, ShipmentExtensionRequest shipmentExtensionRequest) {
    ProofOfDelivery proofOfDelivery = siglusProofOfDeliveryRepository.findByShipmentId(shipmentId);
    UUID podId = proofOfDelivery.getId();
    PodExtension podExtension = PodExtension
        .builder()
        .podId(podId)
        .conferredBy(shipmentExtensionRequest.getConferredBy())
        .preparedBy(shipmentExtensionRequest.getPreparedBy())
        .build();
    log.info("save pod extension when confirm shipment, shipmentId: {}", shipmentId);
    podExtensionRepository.save(podExtension);
  }

  public void mergeShipmentLineItems(ShipmentDto shipmentDto) {
    List<ShipmentLineItemDto> lineItemDtos = shipmentDto.lineItems();
    List<ShipmentLineItemDto> newLineItemDtos = new ArrayList<>();
    lineItemDtos.forEach(lineItem -> {
      ShipmentLineItemDto shipmentLineItemDto = new ShipmentLineItemDto();
      BeanUtils.copyProperties(lineItem, shipmentLineItemDto);
      newLineItemDtos.add(shipmentLineItemDto);
    });
    Map<String, ShipmentLineItemDto> uniqueKeyToShipmentDto = new HashMap<>();
    newLineItemDtos.forEach(shipmentLineItem -> {
      String key = buildForUniqueKey(shipmentLineItem);
      if (uniqueKeyToShipmentDto.containsKey(key)) {
        ShipmentLineItemDto shipmentLineItemDto = uniqueKeyToShipmentDto.get(key);
        Long quantityShipped = shipmentLineItem.getQuantityShipped() + shipmentLineItemDto.getQuantityShipped();
        if (null != shipmentLineItemDto.getId()) {
          shipmentLineItemDto.setQuantityShipped(quantityShipped);
          uniqueKeyToShipmentDto.put(key, shipmentLineItemDto);
        } else {
          shipmentLineItem.setQuantityShipped(quantityShipped);
          uniqueKeyToShipmentDto.put(key, shipmentLineItem);
        }
      } else {
        uniqueKeyToShipmentDto.put(key, shipmentLineItem);
      }
    });
    shipmentDto.setLineItems(new ArrayList<>(uniqueKeyToShipmentDto.values()));
  }

  private void saveToShipmentLineItemExtension(List<ShipmentLineItemDto> shipmentLineItems) {
    List<ShipmentLineItemsExtension> shipmentLineItemsByLocations = Lists.newArrayList();
    shipmentLineItems.forEach(shipmentLineItemDto -> {
      UUID lineItemId = shipmentLineItemDto.getId();
      String locationCode = shipmentLineItemDto.getLocation().getLocationCode();
      String area = shipmentLineItemDto.getLocation().getArea();
      ShipmentLineItemsExtension shipmentLineItemsByLocation = ShipmentLineItemsExtension
          .builder()
          .shipmentLineItemId(lineItemId)
          .locationCode(locationCode)
          .area(area)
          .quantityShipped(shipmentLineItemDto.getQuantityShipped().intValue())
          .build();
      shipmentLineItemsByLocations.add(shipmentLineItemsByLocation);
    });
    log.info("create shipment line item by location, size: {}", shipmentLineItemsByLocations.size());
    shipmentLineItemsExtensionRepository.save(shipmentLineItemsByLocations);
  }
}
