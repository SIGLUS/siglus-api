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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anySet;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;

import java.util.ArrayList;
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
import org.openlmis.fulfillment.service.referencedata.FacilityDto;
import org.openlmis.fulfillment.service.referencedata.ProgramDto;
import org.openlmis.fulfillment.web.shipment.LocationDto;
import org.openlmis.fulfillment.web.shipment.ShipmentLineItemDto;
import org.openlmis.fulfillment.web.shipmentdraft.ShipmentDraftController;
import org.openlmis.fulfillment.web.shipmentdraft.ShipmentDraftDto;
import org.openlmis.fulfillment.web.util.ObjectReferenceDto;
import org.openlmis.fulfillment.web.util.OrderLineItemDto;
import org.openlmis.fulfillment.web.util.OrderObjectReferenceDto;
import org.openlmis.fulfillment.web.util.VersionObjectReferenceDto;
import org.openlmis.stockmanagement.domain.card.StockCard;
import org.siglus.siglusapi.domain.FacilityLocations;
import org.siglus.siglusapi.domain.OrderLineItemExtension;
import org.siglus.siglusapi.domain.ShipmentDraftLineItemsExtension;
import org.siglus.siglusapi.dto.LotDto;
import org.siglus.siglusapi.dto.SiglusShipmentDraftDto;
import org.siglus.siglusapi.dto.SiglusShipmentDraftLineItemDto;
import org.siglus.siglusapi.exception.ValidationMessageException;
import org.siglus.siglusapi.repository.FacilityLocationsRepository;
import org.siglus.siglusapi.repository.OrderLineItemExtensionRepository;
import org.siglus.siglusapi.repository.OrderLineItemRepository;
import org.siglus.siglusapi.repository.ShipmentDraftLineItemsExtensionRepository;
import org.siglus.siglusapi.repository.ShipmentDraftLineItemsRepository;
import org.siglus.siglusapi.repository.SiglusStockCardRepository;
import org.siglus.siglusapi.repository.dto.OrderableVersionDto;
import org.siglus.siglusapi.repository.dto.StockCardReservedDto;
import org.siglus.siglusapi.repository.dto.StockCardStockDto;
import org.siglus.siglusapi.service.client.SiglusShipmentDraftFulfillmentService;
import org.siglus.siglusapi.util.FacilityConfigHelper;
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

  @Mock
  private SiglusStockCardRepository siglusStockCardRepository;

  @Mock
  private FacilityConfigHelper facilityConfigHelper;

  @Mock
  private SiglusStockCardSummariesService siglusStockCardSummariesService;

  @Mock
  private ShipmentDraftLineItemsRepository shipmentDraftLineItemsRepository;

  @Mock
  private SiglusOrderableService siglusOrderableService;

  @Mock
  private SiglusLotService siglusLotService;

  @Mock
  private SiglusStockCardService siglusStockCardService;

  private final UUID draftId = UUID.randomUUID();

  private final UUID orderId = UUID.randomUUID();

  private final UUID lineItemId = UUID.randomUUID();

  private final UUID locationId = UUID.randomUUID();

  private final UUID orderableId = UUID.randomUUID();

  private final UUID lotId = UUID.randomUUID();

  private final UUID stockCardId = UUID.randomUUID();

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

  @Test
  public void shouldSuccessWhenCheckStockOnHandQuantityGivenShipmentDraftHasNotItems() {
    ShipmentDraftDto draftDto = buildShipmentDraftDto();
    try {
      siglusShipmentDraftService.checkStockOnHandQuantity(UUID.randomUUID(), draftDto);
    } catch (Exception e) {
      fail("Unexpected exception: " + e.getMessage());
    }
  }

  @Test(expected = ValidationMessageException.class)
  public void shouldThrowExceptionWhenCheckStockOnHandQuantityGivenHaveNotEnoughSoh() {
    ShipmentLineItemDto lineItemDto = new ShipmentLineItemDto();
    VersionObjectReferenceDto orderable = new VersionObjectReferenceDto(orderableId, null, null, 1L);
    lineItemDto.setOrderable(orderable);
    ObjectReferenceDto lot = new ObjectReferenceDto(UUID.randomUUID());
    lineItemDto.setLot(lot);
    lineItemDto.setQuantityShipped(10L);
    ShipmentDraftDto draftDto = buildShipmentDraftDto();
    draftDto.setLineItems(newArrayList(lineItemDto));
    StockCard stockCard = StockCard.builder()
        .facilityId(draftDto.getOrder().getSupplyingFacility().getId())
        .programId(draftDto.getOrder().getProgram().getId())
        .orderableId(orderableId)
        .lotId(lot.getId())
        .build();
    when(siglusStockCardRepository.findByFacilityIdAndOrderableLotIdPairs(any(), any()))
        .thenReturn(newArrayList(stockCard));
    when(facilityConfigHelper.isLocationManagementEnabled(draftDto.getOrder().getSupplyingFacility().getId()))
        .thenReturn(false);
    StockCardStockDto stockDto = StockCardStockDto.builder()
        .orderableId(orderableId)
        .lotId(lot.getId())
        .stockOnHand(10)
        .build();
    when(siglusStockCardSummariesService.getLatestStockOnHand(any(), anyBoolean()))
        .thenReturn(newArrayList(stockDto));
    StockCardReservedDto reservedDto = StockCardReservedDto.builder()
        .orderableId(orderableId)
        .lotId(lot.getId())
        .reserved(5)
        .build();
    when(shipmentDraftLineItemsRepository.reservedCount(any(), any()))
        .thenReturn(newArrayList(reservedDto));

    siglusShipmentDraftService.checkStockOnHandQuantity(UUID.randomUUID(), draftDto);
  }

  @Test
  public void shouldNotThrowExceptionWhenCheckStockOnHandQuantityGivenEnoughSoh() {
    ShipmentLineItemDto lineItemDto = new ShipmentLineItemDto();
    VersionObjectReferenceDto orderable = new VersionObjectReferenceDto(orderableId, null, null, 1L);
    lineItemDto.setOrderable(orderable);
    ObjectReferenceDto lot = new ObjectReferenceDto(UUID.randomUUID());
    lineItemDto.setLot(lot);
    lineItemDto.setQuantityShipped(5L);
    ShipmentDraftDto draftDto = buildShipmentDraftDto();
    draftDto.setLineItems(newArrayList(lineItemDto));
    StockCard stockCard = StockCard.builder()
        .facilityId(draftDto.getOrder().getSupplyingFacility().getId())
        .programId(draftDto.getOrder().getProgram().getId())
        .orderableId(orderableId)
        .lotId(lot.getId())
        .build();
    when(siglusStockCardRepository.findByFacilityIdAndOrderableLotIdPairs(any(), any()))
        .thenReturn(newArrayList(stockCard));
    when(facilityConfigHelper.isLocationManagementEnabled(draftDto.getOrder().getSupplyingFacility().getId()))
        .thenReturn(false);
    StockCardStockDto stockDto = StockCardStockDto.builder()
        .orderableId(orderableId)
        .lotId(lot.getId())
        .stockOnHand(10)
        .build();
    when(siglusStockCardSummariesService.getLatestStockOnHand(any(), anyBoolean()))
        .thenReturn(newArrayList(stockDto));
    StockCardReservedDto reservedDto = StockCardReservedDto.builder()
        .orderableId(orderableId)
        .lotId(lot.getId())
        .reserved(5)
        .build();
    when(shipmentDraftLineItemsRepository.reservedCount(any(), any()))
        .thenReturn(newArrayList(reservedDto));

    siglusShipmentDraftService.checkStockOnHandQuantity(UUID.randomUUID(), draftDto);
  }

  @Test
  public void shouldNotThrowExceptionWhenCheckStockOnHandQuantityGivenEnoughSohWithLocation() {
    ShipmentLineItemDto lineItemDto = new ShipmentLineItemDto();
    VersionObjectReferenceDto orderable = new VersionObjectReferenceDto(orderableId, null, null, 1L);
    lineItemDto.setOrderable(orderable);
    ObjectReferenceDto lot = new ObjectReferenceDto(UUID.randomUUID());
    lineItemDto.setLot(lot);
    lineItemDto.setQuantityShipped(5L);
    lineItemDto.setLocation(LocationDto.builder().area(area).locationCode(locationCode).build());
    ShipmentDraftDto draftDto = buildShipmentDraftDto();
    draftDto.setLineItems(newArrayList(lineItemDto));
    StockCard stockCard = StockCard.builder()
        .facilityId(draftDto.getOrder().getSupplyingFacility().getId())
        .programId(draftDto.getOrder().getProgram().getId())
        .orderableId(orderableId)
        .lotId(lot.getId())
        .build();
    when(siglusStockCardRepository.findByFacilityIdAndOrderableLotIdPairs(any(), any()))
        .thenReturn(newArrayList(stockCard));
    when(facilityConfigHelper.isLocationManagementEnabled(draftDto.getOrder().getSupplyingFacility().getId()))
        .thenReturn(false);
    StockCardStockDto stockDto = StockCardStockDto.builder()
        .orderableId(orderableId)
        .lotId(lot.getId())
        .stockOnHand(10)
        .area(area)
        .locationCode(locationCode)
        .build();
    when(siglusStockCardSummariesService.getLatestStockOnHand(any(), anyBoolean()))
        .thenReturn(newArrayList(stockDto));
    StockCardReservedDto reservedDto = StockCardReservedDto.builder()
        .orderableId(orderableId)
        .lotId(lot.getId())
        .reserved(5)
        .locationCode(locationCode)
        .build();
    when(shipmentDraftLineItemsRepository.reservedCount(any(), any()))
        .thenReturn(newArrayList(reservedDto));

    siglusShipmentDraftService.checkStockOnHandQuantity(UUID.randomUUID(), draftDto);
  }

  @Test
  public void shouldGetReservedCountSuccess() {
    UUID facilityId = UUID.randomUUID();
    UUID shipmentDraftId = UUID.randomUUID();
    UUID lotId = UUID.randomUUID();
    StockCardReservedDto reservedDto = StockCardReservedDto.builder()
        .orderableId(orderableId)
        .orderableVersionNumber(1)
        .lotId(lotId)
        .reserved(3)
        .locationCode(locationCode)
        .build();
    when(shipmentDraftLineItemsRepository.reservedCount(facilityId, shipmentDraftId))
        .thenReturn(newArrayList(reservedDto));
    ShipmentLineItemDto lineItemDto = new ShipmentLineItemDto();
    VersionObjectReferenceDto product = new VersionObjectReferenceDto(orderableId, null, null, 1L);
    lineItemDto.setOrderable(product);
    ObjectReferenceDto lot = new ObjectReferenceDto(lotId);
    lineItemDto.setLot(lot);
    lineItemDto.setQuantityShipped(5L);
    lineItemDto.setLocation(LocationDto.builder().area(area).locationCode(locationCode).build());
    List<ShipmentLineItemDto> lineItems = newArrayList(lineItemDto);

    List<StockCardReservedDto> reservedDtos = siglusShipmentDraftService.reservedCount(facilityId,
        shipmentDraftId, lineItems);

    assertEquals(reservedDtos.size(), 1);
    StockCardReservedDto dto = reservedDtos.get(0);
    assertEquals(dto.getReserved().intValue(), 3);
    assertEquals(dto.getOrderableId(), orderableId);
    assertEquals(dto.getLotId(), lotId);
    assertEquals(dto.getLocationCode(), locationCode);
  }

  @Test
  public void shouldGetShipmentDraftWithoutLocationSuccess() {
    ShipmentDraftDto draftDto = buildShipmentDraftDto();
    UUID lotId1 = UUID.randomUUID();
    UUID orderableId1 = UUID.randomUUID();
    ShipmentLineItemDto lineItem1 = buildShipmentLineItemDto(lotId1, orderableId1);
    UUID lotId2 = UUID.randomUUID();
    UUID orderableId2 = UUID.randomUUID();
    ShipmentLineItemDto lineItem2 = buildShipmentLineItemDto(lotId2, orderableId2);
    UUID lotId3 = UUID.randomUUID();
    UUID orderableId3 = UUID.randomUUID();
    ShipmentLineItemDto lineItem3 = buildShipmentLineItemDto(lotId3, orderableId3);
    draftDto.setLineItems(newArrayList(lineItem1, lineItem2, lineItem3));
    when(siglusShipmentDraftFulfillmentService.searchShipmentDraft(draftId))
        .thenReturn(draftDto);
    mockOrder();
    when(facilityConfigHelper.isLocationManagementEnabled(any())).thenReturn(false);
    when(shipmentDraftLineItemsRepository.reservedCount(any(), any()))
        .thenReturn(newArrayList());
    mockOrderables(orderableId1, orderableId2, orderableId3);
    mockLots(lotId1, lotId2, lotId3);
    StockCard stockCard1 = new StockCard();
    stockCard1.setId(stockCardId);
    stockCard1.setLotId(lotId1);
    stockCard1.setOrderableId(orderableId1);
    UUID stockCardId2 = UUID.randomUUID();
    StockCard stockCard2 = new StockCard();
    stockCard2.setId(stockCardId2);
    stockCard2.setLotId(lotId2);
    stockCard2.setOrderableId(orderableId2);
    when(siglusStockCardService.findStockCardIdByFacilityAndOrderables(any(), any()))
        .thenReturn(newArrayList(stockCardId, stockCardId2));
    when(siglusStockCardService.findStockCardByIds(any())).thenReturn(newArrayList(stockCard1, stockCard2));
    StockCardStockDto stockDto1 = StockCardStockDto.builder()
        .stockCardId(stockCardId)
        .orderableId(orderableId1)
        .lotId(lotId1)
        .stockOnHand(10)
        .build();
    StockCardStockDto stockDto2 = StockCardStockDto.builder()
        .stockCardId(stockCardId2)
        .orderableId(orderableId2)
        .lotId(lotId2)
        .stockOnHand(0)
        .build();
    when(siglusStockCardSummariesService.getLatestStockOnHandByIds(any(), anyBoolean()))
        .thenReturn(newArrayList(stockDto1, stockDto2));

    SiglusShipmentDraftDto shipmentDraft = siglusShipmentDraftService.getShipmentDraft(draftId);

    assertEquals(draftId, shipmentDraft.getId());
    assertEquals(1, shipmentDraft.getLineItems().size());
    SiglusShipmentDraftLineItemDto lineItemDto = shipmentDraft.getLineItems().get(0);
    assertNotNull(lineItemDto.getLot());
    assertNotNull(lineItemDto.getOrderable());
    assertTrue(lineItemDto.getStockOnHand() > 0);
  }

  @Test
  public void shouldGetShipmentDraftWithLocationSuccess() {
    ShipmentDraftDto draftDto = buildShipmentDraftDto();
    ShipmentLineItemDto lineItem = buildShipmentLineItemDto(lotId, orderableId);
    draftDto.setLineItems(newArrayList(lineItem));
    when(siglusShipmentDraftFulfillmentService.searchShipmentDraft(any())).thenReturn(draftDto);
    mockOrder();
    when(facilityConfigHelper.isLocationManagementEnabled(any())).thenReturn(true);
    when(shipmentDraftLineItemsByLocationRepository.findByShipmentDraftLineItemIdIn(any()))
        .thenReturn(newArrayList(buildShipmentDraftLineItemsExtension(lineItem.getId())));
    when(shipmentDraftLineItemsRepository.reservedCount(any(), any())).thenReturn(newArrayList());
    OrderableVersionDto orderableVersionDto = new OrderableVersionDto();
    orderableVersionDto.setId(orderableId);
    when(siglusOrderableService.findLatestVersionByIds(any())).thenReturn(newArrayList(orderableVersionDto));
    LotDto lotDto = new LotDto();
    lotDto.setId(lotId);
    when(siglusLotService.getLotList(any())).thenReturn(newArrayList(lotDto));
    when(siglusStockCardService.findStockCardIdByFacilityAndOrderables(any(), any()))
        .thenReturn(newArrayList(stockCardId));
    StockCard stockCard = new StockCard();
    stockCard.setId(stockCardId);
    stockCard.setLotId(lotId);
    stockCard.setOrderableId(orderableId);
    when(siglusStockCardService.findStockCardByIds(any())).thenReturn(newArrayList(stockCard));
    StockCardStockDto stockDto = StockCardStockDto.builder()
        .stockCardId(stockCardId)
        .orderableId(orderableId)
        .lotId(lotId)
        .locationCode(locationCode)
        .stockOnHand(10)
        .build();
    when(siglusStockCardSummariesService.getLatestStockOnHandByIds(any(), anyBoolean()))
        .thenReturn(newArrayList(stockDto));

    SiglusShipmentDraftDto shipmentDraft = siglusShipmentDraftService.getShipmentDraft(draftId);

    assertEquals(draftId, shipmentDraft.getId());
    assertEquals(1, shipmentDraft.getLineItems().size());
    SiglusShipmentDraftLineItemDto lineItemDto = shipmentDraft.getLineItems().get(0);
    assertNotNull(lineItemDto.getLot());
    assertNotNull(lineItemDto.getOrderable());
    assertNotNull(lineItemDto.getLocation());
    assertTrue(lineItemDto.getStockOnHand() > 0);
  }

  @Test
  public void shouldReturnEmptyListWhenGetShipmentDraftByOrderIdGivenNoDraftExist() {
    when(siglusShipmentDraftFulfillmentService.getShipmentDraftByOrderId(orderId))
        .thenReturn(new PageImpl<>(newArrayList()));

    List<SiglusShipmentDraftDto> drafts = siglusShipmentDraftService.getShipmentDraftByOrderId(orderId);

    assertEquals(0, drafts.size());
  }

  @Test
  public void shouldReturnDraftsWhenGetShipmentDraftByOrderIdGivenDraftExist() {
    ShipmentDraftDto draftDto = buildShipmentDraftDto();
    when(siglusShipmentDraftFulfillmentService.getShipmentDraftByOrderId(orderId))
        .thenReturn(new PageImpl<>(newArrayList(draftDto)));

    List<SiglusShipmentDraftDto> drafts = siglusShipmentDraftService.getShipmentDraftByOrderId(orderId);

    assertEquals(1, drafts.size());
  }

  private ShipmentDraftDto buildShipmentDraftDto() {
    OrderObjectReferenceDto order = new OrderObjectReferenceDto(orderId);
    order.setOrderLineItems(newArrayList());
    FacilityDto facilityDto = new FacilityDto();
    facilityDto.setId(UUID.randomUUID());
    order.setSupplyingFacility(facilityDto);
    ProgramDto programDto = new ProgramDto();
    programDto.setId(UUID.randomUUID());
    order.setProgram(programDto);
    ShipmentDraftDto draftDto = new ShipmentDraftDto();
    draftDto.setId(draftId);
    draftDto.setOrder(order);
    draftDto.setLineItems(new ArrayList<>());
    return draftDto;
  }

  private ShipmentLineItemDto buildShipmentLineItemDto(UUID lotId, UUID orderableId) {
    ShipmentLineItemDto lineItem = new ShipmentLineItemDto();
    lineItem.setId(UUID.randomUUID());
    lineItem.setLotId(lotId);
    VersionObjectReferenceDto product = new VersionObjectReferenceDto(orderableId, null, null, 1L);
    lineItem.setOrderable(product);
    return  lineItem;
  }

  private ShipmentDraftLineItemsExtension buildShipmentDraftLineItemsExtension(UUID lineItemId) {
    ShipmentDraftLineItemsExtension extension = new ShipmentDraftLineItemsExtension();
    extension.setShipmentDraftLineItemId(lineItemId);
    extension.setLocationCode(locationCode);
    extension.setArea(area);
    return extension;
  }

  private void mockOrder() {
    Order order = new Order();
    order.setId(orderId);
    order.setSupplyingFacilityId(UUID.randomUUID());
    when(orderRepository.findOne(orderId)).thenReturn(order);
  }

  private void mockOrderables(UUID orderableId1, UUID orderableId2, UUID orderableId3) {
    OrderableVersionDto orderable1 = new OrderableVersionDto();
    orderable1.setId(orderableId1);
    OrderableVersionDto orderable2 = new OrderableVersionDto();
    orderable2.setId(orderableId2);
    OrderableVersionDto orderable3 = new OrderableVersionDto();
    orderable3.setId(orderableId3);
    when(siglusOrderableService.findLatestVersionByIds(any()))
        .thenReturn(newArrayList(orderable1, orderable2, orderable3));
  }

  private void mockLots(UUID lotId1, UUID lotId2, UUID lotId3) {
    LotDto lotDto1 = new LotDto();
    lotDto1.setId(lotId1);
    LotDto lotDto2 = new LotDto();
    lotDto2.setId(lotId2);
    LotDto lotDto3 = new LotDto();
    lotDto3.setId(lotId3);
    when(siglusLotService.getLotList(any())).thenReturn(newArrayList(lotDto1, lotDto2, lotDto3));
  }
}
