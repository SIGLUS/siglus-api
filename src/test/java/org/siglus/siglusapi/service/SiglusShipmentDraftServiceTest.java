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
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.fulfillment.domain.Order;
import org.openlmis.fulfillment.domain.OrderLineItem;
import org.openlmis.fulfillment.repository.OrderRepository;
import org.openlmis.fulfillment.web.shipment.LocationDto;
import org.openlmis.fulfillment.web.shipment.ShipmentLineItemDto;
import org.openlmis.fulfillment.web.shipmentdraft.ShipmentDraftController;
import org.openlmis.fulfillment.web.shipmentdraft.ShipmentDraftDto;
import org.openlmis.fulfillment.web.util.OrderLineItemDto;
import org.openlmis.fulfillment.web.util.OrderObjectReferenceDto;
import org.openlmis.fulfillment.web.util.VersionObjectReferenceDto;
import org.siglus.siglusapi.domain.FacilityLocations;
import org.siglus.siglusapi.domain.OrderLineItemExtension;
import org.siglus.siglusapi.domain.ShipmentDraftLineItemsExtension;
import org.siglus.siglusapi.repository.FacilityLocationsRepository;
import org.siglus.siglusapi.repository.OrderLineItemExtensionRepository;
import org.siglus.siglusapi.repository.OrderLineItemRepository;
import org.siglus.siglusapi.repository.ShipmentDraftLineItemsExtensionRepository;
import org.siglus.siglusapi.service.client.SiglusShipmentDraftFulfillmentService;
import org.springframework.data.domain.PageImpl;

@RunWith(MockitoJUnitRunner.class)
public class SiglusShipmentDraftServiceTest {

  @Captor
  private ArgumentCaptor<List<OrderLineItemExtension>> lineItemExtensionsArgumentCaptor;

  @Spy
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

  @Mock
  private ShipmentDraftLineItemsExtensionRepository shipmentDraftLineItemsByLocationRepository;

  @Mock
  private FacilityLocationsRepository locationManagementRepository;

  private final UUID draftId = UUID.randomUUID();

  private final UUID orderId = UUID.randomUUID();

  private final UUID lineItemId = UUID.randomUUID();

  private final UUID locationId = UUID.randomUUID();

  private final UUID orderableId = UUID.randomUUID();

  private final String locationCode = "AA25A";
  private final String area = "Armazem Principal";

  @Before
  public void prepare() {
    ShipmentDraftDto shipmentDraft = new ShipmentDraftDto();
    ShipmentLineItemDto lineItemDto = new ShipmentLineItemDto();
    lineItemDto.setId(lineItemId);
    List<ShipmentLineItemDto> shipmentLineItemDtos = Collections.singletonList(lineItemDto);
    shipmentDraft.setLineItems(shipmentLineItemDtos);
    when(draftController.getShipmentDraft(draftId, Collections.emptySet())).thenReturn(shipmentDraft);
  }

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
    doNothing().when(siglusShipmentDraftService).checkStockOnHandQuantity(any(), any());

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
    doNothing().when(siglusShipmentDraftService).checkStockOnHandQuantity(any(), any());

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

  @Test
  public void shouldGetShipmentDraftByLocation() {
    // given
    ShipmentLineItemDto shipmentLineItemDto = new ShipmentLineItemDto();
    LocationDto locationDto = LocationDto.builder().area(area).locationCode(locationCode).build();
    shipmentLineItemDto.setLocation(locationDto);
    shipmentLineItemDto.setId(lineItemId);
    shipmentLineItemDto.setQuantityShipped(10L);
    ShipmentDraftDto draftDto = new ShipmentDraftDto();
    draftDto.setLineItems(newArrayList(shipmentLineItemDto));
    OrderObjectReferenceDto orderDto = new OrderObjectReferenceDto(orderId);
    draftDto.setOrder(orderDto);
    when(siglusShipmentDraftFulfillmentService.searchShipmentDraft(draftId))
        .thenReturn(draftDto);
    List<ShipmentLineItemDto> shipmentLineItemDtos = draftDto.lineItems();
    ShipmentDraftLineItemsExtension shipmentDraftLineItemsByLocation = new ShipmentDraftLineItemsExtension();
    shipmentDraftLineItemsByLocation.setLocationCode(locationCode);
    shipmentDraftLineItemsByLocation.setArea(area);
    shipmentDraftLineItemsByLocation.setShipmentDraftLineItemId(lineItemId);
    shipmentDraftLineItemsByLocation.setId(lineItemId);
    shipmentDraftLineItemsByLocation.setQuantityShipped(10);
    List<UUID> lineItemIds = shipmentLineItemDtos.stream().map(ShipmentLineItemDto::getId).collect(Collectors.toList());
    when(shipmentDraftLineItemsByLocationRepository.findByShipmentDraftLineItemIdIn(lineItemIds))
        .thenReturn(newArrayList(shipmentDraftLineItemsByLocation));
    FacilityLocations locationManagement = new FacilityLocations();
    locationManagement.setLocationCode(locationCode);
    locationManagement.setId(locationId);
    when(locationManagementRepository.findByIdIn(newArrayList(locationId)))
        .thenReturn(newArrayList(locationManagement));
    when(siglusShipmentDraftFulfillmentService.getShipmentDraftByOrderId(orderId))
        .thenReturn(new PageImpl<>(newArrayList(draftDto)));

    // when
    ShipmentDraftDto shipmentDraftByLocation = siglusShipmentDraftService.getShipmentDraftByLocation(orderId);

    // then
    assertEquals(shipmentDraftByLocation.lineItems().get(0).getLocation().getLocationCode(), locationCode);
  }

  @Test
  public void shouldUpdateShipmentDraftByLocation() {
    // given
    OrderLineItemDto lineItemDto = new OrderLineItemDto();
    lineItemDto.setId(lineItemId);
    lineItemDto.setSkipped(true);
    OrderObjectReferenceDto order = new OrderObjectReferenceDto(orderId);
    order.setOrderLineItems(newArrayList(lineItemDto));
    ShipmentDraftDto draftDto = new ShipmentDraftDto();
    draftDto.setId(draftId);
    draftDto.setOrder(order);
    ShipmentLineItemDto shipmentLineItemDto = new ShipmentLineItemDto();
    LocationDto locationDto = LocationDto.builder().area(area).locationCode(locationCode).build();
    shipmentLineItemDto.setLocation(locationDto);
    shipmentLineItemDto.setId(lineItemId);
    shipmentLineItemDto.setQuantityShipped(10L);
    VersionObjectReferenceDto orderable = new VersionObjectReferenceDto(orderableId, null, null,
        1L);
    shipmentLineItemDto.setOrderable(orderable);
    draftDto.setLineItems(newArrayList(shipmentLineItemDto));
    doNothing().when(siglusShipmentDraftService).checkStockOnHandQuantity(any(), any());
    when(draftController.updateShipmentDraft(draftId, draftDto))
        .thenReturn(draftDto);
    OrderLineItemExtension extension = OrderLineItemExtension.builder()
        .orderLineItemId(lineItemId)
        .skipped(false)
        .build();
    when(lineItemExtensionRepository.findByOrderLineItemIdIn(newHashSet(lineItemId)))
        .thenReturn(newArrayList(extension));
    when(siglusOrderService.updateOrderLineItems(draftDto)).thenReturn(newHashSet(lineItemId));
    List<ShipmentLineItemDto> shipmentLineItemDtos = draftDto.lineItems();
    ShipmentDraftLineItemsExtension shipmentDraftLineItemsByLocation = new ShipmentDraftLineItemsExtension();
    shipmentDraftLineItemsByLocation.setLocationCode(locationCode);
    shipmentDraftLineItemsByLocation.setArea(area);
    shipmentDraftLineItemsByLocation.setShipmentDraftLineItemId(lineItemId);
    List<UUID> lineItemIds = shipmentLineItemDtos.stream().map(ShipmentLineItemDto::getId).collect(Collectors.toList());
    when(shipmentDraftLineItemsByLocationRepository.findByShipmentDraftLineItemIdIn(lineItemIds))
        .thenReturn(newArrayList(shipmentDraftLineItemsByLocation));
    FacilityLocations locationManagement = new FacilityLocations();
    locationManagement.setLocationCode(locationCode);
    locationManagement.setId(locationId);
    when(locationManagementRepository.findByIdIn(newArrayList(locationId)))
        .thenReturn(newArrayList(locationManagement));

    // when
    ShipmentDraftDto shipmentDraftDto = siglusShipmentDraftService.updateShipmentDraftByLocation(draftId, draftDto);

    // then
    assertEquals(shipmentDraftDto.getId(), draftId);
  }

  @Test
  public void shouldDeleteLocationWhenDeleteShipmentDraft() {
    // given
    ShipmentDraftDto draftDto = new ShipmentDraftDto();
    ShipmentLineItemDto shipmentLineItemDto = new ShipmentLineItemDto();
    shipmentLineItemDto.setId(lineItemId);
    shipmentLineItemDto.setLocation(new LocationDto("das", "dasf"));
    draftDto.setLineItems(newArrayList(shipmentLineItemDto));
    OrderObjectReferenceDto orderDto = new OrderObjectReferenceDto(orderId);
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
    siglusShipmentDraftService.deleteShipmentDraftByLocation(draftId);

    // then
    verify(shipmentDraftLineItemsByLocationRepository, times(1))
        .deleteByShipmentDraftLineItemIdIn(Lists.newArrayList(shipmentLineItemDto.getId()));
  }
}
