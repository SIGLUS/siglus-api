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
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.stockmanagement.domain.card.StockCard;
import org.openlmis.stockmanagement.domain.card.StockCardLineItem;
import org.openlmis.stockmanagement.domain.reason.ReasonType;
import org.openlmis.stockmanagement.domain.reason.StockCardLineItemReason;
import org.openlmis.stockmanagement.repository.CalculatedStockOnHandRepository;
import org.openlmis.stockmanagement.service.referencedata.OrderableReferenceDataService;
import org.siglus.siglusapi.domain.CalculatedStockOnHandByLocation;
import org.siglus.siglusapi.domain.StockCardLineItemExtension;
import org.siglus.siglusapi.repository.CalculatedStockOnHandByLocationRepository;
import org.siglus.siglusapi.repository.SiglusStockCardRepository;
import org.siglus.siglusapi.repository.StockCardLineItemExtensionRepository;
import org.siglus.siglusapi.repository.StockCardLocationMovementLineItemRepository;

@SuppressWarnings({"PMD.UnusedPrivateField"})
@RunWith(MockitoJUnitRunner.class)
public class CalculatedStockOnHandByLocationServiceTest {

  @InjectMocks
  private CalculatedStocksOnHandLocationsService calculatedStocksOnHandLocationsService;
  @Mock
  private CalculatedStockOnHandByLocationRepository calculatedStockOnHandByLocationRepository;
  @Mock
  private CalculatedStockOnHandRepository calculatedStocksOnHandRepository;
  @Mock
  private SiglusStockCardRepository siglusStockCardRepository;
  @Mock
  private StockCardLineItemExtensionRepository stockCardLineItemExtensionRepository;
  @Mock
  private StockCardLocationMovementLineItemRepository locationMovementRepository;
  @Mock
  private OrderableReferenceDataService orderableService;

  private final UUID stockCardId = UUID.randomUUID();
  private final UUID lineItemId1 = UUID.randomUUID();
  private final UUID lineItemId2 = UUID.randomUUID();
  private final UUID lineItemId3 = UUID.randomUUID();
  private final UUID lineItemId4 = UUID.randomUUID();
  private final String locationCode = "test-location";
  private final Integer previousSoh = 100;

  @Test
  public void shouldAllCalculatedStockOnHandByLocationIfNoMovement() {
    // given
    StockCardLineItem target = StockCardLineItem.builder().build();
    target.setId(lineItemId1);
    target.setQuantity(previousSoh);
    target.setOccurredDate(LocalDate.now().minusDays(3L));
    StockCard stockCard = buildStockCard(target);
    target.setStockCard(stockCard);

    when(locationMovementRepository.findByStockCardId(stockCardId)).thenReturn(Collections.emptyList());
    when(stockCardLineItemExtensionRepository.findByStockCardLineItemId(any()))
            .thenReturn(Optional.of(buildExtension()));

    List<CalculatedStockOnHandByLocation> toSaveList = new ArrayList<>();
    // when
    calculatedStocksOnHandLocationsService.recalculateLocationStockOnHand(
            toSaveList, buildExtensionMap(), target, buildStockCardIdAndLocationCodeToPreviousStockOnHandMap());

    // then
    // test case, physical inventory 100, issue 30, receive 10
    assertEquals(3, toSaveList.size());
    assertEquals(100, (int) toSaveList.get(0).getStockonhand());
    assertEquals(LocalDate.now().minusDays(3L), toDate(toSaveList.get(0).getOccurreddate()));
    assertEquals(70, (int) toSaveList.get(1).getStockonhand());
    assertEquals(LocalDate.now().minusDays(2L), toDate(toSaveList.get(1).getOccurreddate()));
    assertEquals(80, (int) toSaveList.get(2).getStockonhand());
    assertEquals(LocalDate.now().minusDays(1L), toDate(toSaveList.get(2).getOccurreddate()));
  }

  private StockCard buildStockCard(StockCardLineItem target) {
    StockCard stockCard = new StockCard();
    stockCard.setId(stockCardId);
    StockCardLineItem line2 = new StockCardLineItem();
    line2.setId(lineItemId2);
    line2.setQuantity(30);
    line2.setReason(buildIssueReason());
    line2.setOccurredDate(LocalDate.now().minusDays(2L));
    StockCardLineItem line3 = new StockCardLineItem();
    line3.setId(lineItemId3);
    line3.setQuantity(10);
    line3.setReason(buildReceiveReason());
    line3.setOccurredDate(LocalDate.now().minusDays(1L));
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
