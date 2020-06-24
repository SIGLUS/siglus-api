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
import static com.google.common.collect.Sets.newHashSet;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.fulfillment.domain.Order;
import org.openlmis.fulfillment.domain.OrderLineItem;
import org.openlmis.fulfillment.domain.VersionEntityReference;
import org.openlmis.fulfillment.repository.OrderRepository;
import org.openlmis.fulfillment.service.referencedata.OrderableDto;
import org.openlmis.fulfillment.web.shipment.ShipmentDto;
import org.openlmis.fulfillment.web.shipment.ShipmentLineItemDto;
import org.openlmis.fulfillment.web.util.OrderLineItemDto;
import org.openlmis.fulfillment.web.util.OrderObjectReferenceDto;
import org.siglus.siglusapi.domain.OrderLineItemExtension;
import org.siglus.siglusapi.repository.OrderLineItemExtensionRepository;
import org.siglus.siglusapi.service.client.SiglusShipmentFulfillmentService;

@RunWith(MockitoJUnitRunner.class)
public class SiglusShipmentServiceTest {

  @Captor
  private ArgumentCaptor<Order> orderArgumentCaptor;

  @Captor
  private ArgumentCaptor<List<OrderLineItemExtension>> lineItemExtensionArgumentCaptor;

  @Captor
  private ArgumentCaptor<ShipmentDto> shipmentDtoArgumentCaptor;

  @InjectMocks
  private SiglusShipmentService siglusShipmentService;

  @Mock
  private SiglusShipmentFulfillmentService siglusShipmentFulfillmentService;

  @Mock
  private OrderRepository orderRepository;

  @Mock
  private OrderLineItemExtensionRepository lineItemExtensionRepository;

  private UUID orderId = UUID.randomUUID();

  private UUID orderableId = UUID.randomUUID();

  private UUID lineItemId = UUID.randomUUID();

  @Test
  public void shouldRemoveSkippedLineItemsWhenCreateShipment() {
    // given
    OrderableDto orderableDto = new OrderableDto();
    orderableDto.setId(orderableId);
    OrderLineItemDto lineItemDto = new OrderLineItemDto();
    lineItemDto.setId(lineItemId);
    lineItemDto.setOrderable(orderableDto);
    lineItemDto.setSkipped(true);
    OrderObjectReferenceDto orderDto = new OrderObjectReferenceDto(orderId);
    orderDto.setOrderLineItems(newArrayList(lineItemDto));
    ShipmentLineItemDto shipmentLineItemDto = new ShipmentLineItemDto();
    shipmentLineItemDto.setOrderable(orderableDto);
    ShipmentDto shipmentDto = new ShipmentDto();
    shipmentDto.setOrder(orderDto);
    shipmentDto.setLineItems(newArrayList(shipmentLineItemDto));
    OrderLineItem lineItem = new OrderLineItem();
    lineItem.setId(lineItemId);
    lineItem.setOrderable(new VersionEntityReference(orderableId, 1L));
    Order order = new Order();
    order.setOrderLineItems(newArrayList(lineItem));
    when(orderRepository.findOne(orderId)).thenReturn(order);
    OrderLineItemExtension orderLineItemExtension = OrderLineItemExtension.builder()
        .orderLineItemId(lineItemId).build();
    when(lineItemExtensionRepository.findByOrderLineItemIdIn(newHashSet(lineItemId)))
        .thenReturn(newArrayList(orderLineItemExtension));

    // when
    siglusShipmentService.createShipment(shipmentDto);

    // then
    verify(orderRepository).save(orderArgumentCaptor.capture());
    verify(lineItemExtensionRepository).delete(lineItemExtensionArgumentCaptor.capture());
    verify(siglusShipmentFulfillmentService).createShipment(shipmentDtoArgumentCaptor.capture());
    Order orderToSave = orderArgumentCaptor.getValue();
    List<OrderLineItemExtension> lineItemExtensionsToDelete = lineItemExtensionArgumentCaptor
        .getValue();
    ShipmentDto shipmentDtoToSave = shipmentDtoArgumentCaptor.getValue();
    assertTrue(CollectionUtils.isEmpty(orderToSave.getOrderLineItems()));
    assertTrue(CollectionUtils.isNotEmpty(lineItemExtensionsToDelete));
    assertTrue(CollectionUtils.isEmpty(shipmentDtoToSave.getLineItems()));
  }

  @Test
  public void shouldNotRemoveLineItemsWhenCreateShipmentIfNotSkip() {
    // given
    OrderableDto orderableDto = new OrderableDto();
    orderableDto.setId(orderableId);
    OrderLineItemDto lineItemDto = new OrderLineItemDto();
    lineItemDto.setId(lineItemId);
    lineItemDto.setOrderable(orderableDto);
    lineItemDto.setSkipped(false);
    OrderObjectReferenceDto orderDto = new OrderObjectReferenceDto(orderId);
    orderDto.setOrderLineItems(newArrayList(lineItemDto));
    ShipmentLineItemDto shipmentLineItemDto = new ShipmentLineItemDto();
    shipmentLineItemDto.setOrderable(orderableDto);
    ShipmentDto shipmentDto = new ShipmentDto();
    shipmentDto.setOrder(orderDto);
    shipmentDto.setLineItems(newArrayList(shipmentLineItemDto));
    OrderLineItem lineItem = new OrderLineItem();
    lineItem.setOrderable(new VersionEntityReference(orderableId, 1L));
    Order order = new Order();
    lineItem.setId(lineItemId);
    order.setOrderLineItems(newArrayList(lineItem));
    when(orderRepository.findOne(orderId)).thenReturn(order);
    when(lineItemExtensionRepository.findByOrderLineItemIdIn(newHashSet()))
        .thenReturn(newArrayList());

    // when
    siglusShipmentService.createShipment(shipmentDto);

    // then
    verify(orderRepository).save(orderArgumentCaptor.capture());
    verify(lineItemExtensionRepository).delete(lineItemExtensionArgumentCaptor.capture());
    verify(siglusShipmentFulfillmentService).createShipment(shipmentDtoArgumentCaptor.capture());
    Order orderToSave = orderArgumentCaptor.getValue();
    List<OrderLineItemExtension> lineItemExtensionsToDelete = lineItemExtensionArgumentCaptor
        .getValue();
    ShipmentDto shipmentDtoToSave = shipmentDtoArgumentCaptor.getValue();
    assertTrue(CollectionUtils.isNotEmpty(orderToSave.getOrderLineItems()));
    assertTrue(CollectionUtils.isEmpty(lineItemExtensionsToDelete));
    assertTrue(CollectionUtils.isNotEmpty(shipmentDtoToSave.getLineItems()));
  }
}
