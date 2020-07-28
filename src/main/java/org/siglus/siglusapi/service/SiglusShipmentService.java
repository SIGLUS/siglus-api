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
import static org.siglus.common.i18n.MessageKeys.ERROR_SUB_ORDER_LINE_ITEM;
import static org.siglus.common.i18n.MessageKeys.SHIPMENT_ORDER_STATUS_INVALID;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.openlmis.fulfillment.domain.Order;
import org.openlmis.fulfillment.domain.OrderStatus;
import org.openlmis.fulfillment.domain.ShipmentLineItem;
import org.openlmis.fulfillment.domain.ShipmentLineItem.Importer;
import org.openlmis.fulfillment.repository.OrderRepository;
import org.openlmis.fulfillment.service.referencedata.ProcessingPeriodDto;
import org.openlmis.fulfillment.web.OrderController;
import org.openlmis.fulfillment.web.ValidationException;
import org.openlmis.fulfillment.web.shipment.ShipmentController;
import org.openlmis.fulfillment.web.shipment.ShipmentDto;
import org.openlmis.fulfillment.web.util.OrderDto;
import org.openlmis.fulfillment.web.util.OrderLineItemDto;
import org.openlmis.fulfillment.web.util.OrderObjectReferenceDto;
import org.siglus.common.exception.ValidationMessageException;
import org.siglus.common.util.Message;
import org.siglus.siglusapi.domain.OrderLineItemExtension;
import org.siglus.siglusapi.repository.OrderLineItemExtensionRepository;
import org.siglus.siglusapi.service.client.SiglusProcessingPeriodReferenceDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SiglusShipmentService {

  @Autowired
  private OrderRepository orderRepository;

  @Autowired
  private OrderLineItemExtensionRepository lineItemExtensionRepository;

  @Autowired
  private ShipmentController shipmentController;

  @Autowired
  private SiglusOrderService siglusOrderService;

  @Autowired
  private SiglusNotificationService notificationService;

  @Autowired
  private OrderController orderController;

  @Autowired
  private SiglusShipmentDraftService draftService;

  @Autowired
  private SiglusProcessingPeriodReferenceDataService periodService;

  @Value("${time.zoneId}")
  private String timeZoneId;

  public ShipmentDto createOrderAndShipment(boolean isSubOrder, ShipmentDto shipmentDto) {
    OrderDto orderDto = orderController.getOrder(shipmentDto.getOrder().getId(), null);
    validateOrderStatus(orderDto);
    if (currentDateIsAfterNextPeriodEndDate(orderDto)) {
      deleteExtensionForCurrentDateAfterNextPeriod(orderDto);
      throw new ValidationException(SHIPMENT_ORDER_STATUS_INVALID);
    }
    return createSubOrderAndShipment(isSubOrder, shipmentDto);
  }

  @Transactional
  ShipmentDto createSubOrderAndShipment(boolean isSubOrder, ShipmentDto shipmentDto) {
    if (isSubOrder) {
      createSubOrder(shipmentDto);
    }
    ShipmentDto shipment = createShipment(shipmentDto);
    notificationService.postConfirmShipment(shipment);
    return shipment;
  }


  @Transactional
  void deleteExtensionForCurrentDateAfterNextPeriod(OrderDto orderDto) {
      Order order = orderRepository.findOne(orderDto.getId());
      draftService.deleteOrderLineItemAndInitialedExtension(order);
      order.setStatus(OrderStatus.CLOSED);
      orderRepository.save(order);
  }

  private void validateOrderStatus(OrderDto orderDto) {
    if (orderDto.getStatus().equals(OrderStatus.CLOSED)) {
      throw new ValidationException(SHIPMENT_ORDER_STATUS_INVALID);
    }
  }

  private boolean currentDateIsAfterNextPeriodEndDate(OrderDto orderDto) {
    ProcessingPeriodDto period = orderDto.getProcessingPeriod();
    Pageable pageable = new PageRequest(0, 1);
    Page<org.openlmis.requisition.dto.ProcessingPeriodDto> periodDtos = periodService
        .searchProcessingPeriods(period.getProcessingSchedule().getId(),
            null,  null, period.getEndDate().plusDays(1),
            null,
            null, pageable);
    LocalDate currentDate = LocalDate.now(ZoneId.of(timeZoneId));
    return periodDtos.getContent().size() >= 1 && periodDtos.getContent().get(0).getEndDate()
        .isBefore(currentDate);
  }

  private void createSubOrder(ShipmentDto shipmentDto) {
    Set<UUID> skippedOrderLineItemIds = getSkippedOrderLineItemIds(shipmentDto);
    Map<UUID, List<ShipmentLineItem.Importer>> groupShipment = shipmentDto.getLineItems().stream()
        .collect(Collectors.groupingBy(lineItem -> lineItem.getOrderableIdentity().getId()));
    OrderObjectReferenceDto order = shipmentDto.getOrder();
    List<OrderLineItemDto> orderLineItems = order.getOrderLineItems();
    List<OrderLineItemDto> subOrderLineItems = getSubOrderLineItemDtos(skippedOrderLineItemIds,
        groupShipment, orderLineItems);
    if (subOrderLineItems.isEmpty()) {
      throw new ValidationMessageException(
          new Message(ERROR_SUB_ORDER_LINE_ITEM));
    }
    siglusOrderService.createSubOrder(order, subOrderLineItems);
  }

  private ShipmentDto createShipment(ShipmentDto shipmentDto) {
    Set<UUID> skippedOrderLineItemIds = getSkippedOrderLineItemIds(shipmentDto);
    removeSkippedOrderLineItemsAndExtensions(skippedOrderLineItemIds,
        shipmentDto.getOrder().getId());
    Set<UUID> skippedOrderableIds = getSkippedOrderableIds(shipmentDto);
    shipmentDto.lineItems()
        .removeIf(lineItem -> skippedOrderableIds.contains(lineItem.getOrderable().getId()));

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
      if (!skippedOrderLineItemIds.contains(dto.getId()) && dto.getOrderedQuantity() > 0) {
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

}
