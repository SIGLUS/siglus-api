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
import static org.siglus.siglusapi.util.LocationUtil.getIfNonNull;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import java.time.Month;
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
import org.openlmis.fulfillment.web.shipment.LocationDto;
import org.openlmis.fulfillment.web.shipment.ShipmentController;
import org.openlmis.fulfillment.web.shipment.ShipmentDto;
import org.openlmis.fulfillment.web.shipment.ShipmentLineItemDto;
import org.openlmis.fulfillment.web.util.OrderDto;
import org.openlmis.fulfillment.web.util.OrderLineItemDto;
import org.openlmis.fulfillment.web.util.OrderObjectReferenceDto;
import org.siglus.common.domain.ProcessingPeriodExtension;
import org.siglus.common.repository.ProcessingPeriodExtensionRepository;
import org.siglus.siglusapi.constant.FieldConstants;
import org.siglus.siglusapi.domain.OrderLineItemExtension;
import org.siglus.siglusapi.domain.PodExtension;
import org.siglus.siglusapi.domain.ShipmentLineItemsExtension;
import org.siglus.siglusapi.dto.Message;
import org.siglus.siglusapi.exception.BusinessDataException;
import org.siglus.siglusapi.exception.NotFoundException;
import org.siglus.siglusapi.exception.ValidationMessageException;
import org.siglus.siglusapi.repository.OrderLineItemExtensionRepository;
import org.siglus.siglusapi.repository.PodExtensionRepository;
import org.siglus.siglusapi.repository.ShipmentLineItemsExtensionRepository;
import org.siglus.siglusapi.repository.SiglusProofOfDeliveryRepository;
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

  private final SiglusProofOfDeliveryRepository siglusProofOfDeliveryRepository;

  private final PodExtensionRepository podExtensionRepository;

  private final ProcessingPeriodExtensionRepository processingPeriodExtensionRepository;

  @Transactional
  public ShipmentDto createOrderAndShipment(boolean isSubOrder, ShipmentExtensionRequest shipmentExtensionRequest) {
    ShipmentDto shipmentDto = createOrderAndConfirmShipment(isSubOrder,
        shipmentExtensionRequest.getShipment(), false);
    savePodExtension(shipmentDto.getId(), shipmentExtensionRequest);
    return shipmentDto;
  }

  public void checkFulfillOrderExpired(ShipmentExtensionRequest shipmentExtensionRequest) {
    Month currentOrderFulfillMonth = shipmentExtensionRequest.getShipment().getOrder().getProcessingPeriod()
        .getEndDate().getMonth();
    UUID processingPeriodId = shipmentExtensionRequest.getShipment().getOrder().getProcessingPeriod().getId();
    ProcessingPeriodExtension processingPeriodExtension = processingPeriodExtensionRepository
        .findByProcessingPeriodId(processingPeriodId);
    if (processingPeriodExtension == null) {
      throw new NotFoundException(ERROR_PERIOD_NOT_FOUND);
    }
    List<Month> calculatedFulfillOrderMonth = siglusOrderService
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
    ShipmentDto confirmedShipmentDto = createOrderAndConfirmShipment(isSubOrder,
        shipmentExtensionRequest.getShipment(), true);
    fulfillLocationInfo(uniqueKeyMap, confirmedShipmentDto);
    List<ShipmentLineItemDto> shipmentLineItems = new ArrayList<>(uniqueKeyMap.values());
    saveToShipmentLineItemExtension(shipmentLineItems);
    savePodExtension(confirmedShipmentDto.getId(), shipmentExtensionRequest);
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
    return shipmentLineItemDto.getLot().getId() + FieldConstants.SEPARATOR + shipmentLineItemDto.getOrderable().getId();
  }

  @Transactional
  public ShipmentDto createSubOrderAndShipmentForFc(ShipmentDto shipmentDto) {
    createSubOrder(shipmentDto, false);
    return createShipment(shipmentDto, false, true);
  }

  ShipmentDto createSubOrderAndShipment(boolean isSubOrder, ShipmentDto shipmentDto, boolean isByLocation) {
    if (isSubOrder) {
      createSubOrder(shipmentDto, true);
    }
    return createShipment(shipmentDto, isByLocation, false);
  }

  private ShipmentDto createOrderAndConfirmShipment(boolean isSubOrder, ShipmentDto shipmentDto,
      boolean isByLocation) {
    OrderDto orderDto = orderController.getOrder(shipmentDto.getOrder().getId(), null);
    validateOrderStatus(orderDto);
    if (siglusOrderService.needCloseOrder(orderDto)) {
      if (siglusOrderService.isSuborder(orderDto.getExternalId())) {
        throw new ValidationMessageException(SHIPMENT_ORDER_STATUS_INVALID);
      }
      isSubOrder = false;
    }
    updateOrderLineItems(shipmentDto.getOrder());
    return createSubOrderAndShipment(isSubOrder, shipmentDto, isByLocation);
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

  private ShipmentDto createShipment(ShipmentDto shipmentDto, boolean isByLocation, boolean isFcRequest) {
    Set<UUID> skippedOrderLineItemIds = getSkippedOrderLineItemIds(shipmentDto);
    removeSkippedOrderLineItemsAndExtensions(skippedOrderLineItemIds, shipmentDto.getOrder().getId());
    Set<UUID> skippedOrderableIds = getSkippedOrderableIds(shipmentDto);
    shipmentDto.lineItems().removeIf(lineItem -> skippedOrderableIds.contains(lineItem.getOrderable().getId()));
    return shipmentController.createShipment(shipmentDto, isByLocation, isFcRequest);
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

  private void removeSkippedOrderLineItemsAndExtensions(Set<UUID> skippedOrderLineItemIds, UUID orderId) {
    if (CollectionUtils.isEmpty(skippedOrderLineItemIds)) {
      return;
    }
    Order order = orderRepository.findOne(orderId);
    order.getOrderLineItems().removeIf(orderLineItem -> skippedOrderLineItemIds.contains(orderLineItem.getId()));
    orderRepository.save(order);
    List<OrderLineItemExtension> extensions = lineItemExtensionRepository.findByOrderLineItemIdIn(
        skippedOrderLineItemIds);
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

  public void calculateSubOrderPartialFulfilledValue(Map<UUID, List<Importer>> groupShipment,
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
      ShipmentLineItemsExtension shipmentLineItemsByLocation = ShipmentLineItemsExtension
          .builder()
          .shipmentLineItemId(lineItemId)
          .locationCode(getIfNonNull(LocationDto::getLocationCode, shipmentLineItemDto.getLocation()))
          .area(getIfNonNull(LocationDto::getArea, shipmentLineItemDto.getLocation()))
          .quantityShipped(shipmentLineItemDto.getQuantityShipped().intValue())
          .build();
      shipmentLineItemsByLocations.add(shipmentLineItemsByLocation);
    });
    log.info("create shipment line item by location, size: {}", shipmentLineItemsByLocations.size());
    shipmentLineItemsExtensionRepository.save(shipmentLineItemsByLocations);
  }
}
