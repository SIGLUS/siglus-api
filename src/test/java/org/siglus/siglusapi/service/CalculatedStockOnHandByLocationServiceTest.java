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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableSet;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.fulfillment.web.shipment.LocationDto;
import org.openlmis.fulfillment.web.shipment.ShipmentLineItemDto;
import org.openlmis.fulfillment.web.util.VersionObjectReferenceDto;
import org.openlmis.stockmanagement.domain.card.StockCard;
import org.openlmis.stockmanagement.domain.card.StockCardLineItem;
import org.openlmis.stockmanagement.domain.reason.ReasonType;
import org.openlmis.stockmanagement.domain.reason.StockCardLineItemReason;
import org.openlmis.stockmanagement.repository.CalculatedStockOnHandRepository;
import org.openlmis.stockmanagement.repository.StockCardRepository;
import org.openlmis.stockmanagement.service.referencedata.OrderableReferenceDataService;
import org.siglus.siglusapi.domain.CalculatedStockOnHandByLocation;
import org.siglus.siglusapi.domain.StockCardLineItemExtension;
import org.siglus.siglusapi.domain.StockCardLocationMovementLineItem;
import org.siglus.siglusapi.repository.CalculatedStockOnHandByLocationRepository;
import org.siglus.siglusapi.repository.SiglusStockCardRepository;
import org.siglus.siglusapi.repository.StockCardLineItemExtensionRepository;
import org.siglus.siglusapi.repository.StockCardLocationMovementLineItemRepository;

@SuppressWarnings({"PMD.UnusedPrivateField"})
@RunWith(MockitoJUnitRunner.class)
public class CalculatedStockOnHandByLocationServiceTest {

  @InjectMocks
  private CalculatedStocksOnHandByLocationService calculatedStocksOnHandByLocationService;
  @Mock
  private CalculatedStockOnHandByLocationRepository calculatedStockOnHandByLocationRepository;
  @Mock
  private CalculatedStockOnHandRepository calculatedStocksOnHandRepository;
  @Mock
  private SiglusStockCardRepository siglusStockCardRepository;
  @Mock
  private StockCardRepository stockCardRepository;
  @Mock
  private StockCardLineItemExtensionRepository stockCardLineItemExtensionRepository;
  @Mock
  private StockCardLocationMovementLineItemRepository locationMovementRepository;
  @Mock
  private OrderableReferenceDataService orderableService;

  @Captor
  private ArgumentCaptor<List<CalculatedStockOnHandByLocation>> sohByLocationArgumentCaptor;

  private final UUID stockCardId = UUID.randomUUID();
  private final UUID facilityId = UUID.randomUUID();
  private final UUID lotId = UUID.randomUUID();
  private final UUID orderableId = UUID.randomUUID();
  private final UUID shipmentLineItemId = UUID.randomUUID();
  private final UUID lineItemId1 = UUID.randomUUID();
  private final UUID lineItemId2 = UUID.randomUUID();
  private final UUID lineItemId3 = UUID.randomUUID();
  private final UUID lineItemId4 = UUID.randomUUID();
  private final String locationCode = "test-location";
  private final Integer previousSoh = 100;

  private final String locationCode1 = "test-location1";

  private final String locationCode2 = "test-location2";

  private final String locationCode3 = "test-location3";

  // test case, physical inventory 100, issue 30, receive 10
  @Test
  public void shouldCalculateSohByLocationWhenPhysicalInventoryIfNoMovement() {
    // given
    StockCardLineItem target = StockCardLineItem.builder().build();
    target.setId(lineItemId1);
    target.setQuantity(previousSoh);
    target.setOccurredDate(LocalDate.now().minusDays(5L));
    target.setProcessedDate(LocalDate.now().minusDays(5L).atStartOfDay(ZoneId.systemDefault()));
    StockCard stockCard = buildStockCard(target);
    target.setStockCard(stockCard);

    when(locationMovementRepository.findByStockCardId(stockCardId)).thenReturn(Collections.emptyList());
    when(stockCardLineItemExtensionRepository.findByStockCardLineItemId(any()))
            .thenReturn(Optional.of(buildExtension()));

    Map<UUID, List<StockCardLineItem>> stockCardIdToLineItems = new HashMap<>();
    stockCardIdToLineItems.put(stockCardId, stockCard.getLineItems());
    List<CalculatedStockOnHandByLocation> toSaveList = new ArrayList<>();
    // when
    calculatedStocksOnHandByLocationService.recalculateLocationStockOnHand(toSaveList, stockCardIdToLineItems,
            buildExtensionMap(), target, buildStockCardIdAndLocationCodeToPreviousStockOnHandMap(), new HashMap<>());

    // then
    assertEquals(3, toSaveList.size());
    assertEquals(100, (int) toSaveList.get(0).getStockOnHand());
    assertEquals(LocalDate.now().minusDays(5L), toDate(toSaveList.get(0).getOccurredDate()));
    assertEquals(70, (int) toSaveList.get(1).getStockOnHand());
    assertEquals(LocalDate.now().minusDays(3L), toDate(toSaveList.get(1).getOccurredDate()));
    assertEquals(80, (int) toSaveList.get(2).getStockOnHand());
    assertEquals(LocalDate.now().minusDays(1L), toDate(toSaveList.get(2).getOccurredDate()));
  }

  // test case, physical inventory 100, move in 40, issue 30, move out 20, receive 10
  @Test
  public void shouldCalculateSohByLocationWhenPhysicalInventoryIfLocationMovementExist() {
    // given
    StockCardLineItem target = StockCardLineItem.builder().build();
    target.setId(lineItemId1);
    target.setQuantity(previousSoh);
    target.setOccurredDate(LocalDate.now().minusDays(5L));
    target.setProcessedDate(LocalDate.now().minusDays(5L).atStartOfDay(ZoneId.systemDefault()));
    StockCard stockCard = buildStockCard(target);
    target.setStockCard(stockCard);

    when(stockCardLineItemExtensionRepository.findByStockCardLineItemId(any()))
            .thenReturn(Optional.of(buildExtension()));

    Map<UUID, List<StockCardLineItem>> stockCardIdToLineItems = new HashMap<>();
    stockCardIdToLineItems.put(stockCardId, stockCard.getLineItems());
    List<CalculatedStockOnHandByLocation> toSaveList = new ArrayList<>();
    // when
    calculatedStocksOnHandByLocationService.recalculateLocationStockOnHand(toSaveList, stockCardIdToLineItems,
            buildExtensionMap(), target, buildStockCardIdAndLocationCodeToPreviousStockOnHandMap(), buildMovementMap());

    // then
    assertEquals(5, toSaveList.size());
    assertEquals(100, (int) toSaveList.get(0).getStockOnHand());
    assertEquals(LocalDate.now().minusDays(5L), toDate(toSaveList.get(0).getOccurredDate()));
    assertEquals(140, (int) toSaveList.get(1).getStockOnHand());
    assertEquals(LocalDate.now().minusDays(4L), toDate(toSaveList.get(1).getOccurredDate()));
    assertEquals(110, (int) toSaveList.get(2).getStockOnHand());
    assertEquals(LocalDate.now().minusDays(3L), toDate(toSaveList.get(2).getOccurredDate()));
    assertEquals(90, (int) toSaveList.get(3).getStockOnHand());
    assertEquals(LocalDate.now().minusDays(2L), toDate(toSaveList.get(3).getOccurredDate()));
    assertEquals(100, (int) toSaveList.get(4).getStockOnHand());
    assertEquals(LocalDate.now().minusDays(1L), toDate(toSaveList.get(4).getOccurredDate()));
  }

  // test case, location1 -> location2 30, location2 -> location3 10
  @Test
  public void shouldCalculateSohByLocationWhenSubmitLocationMovement() {
    // given
    when(calculatedStockOnHandByLocationRepository.findPreviousLocationStockOnHandsTillNow(
            ImmutableSet.of(stockCardId), LocalDate.now()))
            .thenReturn(buildPreviousSohByLocation());

    // when
    calculatedStocksOnHandByLocationService.calculateStockOnHandByLocationForMovement(buildMovementList());

    // then
    verify(calculatedStockOnHandByLocationRepository).save(sohByLocationArgumentCaptor.capture());
    List<CalculatedStockOnHandByLocation> sohToSave = sohByLocationArgumentCaptor.getValue();
    assertEquals(3, sohToSave.size());

    CalculatedStockOnHandByLocation sohLocation1 = sohToSave.stream()
            .filter(s -> locationCode1.equals(s.getLocationCode())).findFirst().orElse(null);
    assertNotNull(sohLocation1);
    assertEquals(70, (int) sohLocation1.getStockOnHand());

    CalculatedStockOnHandByLocation sohLocation2 = sohToSave.stream()
            .filter(s -> locationCode2.equals(s.getLocationCode())).findFirst().orElse(null);
    assertNotNull(sohLocation2);
    assertEquals(120, (int) sohLocation2.getStockOnHand());

    CalculatedStockOnHandByLocation sohLocation3 = sohToSave.stream()
            .filter(s -> locationCode3.equals(s.getLocationCode())).findFirst().orElse(null);
    assertNotNull(sohLocation3);
    assertEquals(110, (int) sohLocation3.getStockOnHand());
  }

  @Test
  public void shouldCalculateSohByLocationWhenConfirmShipment() {
    // given
    when(calculatedStockOnHandByLocationRepository.findPreviousLocationStockOnHandsTillNow(
        ImmutableSet.of(stockCardId), LocalDate.now())).thenReturn(buildPreviousSohByLocation());
    when(stockCardRepository.findByFacilityIdAndOrderableIdAndLotId(facilityId, orderableId, lotId))
        .thenReturn(createStockCard());

    // when
    calculatedStocksOnHandByLocationService
        .calculateStockOnHandByLocationForShipment(buildShipmentLineItemList(), facilityId);

    // then
    verify(calculatedStockOnHandByLocationRepository).save(sohByLocationArgumentCaptor.capture());
    List<CalculatedStockOnHandByLocation> sohToSave = sohByLocationArgumentCaptor.getValue();
    assertEquals(1, sohToSave.size());
    CalculatedStockOnHandByLocation sohLocation1 = sohToSave.stream()
        .filter(s -> locationCode1.equals(s.getLocationCode())).findFirst().orElse(null);
    assertNotNull(sohLocation1);
    assertEquals(90, (int) sohLocation1.getStockOnHand());
  }

  private List<CalculatedStockOnHandByLocation> buildPreviousSohByLocation() {
    CalculatedStockOnHandByLocation soh1 = CalculatedStockOnHandByLocation.builder()
            .stockCardId(stockCardId)
            .locationCode(locationCode1)
            .stockOnHand(previousSoh)
            .build();
    CalculatedStockOnHandByLocation soh2 = CalculatedStockOnHandByLocation.builder()
            .stockCardId(stockCardId)
            .locationCode(locationCode2)
            .stockOnHand(previousSoh)
            .build();
    CalculatedStockOnHandByLocation soh3 = CalculatedStockOnHandByLocation.builder()
            .stockCardId(stockCardId)
            .locationCode(locationCode3)
            .stockOnHand(previousSoh)
            .build();

    return Arrays.asList(soh1, soh2, soh3);
  }

  private List<StockCardLocationMovementLineItem> buildMovementList() {
    StockCardLocationMovementLineItem movement1 = StockCardLocationMovementLineItem.builder()
            .stockCardId(stockCardId)
            .srcLocationCode(locationCode1)
            .destLocationCode(locationCode2)
            .quantity(30)
            .occurredDate(LocalDate.now())
            .processedDate(LocalDateTime.now())
            .build();
    StockCardLocationMovementLineItem movement2 = StockCardLocationMovementLineItem.builder()
            .stockCardId(stockCardId)
            .srcLocationCode(locationCode2)
            .destLocationCode(locationCode3)
            .quantity(10)
            .occurredDate(LocalDate.now())
            .processedDate(LocalDateTime.now())
            .build();

    return Arrays.asList(movement2, movement1);
  }

  private List<ShipmentLineItemDto> buildShipmentLineItemList() {
    ShipmentLineItemDto shipmentLineItemDto = new ShipmentLineItemDto();
    shipmentLineItemDto.setId(shipmentLineItemId);
    LocationDto locationDto = new LocationDto();
    locationDto.setLocationCode(locationCode1);
    shipmentLineItemDto.setLocation(locationDto);
    shipmentLineItemDto.setLotId(lotId);
    VersionObjectReferenceDto orderReferenceDto = new VersionObjectReferenceDto(orderableId,
        "", "", 1L);
    shipmentLineItemDto.setOrderable(orderReferenceDto);
    shipmentLineItemDto.setQuantityShipped(10L);

    return Collections.singletonList(shipmentLineItemDto);
  }

  private StockCard createStockCard() {
    StockCard stockCard = new StockCard();
    stockCard.setId(stockCardId);
    stockCard.setOrderableId(orderableId);
    stockCard.setFacilityId(facilityId);
    stockCard.setLotId(lotId);
    return stockCard;
  }

  private Map<UUID, List<StockCardLocationMovementLineItem>> buildMovementMap() {
    StockCardLocationMovementLineItem movement1 = StockCardLocationMovementLineItem.builder()
            .stockCardId(stockCardId)
            .srcLocationCode("")
            .destLocationCode(locationCode)
            .quantity(40)
            .occurredDate(LocalDate.now().minusDays(4L))
            .processedDate(LocalDateTime.now().minusDays(4L))
            .build();

    StockCardLocationMovementLineItem movement2 = StockCardLocationMovementLineItem.builder()
            .stockCardId(stockCardId)
            .srcLocationCode(locationCode)
            .destLocationCode("")
            .quantity(20)
            .occurredDate(LocalDate.now().minusDays(2L))
            .processedDate(LocalDateTime.now().minusDays(2L))
            .build();
    Map<UUID, List<StockCardLocationMovementLineItem>> stockCardIdToMovements = new HashMap<>();
    stockCardIdToMovements.put(stockCardId, Arrays.asList(movement2, movement1));
    return stockCardIdToMovements;
  }

  private StockCard buildStockCard(StockCardLineItem target) {
    StockCard stockCard = new StockCard();
    stockCard.setId(stockCardId);
    StockCardLineItem line2 = new StockCardLineItem();
    line2.setId(lineItemId2);
    line2.setQuantity(30);
    line2.setReason(buildIssueReason());
    line2.setOccurredDate(LocalDate.now().minusDays(3L));
    line2.setProcessedDate(LocalDate.now().minusDays(3L).atStartOfDay(ZoneId.systemDefault()));
    StockCardLineItem line3 = new StockCardLineItem();
    line3.setId(lineItemId3);
    line3.setQuantity(10);
    line3.setReason(buildReceiveReason());
    line3.setOccurredDate(LocalDate.now().minusDays(1L));
    line3.setProcessedDate(LocalDate.now().minusDays(1L).atStartOfDay(ZoneId.systemDefault()));
    stockCard.setLineItems(Arrays.asList(target, line2, line3));
    return stockCard;
  }

  private StockCardLineItemReason buildReceiveReason() {
    StockCardLineItemReason reason = new StockCardLineItemReason();
    reason.setName("Receive");
    reason.setReasonType(ReasonType.CREDIT);
    return reason;
  }

  private StockCardLineItemReason buildIssueReason() {
    StockCardLineItemReason reason = new StockCardLineItemReason();
    reason.setName("Issue");
    reason.setReasonType(ReasonType.DEBIT);
    return reason;
  }

  private Map<String, Integer> buildStockCardIdAndLocationCodeToPreviousStockOnHandMap() {
    Map<String, Integer> map = new HashMap<>();
    map.put(stockCardId + locationCode, previousSoh);
    return map;
  }

  private Map<UUID, StockCardLineItemExtension> buildExtensionMap() {
    Map<UUID, StockCardLineItemExtension> map = new HashMap<>();
    map.put(lineItemId1, buildExtension());
    map.put(lineItemId2, buildExtension());
    map.put(lineItemId3, buildExtension());
    map.put(lineItemId4, buildExtension());
    return map;
  }

  private StockCardLineItemExtension buildExtension() {
    return StockCardLineItemExtension.builder().locationCode(locationCode).build();
  }

  private LocalDate toDate(Date date) {
    return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
  }
}
