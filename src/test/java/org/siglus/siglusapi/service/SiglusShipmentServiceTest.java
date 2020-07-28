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
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.siglus.common.i18n.MessageKeys.ERROR_SUB_ORDER_LINE_ITEM;
import static org.siglus.common.i18n.MessageKeys.SHIPMENT_ORDER_STATUS_INVALID;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.fulfillment.domain.Order;
import org.openlmis.fulfillment.domain.OrderLineItem;
import org.openlmis.fulfillment.domain.OrderStatus;
import org.openlmis.fulfillment.domain.VersionEntityReference;
import org.openlmis.fulfillment.repository.OrderRepository;
import org.openlmis.fulfillment.service.referencedata.OrderableDto;
import org.openlmis.fulfillment.service.referencedata.ProcessingPeriodDto;
import org.openlmis.fulfillment.service.referencedata.ProcessingScheduleDto;
import org.openlmis.fulfillment.util.Pagination;
import org.openlmis.fulfillment.web.OrderController;
import org.openlmis.fulfillment.web.shipment.ShipmentController;
import org.openlmis.fulfillment.web.shipment.ShipmentDto;
import org.openlmis.fulfillment.web.shipment.ShipmentLineItemDto;
import org.openlmis.fulfillment.web.util.OrderDto;
import org.openlmis.fulfillment.web.util.OrderLineItemDto;
import org.openlmis.fulfillment.web.util.OrderObjectReferenceDto;
import org.openlmis.fulfillment.web.util.VersionObjectReferenceDto;
import org.siglus.common.exception.ValidationMessageException;
import org.siglus.siglusapi.domain.OrderLineItemExtension;
import org.siglus.siglusapi.repository.OrderLineItemExtensionRepository;
import org.siglus.siglusapi.service.client.SiglusProcessingPeriodReferenceDataService;
import org.springframework.data.domain.Page;
import org.springframework.test.util.ReflectionTestUtils;

@SuppressWarnings("PMD.TooManyMethods")
@RunWith(MockitoJUnitRunner.class)
public class SiglusShipmentServiceTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Captor
  private ArgumentCaptor<Order> orderArgumentCaptor;

  @Captor
  private ArgumentCaptor<List<OrderLineItemExtension>> lineItemExtensionArgumentCaptor;

  @Captor
  private ArgumentCaptor<List<OrderLineItemDto>> orderLineItemDtoArgumentCaptor;

  @Captor
  private ArgumentCaptor<ShipmentDto> shipmentDtoArgumentCaptor;

  @InjectMocks
  private SiglusShipmentService siglusShipmentService;

  @Mock
  private OrderRepository orderRepository;

  @Mock
  private ShipmentController shipmentController;

  @Mock
  private OrderLineItemExtensionRepository lineItemExtensionRepository;

  @Mock
  private SiglusNotificationService notificationService;

  @Mock
  private SiglusOrderService siglusOrderService;

  @Mock
  private OrderController orderController;

  @Mock
  private SiglusShipmentDraftService draftService;

  @Mock
  private SiglusProcessingPeriodReferenceDataService periodService;


  private UUID orderId = UUID.randomUUID();

  private UUID orderableId = UUID.randomUUID();

  private UUID lineItemId = UUID.randomUUID();

  @Before
  public void prepare() {
    OrderDto order = new OrderDto();

    order.setStatus(OrderStatus.FULFILLING);
    order.setProcessingPeriod(buildProcessingPeriod());
    when(orderController.getOrder(any(UUID.class), any())).thenReturn(order);
    Page<org.openlmis.requisition.dto.ProcessingPeriodDto> periodDtos = Pagination
        .getPage(Collections.EMPTY_LIST);
    when(periodService
        .searchProcessingPeriods(any(UUID.class), any(), any(), any(), any(), any(), any()))
        .thenReturn(periodDtos);
    ReflectionTestUtils.setField(siglusShipmentService, "timeZoneId", "UTC");
  }

  @Test(expected = ValidationMessageException.class)
  public void shouldThrowOrderCloseWhenOrderStatusClosed() {
    // given
    OrderDto order = new OrderDto();
    order.setStatus(OrderStatus.CLOSED);
    when(orderController.getOrder(any(UUID.class), any())).thenReturn(order);
    ShipmentDto shipmentDto = new ShipmentDto();
    OrderObjectReferenceDto orderDto = new OrderObjectReferenceDto(orderId);
    shipmentDto.setOrder(orderDto);

    // when
    siglusShipmentService.createOrderAndShipment(false, shipmentDto);

    // then
    expectedException.expect(ValidationMessageException.class);
    expectedException.expectMessage(SHIPMENT_ORDER_STATUS_INVALID);
  }

  @Test(expected = ValidationMessageException.class)
  public void shouldThrowOrderCloseWhenCurrentDateIsAfterNextPeriodEndDate() {
    // given
    OrderDto orderDto = new OrderDto();
    orderDto.setId(orderId);
    orderDto.setStatus(OrderStatus.FULFILLING);
    orderDto.setProcessingPeriod(buildProcessingPeriod());
    when(orderController.getOrder(any(UUID.class), any())).thenReturn(orderDto);
    ShipmentDto shipmentDto = new ShipmentDto();
    OrderObjectReferenceDto orderReferenceDto = new OrderObjectReferenceDto(orderId);
    shipmentDto.setOrder(orderReferenceDto);
    org.openlmis.requisition.dto.ProcessingPeriodDto dto =
        new org.openlmis.requisition.dto.ProcessingPeriodDto();
    dto.setEndDate(LocalDate.now().minusDays(10));
    Page<org.openlmis.requisition.dto.ProcessingPeriodDto> periodDtos = Pagination
        .getPage(Arrays.asList(dto));
    when(periodService
        .searchProcessingPeriods(any(UUID.class), any(), any(), any(), any(), any(), any()))
        .thenReturn(periodDtos);
    Order order = new Order();
    when(orderRepository.findOne(orderId)).thenReturn(order);

    // when
    siglusShipmentService.createOrderAndShipment(false, shipmentDto);

    // then
    verify(draftService).deleteOrderLineItemAndInitialedExtension(order);
    expectedException.expect(ValidationMessageException.class);
    expectedException.expectMessage(SHIPMENT_ORDER_STATUS_INVALID);
  }

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
    siglusShipmentService.createOrderAndShipment(false, shipmentDto);

    // then
    verify(orderRepository).save(orderArgumentCaptor.capture());
    verify(lineItemExtensionRepository).delete(lineItemExtensionArgumentCaptor.capture());
    verify(shipmentController).createShipment(shipmentDtoArgumentCaptor.capture());
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
    siglusShipmentService.createOrderAndShipment(false, shipmentDto);

    // then
    verify(orderRepository).save(orderArgumentCaptor.capture());
    verify(lineItemExtensionRepository).delete(lineItemExtensionArgumentCaptor.capture());
    verify(shipmentController).createShipment(shipmentDtoArgumentCaptor.capture());
    Order orderToSave = orderArgumentCaptor.getValue();
    List<OrderLineItemExtension> lineItemExtensionsToDelete = lineItemExtensionArgumentCaptor
        .getValue();
    ShipmentDto shipmentDtoToSave = shipmentDtoArgumentCaptor.getValue();
    assertTrue(CollectionUtils.isNotEmpty(orderToSave.getOrderLineItems()));
    assertTrue(CollectionUtils.isEmpty(lineItemExtensionsToDelete));
    assertTrue(CollectionUtils.isNotEmpty(shipmentDtoToSave.getLineItems()));
    verify(notificationService).postConfirmShipment(any());
  }

  @Test
  public void shouldCreateSubOrderWhenLineItemShippedQualityLessOrderQuality() {
    // given
    ShipmentDto shipmentDto = createShipmentDto();
    Order order = new Order();
    OrderLineItem lineItem = new OrderLineItem();
    lineItem.setId(lineItemId);
    order.setOrderLineItems(newArrayList(lineItem));
    when(orderRepository.findOne(shipmentDto.getOrder().getId())).thenReturn(order);
    when(lineItemExtensionRepository.findByOrderLineItemIdIn(newHashSet()))
        .thenReturn(newArrayList());

    // when
    siglusShipmentService.createOrderAndShipment(true, shipmentDto);

    // then
    verify(siglusOrderService).createSubOrder(any(OrderObjectReferenceDto.class),
        orderLineItemDtoArgumentCaptor.capture());
    List<OrderLineItemDto> lineItemDtos = orderLineItemDtoArgumentCaptor.getValue();
    assertEquals(1, lineItemDtos.size());
    assertEquals(Long.valueOf(10), lineItemDtos.get(0).getPartialFulfilledQuantity());
    assertEquals(Long.valueOf(40), lineItemDtos.get(0).getOrderedQuantity());
    verify(notificationService).postConfirmShipment(any());
  }

  @Test(expected = ValidationMessageException.class)
  public void shouldDontCreateSubOrderIfShippedQualityGreaterOrderQualityWhenSubLineItemEmpty() {
    ShipmentDto shipmentDto = createShipmentDto();
    VersionObjectReferenceDto orderReferenceDto = new VersionObjectReferenceDto(orderableId,
        "", "", Long.valueOf(1));
    ShipmentLineItemDto shipmentLineItem1 = new ShipmentLineItemDto();
    shipmentLineItem1.setOrderable(orderReferenceDto);
    shipmentLineItem1.setQuantityShipped(Long.valueOf(50));
    shipmentDto.setLineItems(Arrays.asList(shipmentLineItem1));

    // when
    siglusShipmentService.createOrderAndShipment(true, shipmentDto);

    // then
    expectedException.expect(ValidationMessageException.class);
    expectedException.expectMessage(ERROR_SUB_ORDER_LINE_ITEM);
  }

  @Test(expected = ValidationMessageException.class)
  public void shouldDontCreateSubOrderIfLineItemSkipWhenSubLineItemEmpty() {
    OrderLineItemDto lineItem = new OrderLineItemDto();
    OrderableDto orderableDto = new OrderableDto();
    orderableDto.setId(orderableId);
    lineItem.setOrderable(orderableDto);
    lineItem.setSkipped(true);
    ShipmentDto shipmentDto = createShipmentDto();
    OrderObjectReferenceDto order = shipmentDto.getOrder();
    order.setOrderLineItems(Arrays.asList(lineItem));
    shipmentDto.setOrder(order);
    shipmentDto.setLineItems(new ArrayList<>());

    // when
    siglusShipmentService.createOrderAndShipment(true, shipmentDto);

    // then
    expectedException.expect(ValidationMessageException.class);
    expectedException.expectMessage(ERROR_SUB_ORDER_LINE_ITEM);
  }


  @Test
  public void shouldCreateSubOrderWhenLineItemOrderQualityGreaterThan0AndShipmentEmpty() {
    ShipmentDto shipmentDto = createShipmentDto();
    shipmentDto.setLineItems(new ArrayList<>());
    Order order = new Order();
    OrderLineItem lineItem = new OrderLineItem();
    lineItem.setId(lineItemId);
    order.setOrderLineItems(newArrayList(lineItem));
    when(orderRepository.findOne(shipmentDto.getOrder().getId())).thenReturn(order);
    when(lineItemExtensionRepository.findByOrderLineItemIdIn(newHashSet()))
        .thenReturn(newArrayList());

    // when
    siglusShipmentService.createOrderAndShipment(true, shipmentDto);

    // then
    verify(siglusOrderService).createSubOrder(any(OrderObjectReferenceDto.class),
        orderLineItemDtoArgumentCaptor.capture());
    List<OrderLineItemDto> lineItemDtos = orderLineItemDtoArgumentCaptor.getValue();
    assertEquals(1, lineItemDtos.size());
    assertEquals(Long.valueOf(0), lineItemDtos.get(0).getPartialFulfilledQuantity());
    assertEquals(Long.valueOf(40), lineItemDtos.get(0).getOrderedQuantity());
  }

  private ShipmentDto createShipmentDto() {
    OrderLineItemDto lineItem = new OrderLineItemDto();
    lineItem.setOrderedQuantity(Long.valueOf(40));
    OrderableDto orderableDto = new OrderableDto();
    orderableDto.setId(orderableId);
    lineItem.setOrderable(orderableDto);
    OrderObjectReferenceDto order = new OrderObjectReferenceDto(UUID.randomUUID());
    order.setOrderLineItems(Arrays.asList(lineItem));
    ShipmentDto shipmentDto = new ShipmentDto();
    shipmentDto.setOrder(order);
    VersionObjectReferenceDto orderReferenceDto = new VersionObjectReferenceDto(orderableId,
        "", "", Long.valueOf(1));
    ShipmentLineItemDto shipmentLineItem1 = new ShipmentLineItemDto();
    shipmentLineItem1.setOrderable(orderReferenceDto);
    shipmentLineItem1.setQuantityShipped(Long.valueOf(5));
    ShipmentLineItemDto shipmentLineItem2 = new ShipmentLineItemDto();
    shipmentLineItem2.setOrderable(orderReferenceDto);
    shipmentLineItem2.setQuantityShipped(Long.valueOf(5));
    shipmentDto.setLineItems(Arrays.asList(shipmentLineItem1, shipmentLineItem2));
    return shipmentDto;
  }

  public ProcessingPeriodDto buildProcessingPeriod() {
    ProcessingPeriodDto dto = new ProcessingPeriodDto();
    ProcessingScheduleDto processingSchedule = new ProcessingScheduleDto();
    processingSchedule.setId(UUID.randomUUID());
    dto.setProcessingSchedule(processingSchedule);
    dto.setStartDate(LocalDate.now());
    dto.setEndDate(LocalDate.now());
    return dto;
  }
}
