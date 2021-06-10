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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anySet;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.fulfillment.domain.Order;
import org.openlmis.fulfillment.domain.OrderLineItem;
import org.openlmis.fulfillment.repository.OrderRepository;
import org.openlmis.fulfillment.web.shipmentdraft.ShipmentDraftController;
import org.openlmis.fulfillment.web.shipmentdraft.ShipmentDraftDto;
import org.openlmis.fulfillment.web.util.OrderLineItemDto;
import org.openlmis.fulfillment.web.util.OrderObjectReferenceDto;
import org.siglus.siglusapi.domain.OrderLineItemExtension;
import org.siglus.siglusapi.repository.OrderLineItemExtensionRepository;
import org.siglus.siglusapi.repository.OrderLineItemRepository;
import org.siglus.siglusapi.service.client.SiglusShipmentDraftFulfillmentService;

@RunWith(MockitoJUnitRunner.class)
public class SiglusShipmentDraftServiceTest {

  @Captor
  private ArgumentCaptor<List<OrderLineItemExtension>> lineItemExtensionsArgumentCaptor;

  @InjectMocks
  private SiglusShipmentDraftService siglusShipmentDraftService;

  @Mock
  private SiglusShipmentDraftFulfillmentService siglusShipmentDraftFulfillmentService;

  @Mock
  private OrderLineItemExtensionRepository lineItemExtensionRepository;

  @Mock
  private SiglusOrderService siglusOrderService;

  @Mock
  private OrderLineItemRepository orderLineItemRepository;

  @Mock
  private ShipmentDraftController draftController;

  @Mock
  private OrderRepository orderRepository;

  private final UUID draftId = UUID.randomUUID();

  private final UUID orderId = UUID.randomUUID();

  private final UUID lineItemId = UUID.randomUUID();

  @Test
  public void shouldUpdateLineItemExtensionWhenUpdateShipmentDraftIfExtensionExist() {
    // given
    OrderLineItemDto lineItemDto = new OrderLineItemDto();
    lineItemDto.setId(lineItemId);
    lineItemDto.setSkipped(true);
    OrderObjectReferenceDto order = new OrderObjectReferenceDto(orderId);
    order.setOrderLineItems(newArrayList(lineItemDto));
    ShipmentDraftDto draftDto = new ShipmentDraftDto();
    draftDto.setOrder(order);
    when(draftController.updateShipmentDraft(draftId, draftDto))
        .thenReturn(draftDto);
    OrderLineItemExtension extension = OrderLineItemExtension.builder()
        .orderLineItemId(lineItemId)
        .skipped(false)
        .build();
    when(lineItemExtensionRepository.findByOrderLineItemIdIn(newHashSet(lineItemId)))
        .thenReturn(newArrayList(extension));
    when(siglusOrderService.updateOrderLineItems(draftDto)).thenReturn(newHashSet(lineItemId));

    // when
    siglusShipmentDraftService.updateShipmentDraft(draftId, draftDto);

    // then
    verify(lineItemExtensionRepository).save(lineItemExtensionsArgumentCaptor.capture());
    List<OrderLineItemExtension> lineItemExtensions = lineItemExtensionsArgumentCaptor
        .getValue();
    lineItemExtensions.forEach(lineItemExtension -> {
      assertTrue(lineItemExtension.isSkipped());
      assertFalse(lineItemExtension.isAdded());
    });
  }

  @Test
  public void shouldCreateLineItemExtensionWhenUpdateShipmentDraftIfExtensionNotExist() {
    // given
    OrderLineItemDto lineItemDto = new OrderLineItemDto();
    lineItemDto.setId(lineItemId);
    lineItemDto.setSkipped(true);
    OrderObjectReferenceDto order = new OrderObjectReferenceDto(orderId);
    order.setOrderLineItems(newArrayList(lineItemDto));
    ShipmentDraftDto draftDto = new ShipmentDraftDto();
    draftDto.setOrder(order);
    when(draftController.updateShipmentDraft(draftId, draftDto))
        .thenReturn(draftDto);
    when(lineItemExtensionRepository.findByOrderLineItemIdIn(newHashSet(lineItemId)))
        .thenReturn(newArrayList());
    when(siglusOrderService.updateOrderLineItems(draftDto)).thenReturn(newHashSet(lineItemId));

    // when
    siglusShipmentDraftService.updateShipmentDraft(draftId, draftDto);

    // then
    verify(lineItemExtensionRepository).save(lineItemExtensionsArgumentCaptor.capture());
    List<OrderLineItemExtension> lineItemExtensions = lineItemExtensionsArgumentCaptor
        .getValue();
    lineItemExtensions.forEach(lineItemExtension -> {
      assertTrue(lineItemExtension.isSkipped());
      assertTrue(lineItemExtension.isAdded());
    });
  }

  @Test
  public void shouldDeleteLineItemExtensionWhenDeleteShipmentDraft() {
    // given
    OrderObjectReferenceDto orderDto = new OrderObjectReferenceDto(orderId);
    ShipmentDraftDto draftDto = new ShipmentDraftDto();
    draftDto.setOrder(orderDto);
    when(siglusShipmentDraftFulfillmentService.searchShipmentDraft(draftId))
        .thenReturn(draftDto);
    OrderLineItem lineItem = new OrderLineItem();
    lineItem.setId(lineItemId);
    Order order = new Order();
    order.setId(orderId);
    order.setOrderLineItems(newArrayList(lineItem));
    when(orderRepository.findOne(orderId)).thenReturn(order);
    OrderLineItemExtension extension = OrderLineItemExtension.builder()
        .orderLineItemId(lineItemId)
        .skipped(true)
        .added(true)
        .build();
    when(lineItemExtensionRepository.findByOrderId(orderId))
        .thenReturn(newArrayList(extension));

    // when
    siglusShipmentDraftService.deleteShipmentDraft(draftId);

    // then
    verify(lineItemExtensionRepository).delete(lineItemExtensionsArgumentCaptor.capture());
    verify(orderLineItemRepository).delete(anySet());
    Set<OrderLineItemExtension> lineItemExtensions = (Set<OrderLineItemExtension>)
        lineItemExtensionsArgumentCaptor.getValue();
    assertEquals(1, lineItemExtensions.size());
  }

  @Test
  public void shouldNotDeleteLineItemExtensionIfIsAddedFalseWhenDeleteShipmentDraft() {
    // given
    OrderObjectReferenceDto orderDto = new OrderObjectReferenceDto(orderId);
    ShipmentDraftDto draftDto = new ShipmentDraftDto();
    draftDto.setOrder(orderDto);
    when(siglusShipmentDraftFulfillmentService.searchShipmentDraft(draftId))
        .thenReturn(draftDto);
    OrderLineItem lineItem = new OrderLineItem();
    lineItem.setId(lineItemId);
    Order order = new Order();
    order.setId(orderId);
    order.setOrderLineItems(newArrayList(lineItem));
    when(orderRepository.findOne(orderId)).thenReturn(order);
    OrderLineItemExtension extension = OrderLineItemExtension.builder()
        .orderLineItemId(lineItemId)
        .skipped(true)
        .added(false)
        .partialFulfilledQuantity((long) 10)
        .build();
    when(lineItemExtensionRepository.findByOrderId(orderId)).thenReturn(newArrayList(extension));

    // when
    siglusShipmentDraftService.deleteShipmentDraft(draftId);

    // then
    verify(lineItemExtensionRepository, times(0)).delete(anySet());
    verify(lineItemExtensionRepository).save(lineItemExtensionsArgumentCaptor.capture());
    List<OrderLineItemExtension> lineItemExtensions = lineItemExtensionsArgumentCaptor.getValue();
    assertEquals(1, lineItemExtensions.size());
    assertEquals(Long.valueOf(10),
        lineItemExtensions.iterator().next().getPartialFulfilledQuantity());
    assertEquals(false, lineItemExtensions.iterator().next().isSkipped());
  }

}
