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
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_SUB_ORDER_LINE_ITEM;
import static org.siglus.siglusapi.i18n.MessageKeys.SHIPMENT_ORDER_STATUS_INVALID;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
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
import org.siglus.siglusapi.domain.OrderLineItemExtension;
import org.siglus.siglusapi.domain.ProofsOfDeliveryExtension;
import org.siglus.siglusapi.domain.ShipmentLineItemsExtension;
import org.siglus.siglusapi.dto.Message;
import org.siglus.siglusapi.exception.ValidationMessageException;
import org.siglus.siglusapi.repository.OrderLineItemExtensionRepository;
import org.siglus.siglusapi.repository.ProofsOfDeliveryExtensionRepository;
import org.siglus.siglusapi.repository.ShipmentLineItemsExtensionRepository;
import org.siglus.siglusapi.repository.SiglusProofOfDeliveryRepository;
import org.siglus.siglusapi.util.SiglusAuthenticationHelper;
import org.siglus.siglusapi.web.request.ShipmentExtensionRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

  private final ProofsOfDeliveryExtensionRepository proofsOfDeliveryExtensionRepository;

  @Transactional
  public ShipmentDto createOrderAndShipment(boolean isSubOrder, ShipmentExtensionRequest shipmentExtensionRequest) {
    ShipmentDto shipmentDto = createOrderAndConfirmShipment(isSubOrder,
        shipmentExtensionRequest.getShipment());
    savePodExtension(shipmentDto.getId(), shipmentExtensionRequest);
    return shipmentDto;
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
    ShipmentDto confirmedShipmentDto = createOrderAndConfirmShipment(isSubOrder,
        shipmentExtensionRequest.getShipment());
    List<ShipmentLineItemsExtension> shipmentLineItemsByLocations = Lists.newArrayList();
    fulfillLocationInfo(uniqueKeyMap, confirmedShipmentDto);
    confirmedShipmentDto.lineItems().forEach(shipmentLineItemDto -> {
      UUID lineItemId = shipmentLineItemDto.getId();
      String locationCode = shipmentLineItemDto.getLocation().getLocationCode();
      String area = shipmentLineItemDto.getLocation().getArea();
      ShipmentLineItemsExtension shipmentLineItemsByLocation = ShipmentLineItemsExtension
          .builder()
          .shipmentLineItemId(lineItemId)
          .locationCode(locationCode)
          .area(area)
          .build();
      shipmentLineItemsByLocations.add(shipmentLineItemsByLocation);
    });
    log.info("create shipment line item by location, size: {}", shipmentLineItemsByLocations.size());
    shipmentLineItemsExtensionRepository.save(shipmentLineItemsByLocations);
    UUID facilityId = authenticationHelper.getCurrentUser().getHomeFacilityId();
    calculatedStocksOnHandByLocationService.calculateStockOnHandByLocationForShipment(confirmedShipmentDto.lineItems(),
        facilityId);
    savePodExtension(confirmedShipmentDto.getId(), shipmentExtensionRequest);
    return confirmedShipmentDto;
  }

  private void fulfillLocationInfo(Multimap<String, ShipmentLineItemDto> uniqueKeyMap, ShipmentDto shipmentDto) {
    for (ShipmentLineItemDto lineItemDto : shipmentDto.lineItems()) {
      String newKey = buildForUniqueKey(lineItemDto);
      List<ShipmentLineItemDto> lineItemDtos = (List<ShipmentLineItemDto>) uniqueKeyMap.get(newKey);
      if (null != lineItemDtos.get(0).getLocation()) {
        lineItemDto.setLocation(lineItemDtos.get(0).getLocation());
        uniqueKeyMap.remove(newKey, lineItemDtos.get(0));
      }
    }
  }

  private String buildForUniqueKey(ShipmentLineItemDto shipmentLineItemDto) {
    if (null == shipmentLineItemDto.getLot()) {
      return shipmentLineItemDto.getOrderable().getId() + "&" + shipmentLineItemDto.getQuantityShipped();
    }
    return shipmentLineItemDto.getLot().getId() + "&" + shipmentLineItemDto.getOrderable().getId()
        + "&" + shipmentLineItemDto.getQuantityShipped();
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

  private void savePodExtension(UUID shipmentId, ShipmentExtensionRequest shipmentExtensionRequest) {
    ProofOfDelivery proofOfDelivery = siglusProofOfDeliveryRepository.findByShipmentId(shipmentId);
    UUID podId = proofOfDelivery.getId();
    ProofsOfDeliveryExtension proofsOfDeliveryExtension = ProofsOfDeliveryExtension
        .builder()
        .podId(podId)
        .conferredBy(shipmentExtensionRequest.getConferredBy())
        .preparedBy(shipmentExtensionRequest.getPreparedBy())
        .build();
    log.info("save pod extension when confirm shipment, shipmentId: {}", shipmentId);
    proofsOfDeliveryExtensionRepository.save(proofsOfDeliveryExtension);
  }
}
