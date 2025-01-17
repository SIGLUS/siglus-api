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
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_SUB_ORDER_LINE_ITEM;
import static org.siglus.siglusapi.i18n.MessageKeys.SHIPMENT_ORDER_STATUS_INVALID;

import com.google.common.collect.Lists;
import java.time.LocalDate;
import java.time.YearMonth;
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
import org.openlmis.fulfillment.domain.ProofOfDelivery;
import org.openlmis.fulfillment.domain.ProofOfDeliveryLineItem;
import org.openlmis.fulfillment.domain.ProofOfDeliveryStatus;
import org.openlmis.fulfillment.domain.Shipment;
import org.openlmis.fulfillment.domain.ShipmentLineItem;
import org.openlmis.fulfillment.domain.VersionEntityReference;
import org.openlmis.fulfillment.repository.OrderRepository;
import org.openlmis.fulfillment.service.referencedata.FacilityDto;
import org.openlmis.fulfillment.service.referencedata.OrderableDto;
import org.openlmis.fulfillment.service.referencedata.ProcessingPeriodDto;
import org.openlmis.fulfillment.service.referencedata.ProcessingScheduleDto;
import org.openlmis.fulfillment.web.OrderController;
import org.openlmis.fulfillment.web.shipment.LocationDto;
import org.openlmis.fulfillment.web.shipment.ShipmentController;
import org.openlmis.fulfillment.web.shipment.ShipmentDto;
import org.openlmis.fulfillment.web.shipment.ShipmentLineItemDto;
import org.openlmis.fulfillment.web.util.ObjectReferenceDto;
import org.openlmis.fulfillment.web.util.OrderDto;
import org.openlmis.fulfillment.web.util.OrderLineItemDto;
import org.openlmis.fulfillment.web.util.OrderObjectReferenceDto;
import org.openlmis.fulfillment.web.util.VersionObjectReferenceDto;
import org.openlmis.referencedata.domain.Lot;
import org.openlmis.requisition.service.RequisitionService;
import org.openlmis.stockmanagement.domain.card.StockCard;
import org.openlmis.stockmanagement.domain.card.StockCardLineItem;
import org.openlmis.stockmanagement.repository.StockCardLineItemRepository;
import org.openlmis.stockmanagement.repository.StockCardRepository;
import org.siglus.common.domain.ProcessingPeriodExtension;
import org.siglus.common.repository.ProcessingPeriodExtensionRepository;
import org.siglus.siglusapi.domain.OrderLineItemExtension;
import org.siglus.siglusapi.domain.PodExtension;
import org.siglus.siglusapi.domain.StockCardLineItemExtension;
import org.siglus.siglusapi.dto.UserDto;
import org.siglus.siglusapi.exception.BusinessDataException;
import org.siglus.siglusapi.exception.NotFoundException;
import org.siglus.siglusapi.exception.ValidationMessageException;
import org.siglus.siglusapi.repository.OrderLineItemExtensionRepository;
import org.siglus.siglusapi.repository.PodExtensionRepository;
import org.siglus.siglusapi.repository.ShipmentLineItemsExtensionRepository;
import org.siglus.siglusapi.repository.ShipmentsExtensionRepository;
import org.siglus.siglusapi.repository.SiglusLotRepository;
import org.siglus.siglusapi.repository.SiglusProofOfDeliveryRepository;
import org.siglus.siglusapi.repository.SiglusStockCardRepository;
import org.siglus.siglusapi.repository.StockCardLineItemExtensionRepository;
import org.siglus.siglusapi.util.SiglusAuthenticationHelper;
import org.siglus.siglusapi.web.request.ShipmentExtensionRequest;
import org.springframework.test.util.ReflectionTestUtils;

@SuppressWarnings({"PMD.TooManyMethods", "PMD.UnusedPrivateField"})
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
  private SiglusOrderService siglusOrderService;

  @Mock
  private OrderController orderController;

  @Mock
  private SiglusShipmentDraftService draftService;

  @Mock
  private ShipmentLineItemsExtensionRepository shipmentLineItemsExtensionRepository;

  @Mock
  private SiglusAuthenticationHelper authenticationHelper;

  @Mock
  private SiglusProofOfDeliveryRepository siglusProofOfDeliveryRepository;

  @Mock
  private PodExtensionRepository podExtensionRepository;

  @Mock
  private StockCardLineItemRepository stockCardLineItemRepository;

  @Mock
  private StockCardRepository stockCardRepository;

  @Mock
  private StockCardLineItemExtensionRepository stockCardLineItemExtensionRepository;

  @Mock
  private SiglusStockCardRepository siglusStockCardRepository;

  @Mock
  private ProcessingPeriodExtensionRepository processingPeriodExtensionRepository;

  @Mock
  private RequisitionService requisitionService;

  @Mock
  private ShipmentsExtensionRepository shipmentsExtensionRepository;

  @Mock
  private SiglusLotRepository siglusLotRepository;

  private final UUID orderId = UUID.randomUUID();

  private final UUID orderableId = UUID.randomUUID();

  private final UUID lineItemId = UUID.randomUUID();

  private final UUID facilityId = UUID.randomUUID();

  private final UUID processingPeriodId = UUID.randomUUID();

  private final UUID lotId = UUID.randomUUID();
  private final UUID lotId2 = UUID.randomUUID();

  private final UUID podId = UUID.randomUUID();

  private final String conferredBy = "conferredBy";

  private final String preparedBy = "preparedBy";

  private final UUID stockCardId = UUID.randomUUID();

  private final UUID stockCardLineItemId = UUID.randomUUID();

  @Before
  public void prepare() {
    OrderDto order = new OrderDto();

    order.setStatus(OrderStatus.FULFILLING);
    order.setProcessingPeriod(buildProcessingPeriod());
    when(orderController.getOrder(any(UUID.class), any())).thenReturn(order);
    when(siglusOrderService.needCloseOrder(any()))
        .thenReturn(false);

    ReflectionTestUtils.setField(siglusShipmentService, "fefoIndex",
        0.75);
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
    ShipmentExtensionRequest shipmentExtensionRequest = new ShipmentExtensionRequest();
    shipmentExtensionRequest.setShipment(shipmentDto);
    shipmentExtensionRequest.setConferredBy(conferredBy);
    shipmentExtensionRequest.setPreparedBy(preparedBy);

    // when
    siglusShipmentService.createOrderAndShipment(false, shipmentExtensionRequest);

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
    ShipmentExtensionRequest shipmentExtensionRequest = new ShipmentExtensionRequest();
    shipmentExtensionRequest.setShipment(shipmentDto);
    shipmentExtensionRequest.setConferredBy(conferredBy);
    shipmentExtensionRequest.setPreparedBy(preparedBy);
    org.openlmis.requisition.dto.ProcessingPeriodDto dto =
        new org.openlmis.requisition.dto.ProcessingPeriodDto();
    dto.setEndDate(LocalDate.now().minusDays(10));
    when(siglusOrderService.needCloseOrder(any())).thenReturn(true);
    when(siglusOrderService.isSuborder(any())).thenReturn(true);
    Order order = new Order();
    when(orderRepository.findOne(orderId)).thenReturn(order);

    // when
    siglusShipmentService.createOrderAndShipment(false, shipmentExtensionRequest);

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
    shipmentLineItemDto.setStockOnHand(5L);
    shipmentLineItemDto.setQuantityShipped(5L);
    shipmentLineItemDto.setLot(new ObjectReferenceDto(lotId));
    ShipmentDto shipmentDto = new ShipmentDto();
    shipmentDto.setOrder(orderDto);
    shipmentDto.setLineItems(newArrayList(shipmentLineItemDto));
    ShipmentExtensionRequest shipmentExtensionRequest = new ShipmentExtensionRequest();
    shipmentExtensionRequest.setShipment(shipmentDto);
    shipmentExtensionRequest.setConferredBy(conferredBy);
    shipmentExtensionRequest.setPreparedBy(preparedBy);
    OrderLineItem lineItem = new OrderLineItem();
    lineItem.setId(lineItemId);
    lineItem.setOrderable(new VersionEntityReference(orderableId, 1L));
    Order order = new Order();
    order.setOrderLineItems(newArrayList(lineItem));
    Lot lot = new Lot();
    lot.setId(lotId);
    lot.setExpirationDate(LocalDate.now());
    when(siglusLotRepository.findAllByIdIn(newHashSet(lotId))).thenReturn(newArrayList(lot));
    when(orderRepository.findOne(orderId)).thenReturn(order);
    OrderLineItemExtension orderLineItemExtension = OrderLineItemExtension.builder()
        .orderLineItemId(lineItemId).build();
    when(lineItemExtensionRepository.findByOrderLineItemIdIn(newHashSet(lineItemId)))
        .thenReturn(newArrayList(orderLineItemExtension));
    when(siglusProofOfDeliveryRepository.findByShipmentId(any())).thenReturn(buildMockProofOfDelivery());
    when(shipmentController.createShipment(shipmentDto, false, false)).thenReturn(shipmentDto);

    // when
    siglusShipmentService.createOrderAndShipment(false, shipmentExtensionRequest);

    // then
    verify(orderRepository, times(2)).save(orderArgumentCaptor.capture());
    verify(lineItemExtensionRepository).delete(lineItemExtensionArgumentCaptor.capture());
    verify(shipmentController).createShipment(shipmentDtoArgumentCaptor.capture(), anyBoolean(), anyBoolean());
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
    shipmentLineItemDto.setQuantityShipped(5L);
    shipmentLineItemDto.setStockOnHand(5L);
    shipmentLineItemDto.setLot(new OrderObjectReferenceDto(UUID.randomUUID()));
    ShipmentDto shipmentDto = new ShipmentDto();
    shipmentDto.setOrder(orderDto);
    shipmentDto.setLineItems(newArrayList(shipmentLineItemDto));
    ShipmentExtensionRequest shipmentExtensionRequest = new ShipmentExtensionRequest();
    shipmentExtensionRequest.setShipment(shipmentDto);
    shipmentExtensionRequest.setConferredBy(conferredBy);
    shipmentExtensionRequest.setPreparedBy(preparedBy);
    OrderLineItem lineItem = new OrderLineItem();
    lineItem.setOrderable(new VersionEntityReference(orderableId, 1L));
    Order order = new Order();
    lineItem.setId(lineItemId);
    order.setOrderLineItems(newArrayList(lineItem));
    when(orderRepository.findOne(orderId)).thenReturn(order);
    when(lineItemExtensionRepository.findByOrderLineItemIdIn(newHashSet())).thenReturn(newArrayList());
    when(siglusProofOfDeliveryRepository.findByShipmentId(any())).thenReturn(buildMockProofOfDelivery());
    when(shipmentController.createShipment(shipmentDto, false, false)).thenReturn(shipmentDto);
    // when
    siglusShipmentService.createOrderAndShipment(false, shipmentExtensionRequest);

    // then
    verify(orderRepository, times(1)).save(orderArgumentCaptor.capture());
    verify(lineItemExtensionRepository, times(0)).delete(lineItemExtensionArgumentCaptor.capture());
    verify(shipmentController).createShipment(shipmentDtoArgumentCaptor.capture(), anyBoolean(), anyBoolean());
    Order orderToSave = orderArgumentCaptor.getValue();
    ShipmentDto shipmentDtoToSave = shipmentDtoArgumentCaptor.getValue();
    assertTrue(CollectionUtils.isNotEmpty(orderToSave.getOrderLineItems()));
    assertTrue(CollectionUtils.isNotEmpty(shipmentDtoToSave.getLineItems()));
    verify(podExtensionRepository, times(1)).save(buildMockPodExtension());
  }

  @Test
  public void shouldCreateSubOrderWhenLineItemShippedQualityLessOrderQuality() {
    // given
    Lot lot1 = new Lot();
    lot1.setId(lotId);
    lot1.setExpirationDate(LocalDate.now());
    Lot lot2 = new Lot();
    lot2.setId(lotId2);
    lot2.setExpirationDate(LocalDate.now().plusDays(1));
    when(siglusLotRepository.findAllByIdIn(newHashSet(lotId, lotId2))).thenReturn(newArrayList(lot1, lot2));
    ShipmentDto shipmentDto = createShipmentDto();
    ShipmentExtensionRequest shipmentExtensionRequest = new ShipmentExtensionRequest();
    shipmentExtensionRequest.setShipment(shipmentDto);
    shipmentExtensionRequest.setConferredBy(conferredBy);
    shipmentExtensionRequest.setPreparedBy(preparedBy);
    Order order = new Order();
    OrderLineItem lineItem = new OrderLineItem();
    lineItem.setOrderable(new VersionEntityReference(orderableId, 1L));
    lineItem.setId(lineItemId);
    order.setOrderLineItems(newArrayList(lineItem));
    when(orderRepository.findOne(shipmentDto.getOrder().getId())).thenReturn(order);
    when(lineItemExtensionRepository.findByOrderLineItemIdIn(newHashSet()))
        .thenReturn(newArrayList());
    when(siglusProofOfDeliveryRepository.findByShipmentId(any())).thenReturn(buildMockProofOfDelivery());
    when(shipmentController.createShipment(shipmentDto, false, false)).thenReturn(shipmentDto);

    // when
    siglusShipmentService.createOrderAndShipment(true, shipmentExtensionRequest);

    // then
    verify(siglusOrderService).createSubOrder(any(OrderObjectReferenceDto.class),
        orderLineItemDtoArgumentCaptor.capture());
    List<OrderLineItemDto> lineItemDtos = orderLineItemDtoArgumentCaptor.getValue();
    assertEquals(1, lineItemDtos.size());
    assertEquals(Long.valueOf(10), lineItemDtos.get(0).getPartialFulfilledQuantity());
    assertEquals(Long.valueOf(40), lineItemDtos.get(0).getOrderedQuantity());
  }

  @Test(expected = ValidationMessageException.class)
  public void shouldDontCreateSubOrderIfShippedQualityGreaterOrderQualityWhenSubLineItemEmpty() {
    ShipmentDto shipmentDto = createShipmentDto();
    VersionObjectReferenceDto orderReferenceDto = new VersionObjectReferenceDto(orderableId,
        "", "", 1L);
    ShipmentLineItemDto shipmentLineItem1 = new ShipmentLineItemDto();
    shipmentLineItem1.setOrderable(orderReferenceDto);
    shipmentLineItem1.setQuantityShipped(50L);
    shipmentDto.setLineItems(Collections.singletonList(shipmentLineItem1));
    ShipmentExtensionRequest shipmentExtensionRequest = new ShipmentExtensionRequest();
    shipmentExtensionRequest.setShipment(shipmentDto);
    shipmentExtensionRequest.setConferredBy(conferredBy);
    shipmentExtensionRequest.setPreparedBy(preparedBy);
    when(orderRepository.findOne(any())).thenReturn(mockOrder());

    // when
    siglusShipmentService.createOrderAndShipment(true, shipmentExtensionRequest);

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
    order.setOrderLineItems(Collections.singletonList(lineItem));
    shipmentDto.setOrder(order);
    shipmentDto.setLineItems(new ArrayList<>());
    ShipmentExtensionRequest shipmentExtensionRequest = new ShipmentExtensionRequest();
    shipmentExtensionRequest.setShipment(shipmentDto);
    shipmentExtensionRequest.setConferredBy(conferredBy);
    shipmentExtensionRequest.setPreparedBy(preparedBy);
    when(orderRepository.findOne(any())).thenReturn(mockOrder());

    // when
    siglusShipmentService.createOrderAndShipment(true, shipmentExtensionRequest);

    // then
    expectedException.expect(ValidationMessageException.class);
    expectedException.expectMessage(ERROR_SUB_ORDER_LINE_ITEM);
  }


  @Test
  public void shouldCreateSubOrderWhenLineItemOrderQualityGreaterThan0AndShipmentEmpty() {
    Lot lot1 = new Lot();
    lot1.setId(lotId);
    lot1.setExpirationDate(LocalDate.now());
    Lot lot2 = new Lot();
    lot2.setId(lotId2);
    lot2.setExpirationDate(LocalDate.now().plusDays(1));
    when(siglusLotRepository.findAllByIdIn(newHashSet(lotId, lotId2))).thenReturn(newArrayList(lot1, lot2));
    ShipmentDto shipmentDto = createShipmentDto();
    ShipmentExtensionRequest shipmentExtensionRequest = new ShipmentExtensionRequest();
    shipmentExtensionRequest.setShipment(shipmentDto);
    shipmentExtensionRequest.setConferredBy(conferredBy);
    shipmentExtensionRequest.setPreparedBy(preparedBy);
    Order order = new Order();
    OrderLineItem lineItem = new OrderLineItem();
    lineItem.setOrderable(new VersionEntityReference(orderableId, 1L));
    lineItem.setId(lineItemId);
    order.setOrderLineItems(newArrayList(lineItem));
    when(orderRepository.findOne(shipmentDto.getOrder().getId())).thenReturn(order);
    when(lineItemExtensionRepository.findByOrderLineItemIdIn(newHashSet()))
        .thenReturn(newArrayList());
    when(siglusProofOfDeliveryRepository.findByShipmentId(any())).thenReturn(buildMockProofOfDelivery());
    when(shipmentController.createShipment(shipmentDto, false, false)).thenReturn(shipmentDto);

    // when
    siglusShipmentService.createOrderAndShipment(true, shipmentExtensionRequest);

    // then
    verify(siglusOrderService).createSubOrder(any(OrderObjectReferenceDto.class),
        orderLineItemDtoArgumentCaptor.capture());
    List<OrderLineItemDto> lineItemDtos = orderLineItemDtoArgumentCaptor.getValue();
    assertEquals(1, lineItemDtos.size());
    assertEquals(Long.valueOf(10), lineItemDtos.get(0).getPartialFulfilledQuantity());
    assertEquals(Long.valueOf(40), lineItemDtos.get(0).getOrderedQuantity());
  }

  @Test
  public void shouldConfirmShipmentWithLocation() {
    // given
    ShipmentLineItemDto shipmentLineItem = new ShipmentLineItemDto();
    OrderableDto orderableDto = new OrderableDto();
    orderableDto.setId(orderableId);
    shipmentLineItem.setOrderable(orderableDto);
    shipmentLineItem.setQuantityShipped(5L);
    shipmentLineItem.setLotId(lotId);
    LocationDto locationDto = new LocationDto();
    locationDto.setLocationCode("ABC");
    locationDto.setArea("QAZ");
    shipmentLineItem.setLocation(locationDto);
    shipmentLineItem.setQuantityShipped(5L);
    shipmentLineItem.setStockOnHand(5L);
    shipmentLineItem.setLot(new OrderObjectReferenceDto(UUID.randomUUID()));
    ShipmentDto shipmentDto = createShipmentDto();
    shipmentDto.setLineItems(Lists.newArrayList(shipmentLineItem));
    Order order = new Order();
    OrderLineItem lineItem = new OrderLineItem();
    lineItem.setOrderable(new VersionEntityReference(orderableId, 1L));
    lineItem.setId(lineItemId);
    order.setOrderLineItems(newArrayList(lineItem));
    ShipmentExtensionRequest shipmentExtensionRequest = new ShipmentExtensionRequest();
    shipmentExtensionRequest.setShipment(shipmentDto);
    shipmentExtensionRequest.setConferredBy(conferredBy);
    shipmentExtensionRequest.setPreparedBy(preparedBy);
    when(orderRepository.findOne(shipmentDto.getOrder().getId())).thenReturn(order);
    when(shipmentController.createShipment(shipmentDto, true, false)).thenReturn(createShipmentDtoWithLocation());
    when(authenticationHelper.getCurrentUser()).thenReturn(buildUserDto());
    when(siglusProofOfDeliveryRepository.findByShipmentId(any())).thenReturn(buildMockProofOfDelivery());
    when(stockCardRepository.findByFacilityIdAndOrderableIdAndLotId(any(), any(), any()))
        .thenReturn(buildStockCard());
    when(stockCardLineItemRepository.findLatestByStockCardIds(any()))
        .thenReturn(Lists.newArrayList(buildStockCardLineItem()));
    when(siglusStockCardRepository.findByFacilityIdAndOrderableLotIdPairs(any(), any()))
        .thenReturn(Lists.newArrayList(buildStockCard()));

    // when
    siglusShipmentService.createOrderAndShipmentByLocation(false, shipmentExtensionRequest);

    // then
    verify(shipmentLineItemsExtensionRepository, times(0)).save(Lists.newArrayList());
    verify(stockCardLineItemExtensionRepository, times(0))
        .save(Lists.newArrayList(buildStockCardLineItemExtension()));
  }

  @Test(expected = NotFoundException.class)
  public void shouldThrowNotFoundExceptionWhenProcessingPeriodExtensionIsNull() {
    //given
    ShipmentExtensionRequest shipmentExtensionRequest = createShipmentExtensionRequest();
    when(processingPeriodExtensionRepository.findByProcessingPeriodId(processingPeriodId)).thenReturn(null);

    //when
    siglusShipmentService.checkFulfillOrderExpired(shipmentExtensionRequest);
  }

  @Test(expected = BusinessDataException.class)
  public void shouldThrowBusinessExceptionWhenFulfillOrderExpired() {
    //given
    ProcessingPeriodExtension processingPeriodExtension = new ProcessingPeriodExtension();
    processingPeriodExtension.setSubmitStartDate(LocalDate.now().plusMonths(1).plusDays(1));
    processingPeriodExtension.setSubmitEndDate(LocalDate.now().plusMonths(1).plusDays(2));
    ShipmentExtensionRequest shipmentExtensionRequest = createShipmentExtensionRequest();
    when(processingPeriodExtensionRepository.findByProcessingPeriodId(processingPeriodId))
        .thenReturn(processingPeriodExtension);
    YearMonth yearMonth = YearMonth.of(LocalDate.now().getYear(), LocalDate.now().getMonth().plus(1));
    when(requisitionService.calculateFulfillOrderYearMonth(processingPeriodExtension))
        .thenReturn(Arrays.asList(yearMonth));

    //when
    siglusShipmentService.checkFulfillOrderExpired(shipmentExtensionRequest);
  }

  @Test(expected = ValidationMessageException.class)
  public void shouldThrowValidationMessageExceptionWhenLineItemDuplicated() {
    // given
    UUID orderableId = UUID.randomUUID();
    OrderableDto orderableDto = new OrderableDto();
    orderableDto.setId(orderableId);
    ShipmentLineItemDto shipmentLineItemDto1 = new ShipmentLineItemDto();
    shipmentLineItemDto1.setOrderable(orderableDto);
    ShipmentLineItemDto shipmentLineItemDto2 = new ShipmentLineItemDto();
    shipmentLineItemDto2.setOrderable(orderableDto);
    UUID lotId1 = UUID.randomUUID();
    shipmentLineItemDto1.setLotId(lotId1);
    shipmentLineItemDto2.setLotId(lotId1);
    ShipmentExtensionRequest shipmentExtensionRequest = createShipmentExtensionRequest();
    shipmentExtensionRequest.getShipment().setLineItems(new ArrayList<>());
    shipmentExtensionRequest.getShipment().lineItems().add(shipmentLineItemDto1);
    shipmentExtensionRequest.getShipment().lineItems().add(shipmentLineItemDto2);

    // when
    siglusShipmentService.validShipmentLineItemsDuplicated(shipmentExtensionRequest);
  }

  @Test(expected = ValidationMessageException.class)
  public void shouldThrowValidationMessageExceptionWhenLineItemDuplicatedWithoutLot() {
    // given
    UUID orderableId = UUID.randomUUID();
    OrderableDto orderableDto = new OrderableDto();
    orderableDto.setId(orderableId);
    ShipmentLineItemDto shipmentLineItemDto1 = new ShipmentLineItemDto();
    shipmentLineItemDto1.setOrderable(orderableDto);
    ShipmentLineItemDto shipmentLineItemDto2 = new ShipmentLineItemDto();
    shipmentLineItemDto2.setOrderable(orderableDto);
    ShipmentExtensionRequest shipmentExtensionRequest = createShipmentExtensionRequest();
    shipmentExtensionRequest.getShipment().setLineItems(new ArrayList<>());
    shipmentExtensionRequest.getShipment().lineItems().add(shipmentLineItemDto1);
    shipmentExtensionRequest.getShipment().lineItems().add(shipmentLineItemDto2);

    // when
    siglusShipmentService.validShipmentLineItemsDuplicated(shipmentExtensionRequest);
  }

  @Test
  public void shouldCalcFefoIsTrue() {
    // given
    VersionObjectReferenceDto orderReferenceDto = new VersionObjectReferenceDto(orderableId,
        "", "", 1L);
    ShipmentLineItemDto shipmentLineItem1 = new ShipmentLineItemDto();
    shipmentLineItem1.setOrderable(orderReferenceDto);
    shipmentLineItem1.setQuantityShipped(50L);
    shipmentLineItem1.setStockOnHand(100L);
    shipmentLineItem1.setLot(new ObjectReferenceDto(lotId));
    ShipmentLineItemDto shipmentLineItem2 = new ShipmentLineItemDto();
    shipmentLineItem2.setOrderable(orderReferenceDto);
    shipmentLineItem2.setQuantityShipped(5L);
    shipmentLineItem2.setStockOnHand(100L);
    shipmentLineItem2.setLot(new ObjectReferenceDto(lotId2));
    ShipmentDto shipmentDto = new ShipmentDto();
    shipmentDto.setLineItems(Arrays.asList(shipmentLineItem1, shipmentLineItem2));
    Lot lot1 = new Lot();
    lot1.setId(lotId);
    lot1.setExpirationDate(LocalDate.now());
    Lot lot2 = new Lot();
    lot2.setId(lotId2);
    lot2.setExpirationDate(LocalDate.now().plusDays(1));
    when(siglusLotRepository.findAllByIdIn(newHashSet(lotId, lotId2))).thenReturn(newArrayList(lot1, lot2));
    // when

    // then
    assertTrue(siglusShipmentService.calcIsFefo(shipmentDto));
  }

  @Test
  public void shouldCalcFefoIsFalse() {
    // given
    VersionObjectReferenceDto orderReferenceDto = new VersionObjectReferenceDto(orderableId,
        "", "", 1L);
    ShipmentLineItemDto shipmentLineItem1 = new ShipmentLineItemDto();
    shipmentLineItem1.setOrderable(orderReferenceDto);
    shipmentLineItem1.setQuantityShipped(5L);
    shipmentLineItem1.setStockOnHand(100L);
    shipmentLineItem1.setLot(new ObjectReferenceDto(lotId));
    ShipmentLineItemDto shipmentLineItem2 = new ShipmentLineItemDto();
    shipmentLineItem2.setOrderable(orderReferenceDto);
    shipmentLineItem2.setQuantityShipped(50L);
    shipmentLineItem2.setStockOnHand(100L);
    shipmentLineItem2.setLot(new ObjectReferenceDto(lotId2));
    ShipmentDto shipmentDto = new ShipmentDto();
    shipmentDto.setLineItems(Arrays.asList(shipmentLineItem1, shipmentLineItem2));
    Lot lot1 = new Lot();
    lot1.setId(lotId);
    lot1.setExpirationDate(LocalDate.now());
    Lot lot2 = new Lot();
    lot2.setId(lotId2);
    lot2.setExpirationDate(LocalDate.now().plusDays(1));
    when(siglusLotRepository.findAllByIdIn(newHashSet(lotId, lotId2))).thenReturn(newArrayList(lot1, lot2));
    // when

    // then
    assertFalse(siglusShipmentService.calcIsFefo(shipmentDto));
  }

  @Test
  public void shouldThrowExceptionWhenShippedQuantityIsZero() {
    // given
    VersionObjectReferenceDto orderReferenceDto = new VersionObjectReferenceDto(orderableId,
        "", "", 1L);
    ShipmentLineItemDto shipmentLineItem1 = new ShipmentLineItemDto();
    shipmentLineItem1.setOrderable(orderReferenceDto);
    shipmentLineItem1.setQuantityShipped(0L);
    shipmentLineItem1.setStockOnHand(100L);
    shipmentLineItem1.setLot(new ObjectReferenceDto(lotId));
    ShipmentLineItemDto shipmentLineItem2 = new ShipmentLineItemDto();
    shipmentLineItem2.setOrderable(orderReferenceDto);
    shipmentLineItem2.setQuantityShipped(0L);
    shipmentLineItem2.setStockOnHand(100L);
    shipmentLineItem2.setLot(new ObjectReferenceDto(lotId2));
    ShipmentDto shipmentDto = new ShipmentDto();
    shipmentDto.setLineItems(Arrays.asList(shipmentLineItem1, shipmentLineItem2));
    Lot lot1 = new Lot();
    lot1.setId(lotId);
    lot1.setExpirationDate(LocalDate.now());
    Lot lot2 = new Lot();
    lot2.setId(lotId2);
    lot2.setExpirationDate(LocalDate.now().plusDays(1));
    when(siglusLotRepository.findAllByIdIn(newHashSet(lotId, lotId2))).thenReturn(newArrayList(lot1, lot2));
    // when
    boolean result = siglusShipmentService.calcIsFefo(shipmentDto);
    // then
    assertTrue(result);
  }

  private ShipmentExtensionRequest createShipmentExtensionRequest() {
    OrderObjectReferenceDto order = new OrderObjectReferenceDto();
    ProcessingPeriodDto processingPeriodDto = new ProcessingPeriodDto();
    processingPeriodDto.setId(processingPeriodId);
    processingPeriodDto.setEndDate(LocalDate.now());
    order.setProcessingPeriod(processingPeriodDto);
    ShipmentDto shipmentDto = new ShipmentDto();
    shipmentDto.setOrder(order);
    ShipmentExtensionRequest shipmentExtensionRequest = new ShipmentExtensionRequest();
    shipmentExtensionRequest.setShipment(shipmentDto);
    return shipmentExtensionRequest;
  }

  private ShipmentDto createShipmentDto() {
    OrderLineItemDto lineItem = new OrderLineItemDto();
    lineItem.setOrderedQuantity(40L);
    OrderableDto orderableDto = new OrderableDto();
    orderableDto.setId(orderableId);
    lineItem.setOrderable(orderableDto);
    OrderObjectReferenceDto order = new OrderObjectReferenceDto(UUID.randomUUID());
    order.setOrderLineItems(Collections.singletonList(lineItem));
    ShipmentDto shipmentDto = new ShipmentDto();
    shipmentDto.setOrder(order);
    VersionObjectReferenceDto orderReferenceDto = new VersionObjectReferenceDto(orderableId,
        "", "", 1L);
    ShipmentLineItemDto shipmentLineItem1 = new ShipmentLineItemDto();
    shipmentLineItem1.setOrderable(orderReferenceDto);
    shipmentLineItem1.setQuantityShipped(5L);
    shipmentLineItem1.setStockOnHand(5L);
    shipmentLineItem1.setLot(new ObjectReferenceDto(lotId));
    ShipmentLineItemDto shipmentLineItem2 = new ShipmentLineItemDto();
    shipmentLineItem2.setOrderable(orderReferenceDto);
    shipmentLineItem2.setQuantityShipped(5L);
    shipmentLineItem2.setStockOnHand(5L);
    shipmentLineItem2.setLot(new ObjectReferenceDto(lotId2));
    shipmentDto.setLineItems(Arrays.asList(shipmentLineItem1, shipmentLineItem2));
    return shipmentDto;
  }

  private ShipmentDto createShipmentDtoWithLocation() {
    OrderLineItemDto lineItem = new OrderLineItemDto();
    lineItem.setOrderedQuantity(40L);
    OrderableDto orderableDto = new OrderableDto();
    orderableDto.setId(orderableId);
    lineItem.setOrderable(orderableDto);
    lineItem.setId(lineItemId);
    lineItem.setSkipped(true);
    OrderObjectReferenceDto order = new OrderObjectReferenceDto(UUID.randomUUID());
    order.setOrderLineItems(Collections.singletonList(lineItem));
    FacilityDto facilityDto = new FacilityDto();
    facilityDto.setId(facilityId);
    order.setFacility(facilityDto);
    ShipmentDto shipmentDto = new ShipmentDto();
    shipmentDto.setOrder(order);
    VersionObjectReferenceDto orderReferenceDto = new VersionObjectReferenceDto(orderableId,
        "", "", 1L);
    ShipmentLineItemDto shipmentLineItem = new ShipmentLineItemDto();
    shipmentLineItem.setOrderable(orderReferenceDto);
    shipmentLineItem.setQuantityShipped(5L);
    shipmentLineItem.setStockOnHand(5L);
    shipmentLineItem.setLotId(lotId);
    LocationDto locationDto = new LocationDto();
    locationDto.setLocationCode("AA25A");
    locationDto.setArea("balabala");
    shipmentLineItem.setLocation(locationDto);
    shipmentDto.setLineItems(Collections.singletonList(shipmentLineItem));
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

  private Order mockOrder() {
    Order order = new Order();
    order.setId(orderId);
    order.setOrderLineItems(newArrayList());
    return order;
  }

  private UserDto buildUserDto() {
    UserDto userDto = new UserDto();
    userDto.setHomeFacilityId(facilityId);
    return userDto;
  }

  private ProofOfDelivery buildMockProofOfDelivery() {
    VersionEntityReference orderable = new VersionEntityReference();
    orderable.setId(orderableId);
    orderable.setVersionNumber(1L);
    ShipmentLineItem shipmentLineItem = new ShipmentLineItem(orderable, 10L, null);
    Shipment shipment = new Shipment(new Order(), null, "notes",
        org.assertj.core.util.Lists.newArrayList(shipmentLineItem), null);
    ProofOfDeliveryLineItem proofOfDeliveryLineItem = new ProofOfDeliveryLineItem(orderable, lotId, 10,
        null, 0, null, "notes");
    ProofOfDelivery proofOfDelivery = new ProofOfDelivery(shipment, ProofOfDeliveryStatus.CONFIRMED,
        org.assertj.core.util.Lists.newArrayList(proofOfDeliveryLineItem),
        "test", "test", LocalDate.now());
    proofOfDelivery.setId(podId);
    return proofOfDelivery;
  }

  private PodExtension buildMockPodExtension() {
    return PodExtension
        .builder()
        .podId(podId)
        .preparedBy(preparedBy)
        .conferredBy(conferredBy)
        .build();
  }

  private StockCardLineItemExtension buildStockCardLineItemExtension() {
    return StockCardLineItemExtension
        .builder()
        .stockCardLineItemId(lineItemId)
        .locationCode("ABC")
        .area("DEF")
        .build();
  }

  private StockCard buildStockCard() {
    StockCard stockCard = new StockCard();
    stockCard.setId(stockCardId);
    stockCard.setOrderableId(orderableId);
    stockCard.setLotId(lotId);
    return stockCard;
  }

  private StockCardLineItem buildStockCardLineItem() {
    StockCardLineItem stockCardLineItem = new StockCardLineItem();
    stockCardLineItem.setStockCard(buildStockCard());
    stockCardLineItem.setId(stockCardLineItemId);
    return stockCardLineItem;
  }
}
