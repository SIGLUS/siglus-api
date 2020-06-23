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

import java.util.Set;
import java.util.UUID;
import org.openlmis.fulfillment.domain.Order;
import org.openlmis.fulfillment.repository.OrderRepository;
import org.openlmis.fulfillment.web.shipment.ShipmentDto;
import org.openlmis.fulfillment.web.util.OrderLineItemDto;
import org.siglus.siglusapi.service.client.SiglusShipmentFulfillmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SiglusShipmentService {

  @Autowired
  private SiglusShipmentFulfillmentService siglusShipmentFulfillmentService;

  @Autowired
  private OrderRepository orderRepository;

  @Transactional
  public ShipmentDto createShipment(ShipmentDto shipmentDto) {
    Set<UUID> skippedOrderableIds = getSkippedOrderableIds(shipmentDto);
    removeSkippedOrderLineItems(skippedOrderableIds, shipmentDto.getOrder().getId());
    shipmentDto.lineItems()
        .removeIf(lineItem -> skippedOrderableIds.contains(lineItem.getOrderable().getId()));
    return siglusShipmentFulfillmentService.createShipment(shipmentDto);
  }

  private Set<UUID> getSkippedOrderableIds(ShipmentDto shipmentDto) {
    return shipmentDto.getOrder().getOrderLineItems().stream()
          .filter(OrderLineItemDto::isSkipped)
          .map(orderLineItemDto -> orderLineItemDto.getOrderable().getId())
          .collect(toSet());
  }

  private void removeSkippedOrderLineItems(Set<UUID> skippedOrderableIds, UUID orderId) {
    Order order = orderRepository.findOne(orderId);
    order.getOrderLineItems().removeIf(
        orderLineItem -> skippedOrderableIds.contains(orderLineItem.getOrderable().getId()));
    orderRepository.save(order);
  }
}
