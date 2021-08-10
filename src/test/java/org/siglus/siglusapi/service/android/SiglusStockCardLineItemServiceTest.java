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

package org.siglus.siglusapi.service.android;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.powermock.api.mockito.PowerMockito.when;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.stockmanagement.domain.card.StockCard;
import org.openlmis.stockmanagement.domain.card.StockCardLineItem;
import org.openlmis.stockmanagement.domain.event.CalculatedStockOnHand;
import org.openlmis.stockmanagement.domain.event.StockEvent;
import org.openlmis.stockmanagement.domain.physicalinventory.PhysicalInventoryLineItemAdjustment;
import org.openlmis.stockmanagement.domain.reason.ReasonType;
import org.openlmis.stockmanagement.domain.reason.StockCardLineItemReason;
import org.openlmis.stockmanagement.domain.sourcedestination.Node;
import org.openlmis.stockmanagement.domain.sourcedestination.Organization;
import org.openlmis.stockmanagement.dto.referencedata.UserDto;
import org.openlmis.stockmanagement.repository.CalculatedStockOnHandRepository;
import org.openlmis.stockmanagement.repository.OrganizationRepository;
import org.openlmis.stockmanagement.testutils.CalculatedStockOnHandDataBuilder;
import org.openlmis.stockmanagement.util.StockmanagementAuthenticationHelper;
import org.siglus.siglusapi.domain.StockEventProductRequested;
import org.siglus.siglusapi.dto.android.response.LotMovementItemResponse;
import org.siglus.siglusapi.dto.android.response.SiglusLotResponse;
import org.siglus.siglusapi.dto.android.response.SiglusStockMovementItemResponse;
import org.siglus.siglusapi.repository.SiglusStockCardLineItemRepository;
import org.siglus.siglusapi.repository.StockEventProductRequestedRepository;
import org.springframework.data.domain.Example;

@SuppressWarnings("PMD.TooManyMethods")
@RunWith(MockitoJUnitRunner.class)
public class SiglusStockCardLineItemServiceTest {

  @Mock
  private SiglusStockCardLineItemRepository stockCardLineItemRepository;

  @Mock
  private CalculatedStockOnHandRepository calculatedStockOnHandRepository;

  @Mock
  private OrganizationRepository organizationRepository;

  @Mock
  private StockEventProductRequestedRepository requestQuantityRepository;

  @Mock
  private StockmanagementAuthenticationHelper authenticationHelper;

  @InjectMocks
  private SiglusStockCardLineItemService stockCardLineItemService;

  private final UUID stockCardId1 = UUID.randomUUID();

  private final UUID stockCardId2 = UUID.randomUUID();

  private final UUID stockCardId3 = UUID.randomUUID();

  private final UUID stockCardId4 = UUID.randomUUID();

  private final UUID stockCardId5 = UUID.randomUUID();

  private final UUID orderableId1 = UUID.randomUUID();

  private final UUID orderableId2 = UUID.randomUUID();

  private final UUID eventId = UUID.randomUUID();

  private final UUID orderableId3 = UUID.randomUUID();

  private final UUID lotId1FromOrderable1 = UUID.randomUUID();

  private final UUID lotId2FromOrderable1 = UUID.randomUUID();

  private final UUID lotId1FromOrderable2 = UUID.randomUUID();

  private final UUID lotId2FromOrderable2 = UUID.randomUUID();

  private final String lotId1FromOrderable1Code = "lotId1FromOrderable1Code";

  private final String lotId1FromOrderable2Code = "lotId1FromOrderable2Code";

  private final String lotId2FromOrderable2Code = "lotId2FromOrderable2Code";

  private final String day0701 = "2021-07-01";

  private final String day0702 = "2021-07-02";

  private final String day070108 = "2021-07-01 08:00:00";

  private final String day070109 = "2021-07-01 09:00:00";

  private final String day070110 = "2021-07-01 10:00:00";

  private final String day070111 = "2021-07-01 11:00:00";

  private final String day070112 = "2021-07-01 12:00:00";

  private final String day070208 = "2021-07-02 08:00:00";

  private final String day070209 = "2021-07-02 09:00:00";

  private final String day070210 = "2021-07-02 10:00:00";

  private final String dayOrigin070108 = "2021-07-01T08:00:10Z";

  private final String dayOrigin070109 = "2021-07-01T09:00:10Z";

  private final String dayOrigin070110 = "2021-07-01T10:00:10Z";

  private final String dayOrigin070111 = "2021-07-01T11:00:10Z";

  private final String dayOrigin070112 = "2021-07-01T12:00:10Z";

  private final String dayOrigin070208 = "2021-07-02T08:00:10Z";

  private final String dayOrigin070209 = "2021-07-02T09:00:10Z";

  private final String dayOrigin070210 = "2021-07-02T10:00:10Z";

  private final String physicalInventory = "PHYSICAL_INVENTORY";

  private final String customerReturn = "CUSTOMER_RETURN";

  private final String adjustmentName = "Devoluções de clientes (US e Enfermarias Dependentes)";

  private final UUID orgSourceId = UUID.randomUUID();

  private final UUID orgDestinationId = UUID.randomUUID();

  private final UUID homefacilityId = UUID.randomUUID();

  private final String signature = "yyd";

  private final String documentNumber = "documentTest";

  private StockEvent event;

  private final Map<UUID, SiglusLotResponse> siglusLotResponseByLotId = new HashMap<>();

  @Before
  public void prepare() {
    UserDto userDto = new UserDto();
    userDto.setHomeFacilityId(homefacilityId);
    when(authenticationHelper.getCurrentUser()).thenReturn(userDto);
    when(organizationRepository.findAll()).thenReturn(createOrganization());
    event = new StockEvent();
    event.setId(eventId);
    when(requestQuantityRepository.findOne(any(Example.class)))
        .thenReturn(StockEventProductRequested.builder().requestedQuantity(200).build());
    initSiglusLotResponse();
  }

  @Test
  public void shouldEqualStockMovementItemsWhenMove() {
    // given
    createStockMovements();
    // when
    Map<UUID, List<SiglusStockMovementItemResponse>> stockMovementItemResponseMap = stockCardLineItemService
        .getStockMovementByOrderableId(homefacilityId, "2021-06-30", "2021-07-03", null, null,
            siglusLotResponseByLotId);

    // then
    // 2021-07-01 08:00:00 physicalInventory positive 50
    SiglusStockMovementItemResponse stockMovementItemResponse1 = stockMovementItemResponseMap.get(orderableId1).stream()
        .filter(i -> i.getProcessedDate().toString().equals(dayOrigin070108))
        .findFirst().orElse(new SiglusStockMovementItemResponse());
    LotMovementItemResponse lotMovementItemResponse1 = stockMovementItemResponse1.getLotMovementItems().stream()
        .filter(i -> i.getLotCode().equals(lotId1FromOrderable1Code))
        .findFirst().orElse(new LotMovementItemResponse());

    assertEquals(Integer.valueOf(200), lotMovementItemResponse1.getStockOnHand());
    assertEquals(Integer.valueOf(50), lotMovementItemResponse1.getQuantity());
    assertEquals("INVENTORY_POSITIVE", lotMovementItemResponse1.getReason());

    // 2021-07-01 09:00:00 issue 10
    SiglusStockMovementItemResponse stockMovementItemResponse2 = stockMovementItemResponseMap.get(orderableId1).stream()
        .filter(i -> i.getProcessedDate().toString().equals(dayOrigin070109))
        .findFirst().orElse(new SiglusStockMovementItemResponse());
    LotMovementItemResponse lotMovementItemResponse2 = stockMovementItemResponse2.getLotMovementItems().stream()
        .filter(i -> i.getLotCode().equals(lotId1FromOrderable1Code))
        .findFirst().orElse(new LotMovementItemResponse());

    assertEquals(Integer.valueOf(190), lotMovementItemResponse2.getStockOnHand());
    assertEquals(Integer.valueOf(-10), lotMovementItemResponse2.getQuantity());
    assertEquals("PUB_PHARMACY", lotMovementItemResponse2.getReason());

    // 2021-07-01 10:00:00 receive 30
    SiglusStockMovementItemResponse stockMovementItemResponse3 = stockMovementItemResponseMap.get(orderableId1).stream()
        .filter(i -> i.getProcessedDate().toString().equals(dayOrigin070110))
        .findFirst().orElse(new SiglusStockMovementItemResponse());
    LotMovementItemResponse lotMovementItemResponse3 = stockMovementItemResponse3.getLotMovementItems().stream()
        .filter(i -> i.getLotCode().equals(lotId1FromOrderable1Code))
        .findFirst().orElse(new LotMovementItemResponse());

    assertEquals(Integer.valueOf(220), lotMovementItemResponse3.getStockOnHand());
    assertEquals(Integer.valueOf(30), lotMovementItemResponse3.getQuantity());
    assertEquals("DISTRICT_DDM", lotMovementItemResponse3.getReason());

    // 2021-07-01 11:00:00 physicalInventory inventory 220
    SiglusStockMovementItemResponse stockMovementItemResponse4 = stockMovementItemResponseMap.get(orderableId1).stream()
        .filter(i -> i.getProcessedDate().toString().equals(dayOrigin070111))
        .findFirst().orElse(new SiglusStockMovementItemResponse());
    LotMovementItemResponse lotMovementItemResponse4 = stockMovementItemResponse4.getLotMovementItems().stream()
        .filter(i -> i.getLotCode().equals(lotId1FromOrderable1Code))
        .findFirst().orElse(new LotMovementItemResponse());

    assertEquals(Integer.valueOf(220), lotMovementItemResponse4.getStockOnHand());
    assertEquals(Integer.valueOf(0), lotMovementItemResponse4.getQuantity());
    assertEquals("INVENTORY", lotMovementItemResponse4.getReason());

    // 2021-07-01 12:00:00 adjustment positive 50
    SiglusStockMovementItemResponse stockMovementItemResponse5 = stockMovementItemResponseMap.get(orderableId1).stream()
        .filter(i -> i.getProcessedDate().toString().equals(dayOrigin070112))
        .findFirst().orElse(new SiglusStockMovementItemResponse());
    LotMovementItemResponse lotMovementItemResponse5 = stockMovementItemResponse5.getLotMovementItems().stream()
        .filter(i -> i.getLotCode().equals(lotId1FromOrderable1Code))
        .findFirst().orElse(new LotMovementItemResponse());

    assertEquals(Integer.valueOf(270), lotMovementItemResponse5.getStockOnHand());
    assertEquals(Integer.valueOf(50), lotMovementItemResponse5.getQuantity());
    assertEquals(customerReturn, lotMovementItemResponse5.getReason());

    // 2021-07-02 08:00:00 adjustment negative 10
    SiglusStockMovementItemResponse stockMovementItemResponse6 = stockMovementItemResponseMap.get(orderableId1).stream()
        .filter(i -> i.getProcessedDate().toString().equals(dayOrigin070208))
        .findFirst().orElse(new SiglusStockMovementItemResponse());
    LotMovementItemResponse lotMovementItemResponse6 = stockMovementItemResponse6.getLotMovementItems().stream()
        .filter(i -> i.getLotCode().equals(lotId1FromOrderable1Code))
        .findFirst().orElse(new LotMovementItemResponse());

    assertEquals(Integer.valueOf(260), lotMovementItemResponse6.getStockOnHand());
    assertEquals(Integer.valueOf(-10), lotMovementItemResponse6.getQuantity());
    assertEquals(customerReturn, lotMovementItemResponse6.getReason());

    // 2021-07-02 09:00:00 physicalInventory negative 180
    SiglusStockMovementItemResponse stockMovementItemResponse7 = stockMovementItemResponseMap.get(orderableId1).stream()
        .filter(i -> i.getProcessedDate().toString().equals(dayOrigin070209))
        .findFirst().orElse(new SiglusStockMovementItemResponse());
    LotMovementItemResponse lotMovementItemResponse7 = stockMovementItemResponse7.getLotMovementItems().stream()
        .filter(i -> i.getLotCode().equals(lotId1FromOrderable1Code))
        .findFirst().orElse(new LotMovementItemResponse());

    assertEquals(Integer.valueOf(180), lotMovementItemResponse7.getStockOnHand());
    assertEquals(Integer.valueOf(-80), lotMovementItemResponse7.getQuantity());
    assertEquals("INVENTORY_NEGATIVE", lotMovementItemResponse7.getReason());

    // 2021-07-02 10:00:00 issue 10
    SiglusStockMovementItemResponse stockMovementItemResponse8 = stockMovementItemResponseMap.get(orderableId1).stream()
        .filter(i -> i.getProcessedDate().toString().equals(dayOrigin070210))
        .findFirst().orElse(new SiglusStockMovementItemResponse());
    LotMovementItemResponse lotMovementItemResponse8 = stockMovementItemResponse8.getLotMovementItems().stream()
        .filter(i -> i.getLotCode().equals(lotId1FromOrderable1Code))
        .findFirst().orElse(new LotMovementItemResponse());

    assertEquals(Integer.valueOf(170), lotMovementItemResponse8.getStockOnHand());
    assertEquals(Integer.valueOf(-10), lotMovementItemResponse8.getQuantity());
    assertEquals("PUB_PHARMACY", lotMovementItemResponse8.getReason());
  }

  @Test
  public void shouldEqualStockMovementItemsWhenAdjustmentNoLot() {
    // given
    createNoLotStockMovements();
    // when
    Map<UUID, List<SiglusStockMovementItemResponse>> stockMovementItemResponseMap = stockCardLineItemService
        .getStockMovementByOrderableId(homefacilityId, "2021-06-29", "2021-07-05", null, null,
            siglusLotResponseByLotId);

    // then
    // 2021-07-01 08:00:00 physicalInventory positive 50
    SiglusStockMovementItemResponse stockMovementItemResponse1 = stockMovementItemResponseMap.get(orderableId3).stream()
        .filter(i -> i.getProcessedDate().toString().equals(dayOrigin070108))
        .findFirst().orElse(new SiglusStockMovementItemResponse());
    assertEquals(Integer.valueOf(200), stockMovementItemResponse1.getStockOnHand());
    assertEquals(Integer.valueOf(50), stockMovementItemResponse1.getMovementQuantity());
    assertEquals("INVENTORY_POSITIVE", stockMovementItemResponse1.getReason());
    assertEquals(physicalInventory, stockMovementItemResponse1.getType());

    // 2021-07-01 11:00:00 physicalInventory inventory 220
    SiglusStockMovementItemResponse stockMovementItemResponse4 = stockMovementItemResponseMap.get(orderableId3).stream()
        .filter(i -> i.getProcessedDate().toString().equals(dayOrigin070111))
        .findFirst().orElse(new SiglusStockMovementItemResponse());

    assertEquals(Integer.valueOf(220), stockMovementItemResponse4.getStockOnHand());
    assertEquals(Integer.valueOf(0), stockMovementItemResponse4.getMovementQuantity());
    assertEquals("INVENTORY", stockMovementItemResponse4.getReason());
    assertEquals(physicalInventory, stockMovementItemResponse4.getType());

    // 2021-07-01 12:00:00 adjustment positive 50
    SiglusStockMovementItemResponse stockMovementItemResponse5 = stockMovementItemResponseMap.get(orderableId3).stream()
        .filter(i -> i.getProcessedDate().toString().equals(dayOrigin070112))
        .findFirst().orElse(new SiglusStockMovementItemResponse());

    assertEquals(Integer.valueOf(270), stockMovementItemResponse5.getStockOnHand());
    assertEquals(Integer.valueOf(50), stockMovementItemResponse5.getMovementQuantity());
    assertEquals(customerReturn, stockMovementItemResponse5.getReason());
    assertEquals("ADJUSTMENT", stockMovementItemResponse5.getType());

    // 2021-07-02 08:00:00 adjustment negative 10
    SiglusStockMovementItemResponse stockMovementItemResponse6 = stockMovementItemResponseMap.get(orderableId3).stream()
        .filter(i -> i.getProcessedDate().toString().equals(dayOrigin070208))
        .findFirst().orElse(new SiglusStockMovementItemResponse());

    assertEquals(Integer.valueOf(260), stockMovementItemResponse6.getStockOnHand());
    assertEquals(Integer.valueOf(-10), stockMovementItemResponse6.getMovementQuantity());
    assertEquals(customerReturn, stockMovementItemResponse6.getReason());
    assertEquals("ADJUSTMENT", stockMovementItemResponse6.getType());

    // 2021-07-02 09:00:00 physicalInventory negative 180
    SiglusStockMovementItemResponse stockMovementItemResponse7 = stockMovementItemResponseMap.get(orderableId3).stream()
        .filter(i -> i.getProcessedDate().toString().equals(dayOrigin070209))
        .findFirst().orElse(new SiglusStockMovementItemResponse());

    assertEquals(Integer.valueOf(180), stockMovementItemResponse7.getStockOnHand());
    assertEquals(Integer.valueOf(-80), stockMovementItemResponse7.getMovementQuantity());
    assertEquals("INVENTORY_NEGATIVE", stockMovementItemResponse7.getReason());
    assertEquals(physicalInventory, stockMovementItemResponse7.getType());

    // 2021-07-02 10:00:00 issue 10
    SiglusStockMovementItemResponse stockMovementItemResponse8 = stockMovementItemResponseMap.get(orderableId3).stream()
        .filter(i -> i.getProcessedDate().toString().equals(dayOrigin070210))
        .findFirst().orElse(new SiglusStockMovementItemResponse());

    assertEquals(Integer.valueOf(190), stockMovementItemResponse8.getStockOnHand());
    assertEquals(Integer.valueOf(10), stockMovementItemResponse8.getMovementQuantity());
    assertNull(stockMovementItemResponse8.getReason());
    assertEquals("UNPACK_KIT", stockMovementItemResponse8.getType());
  }

  @Test
  public void shouldEqualStockMovementItemsWhenIssueReceiveNoLot() {
    // given
    createNoLotStockMovements();
    // when
    Map<UUID, List<SiglusStockMovementItemResponse>> stockMovementItemResponseMap = stockCardLineItemService
        .getStockMovementByOrderableId(homefacilityId, "2021-06-28", "2021-07-06", null, null,
            siglusLotResponseByLotId);
    // then
    // 2021-07-01 09:00:00 issue 10
    SiglusStockMovementItemResponse stockMovementItemResponse2 = stockMovementItemResponseMap.get(orderableId3).stream()
        .filter(i -> i.getProcessedDate().toString().equals(dayOrigin070109))
        .findFirst().orElse(new SiglusStockMovementItemResponse());
    assertNull(stockMovementItemResponse2.getLotMovementItems());

    assertEquals(Integer.valueOf(190), stockMovementItemResponse2.getStockOnHand());
    assertEquals(Integer.valueOf(-10), stockMovementItemResponse2.getMovementQuantity());
    assertEquals("PUB_PHARMACY", stockMovementItemResponse2.getReason());
    assertEquals("ISSUE", stockMovementItemResponse2.getType());

    // 2021-07-01 10:00:00 receive 30
    SiglusStockMovementItemResponse stockMovementItemResponse3 = stockMovementItemResponseMap.get(orderableId3).stream()
        .filter(i -> i.getProcessedDate().toString().equals(dayOrigin070110))
        .findFirst().orElse(new SiglusStockMovementItemResponse());

    assertEquals(Integer.valueOf(220), stockMovementItemResponse3.getStockOnHand());
    assertEquals(Integer.valueOf(30), stockMovementItemResponse3.getMovementQuantity());
    assertEquals("DISTRICT_DDM", stockMovementItemResponse3.getReason());
    assertEquals("RECEIVE", stockMovementItemResponse3.getType());
  }

  @Test
  public void shouldEqualStockMovementItemInfo() {
    // given
    createStockMovements();
    // when
    Map<UUID, List<SiglusStockMovementItemResponse>> stockMovementItemResponseMap = stockCardLineItemService
        .getStockMovementByOrderableId(homefacilityId, "2021-06-27", "2021-07-07", null, null,
            siglusLotResponseByLotId);

    // then
    // 2021-07-01 08:00:00 physicalInventory positive 50
    SiglusStockMovementItemResponse stockMovementItemResponse1 = stockMovementItemResponseMap.get(orderableId1).stream()
        .filter(i -> i.getProcessedDate().toString().equals(dayOrigin070108))
        .findFirst().orElse(new SiglusStockMovementItemResponse());
    assertEquals(Integer.valueOf(50), stockMovementItemResponse1.getMovementQuantity());
    assertEquals(Integer.valueOf(200), stockMovementItemResponse1.getStockOnHand());
    assertEquals(physicalInventory, stockMovementItemResponse1.getType());
    assertEquals(signature, stockMovementItemResponse1.getSignature());
    assertEquals(Integer.valueOf(200), stockMovementItemResponse1.getRequested());

    LotMovementItemResponse lotMovementItemResponse1 = stockMovementItemResponse1.getLotMovementItems().stream()
        .filter(i -> i.getLotCode().equals(lotId1FromOrderable1Code))
        .findFirst().orElse(new LotMovementItemResponse());
    assertEquals(documentNumber, lotMovementItemResponse1.getDocumentNumber());

    // 2021-07-01 10:00:00 receive 30+40 220 140
    SiglusStockMovementItemResponse stockMovementItemResponse2 = stockMovementItemResponseMap.get(orderableId1).stream()
        .filter(i -> i.getProcessedDate().toString().equals(dayOrigin070110))
        .findFirst().orElse(new SiglusStockMovementItemResponse());
    assertEquals(Integer.valueOf(70), stockMovementItemResponse2.getMovementQuantity());
    assertEquals(Integer.valueOf(360), stockMovementItemResponse2.getStockOnHand());
    assertEquals("RECEIVE", stockMovementItemResponse2.getType());

    // 2021-07-02 09:00:00 physicalInventory negative -80 -80 180 60
    SiglusStockMovementItemResponse stockMovementItemResponse3 = stockMovementItemResponseMap.get(orderableId1).stream()
        .filter(i -> i.getProcessedDate().toString().equals(dayOrigin070209))
        .findFirst().orElse(new SiglusStockMovementItemResponse());
    assertEquals(Integer.valueOf(-160), stockMovementItemResponse3.getMovementQuantity());
    assertEquals(Integer.valueOf(240), stockMovementItemResponse3.getStockOnHand());
    assertEquals(physicalInventory, stockMovementItemResponse3.getType());
  }

  private void createStockMovements() {
    StockCard orderable1lot1 = createStockCardToOrderableOneAndLotOne();
    StockCard orderable1lot2 = createStockCardToOrderableOneAndLotTwo();

    StockCard orderable2lot1 = createStockCardToOrderableTwoAndLotOne();
    StockCard orderable2lot2 = createStockCardToOrderableTwoAndLotTwo();

    // 2021-07-01 08:00:00 physicalInventory positive
    StockCardLineItem physicalInventoryItem1 = createStockCardLineItem(null, null, orderable1lot1, 200,
        day070108, dayOrigin070108, day0701, positiveAdjustment(), null);

    Node destinationNode = new Node();
    destinationNode.setReferenceId(orgDestinationId);
    // 2021-07-01 09:00:00 issue
    StockCardLineItem issueItem1 = createStockCardLineItem(destinationNode, null, orderable1lot1, 10,
        day070109, dayOrigin070109, day0701, null, null);
    StockCardLineItem issueItem2 = createStockCardLineItem(destinationNode, null, orderable2lot1, 20,
        day070109, dayOrigin070109, day0701, null, null);

    Node sourceNode = new Node();
    sourceNode.setReferenceId(orgSourceId);
    // 2021-07-01 10:00:00 receive
    StockCardLineItem receiveItem1 = createStockCardLineItem(null, sourceNode, orderable1lot1, 30,
        day070110, dayOrigin070110, day0701, null, null);
    StockCardLineItem receiveItem2 = createStockCardLineItem(null, sourceNode, orderable1lot2, 40,
        day070110, dayOrigin070110, day0701, null, null);

    // 2021-07-01 11:00:00 physicalInventory inventory
    StockCardLineItem physicalInventoryItem2 = createStockCardLineItem(null, null, orderable1lot1, 220,
        day070111, dayOrigin070111, day0701, null, null);

    // 2021-07-01 12:00:00 adjustment positive
    StockCardLineItemReason reason1 = StockCardLineItemReason.builder()
        .name(adjustmentName).reasonType(ReasonType.CREDIT).build();
    StockCardLineItem adjustmentItem1 = createStockCardLineItem(null, null, orderable1lot1, 50,
        day070112, dayOrigin070112, day0701, null, reason1);
    StockCardLineItem adjustmentItem2 = createStockCardLineItem(null, null, orderable2lot2, 60,
        day070112, dayOrigin070112, day0701, null, reason1);

    // 2021-07-02 08:00:00 adjustment negative
    StockCardLineItemReason reason2 = StockCardLineItemReason.builder()
        .name(adjustmentName).reasonType(ReasonType.DEBIT).build();
    StockCardLineItem adjustmentItem3 = createStockCardLineItem(null, null, orderable1lot1, 10,
        day070208, dayOrigin070208, day0702, null, reason2);
    StockCardLineItem adjustmentItem4 = createStockCardLineItem(null, null, orderable2lot1, 20,
        day070208, dayOrigin070208, day0702, null, reason2);

    // 2021-07-02 09:00:00 physicalInventory negative
    StockCardLineItem physicalInventoryItem3 = createStockCardLineItem(null, null, orderable1lot1, 180,
        day070209, dayOrigin070209, day0702, negativeAdjustment(), null);
    StockCardLineItem physicalInventoryItem4 = createStockCardLineItem(null, null, orderable1lot2, 60,
        day070209, dayOrigin070209, day0702, negativeAdjustment(), null);

    // 2021-07-02 10:00:00 issue
    StockCardLineItem issueItem3 = createStockCardLineItem(destinationNode, null, orderable1lot1, 10,
        day070210, dayOrigin070210, day0702, null, null);
    StockCardLineItem issueItem4 = createStockCardLineItem(destinationNode, null, orderable2lot1, 20,
        day070210, dayOrigin070210, day0702, null, null);

    List<StockCardLineItem> stockCardLineItemList = Arrays
        .asList(physicalInventoryItem1, issueItem1, issueItem2, receiveItem1, receiveItem2, physicalInventoryItem2,
            adjustmentItem1, adjustmentItem2, adjustmentItem3, adjustmentItem4, physicalInventoryItem3,
            physicalInventoryItem4, issueItem3,
            issueItem4);

    // 2021-07-01
    CalculatedStockOnHand sohOrderable1lot1Day1 = createCalculatedStockOnHand(orderable1lot1, day0701, 270);
    CalculatedStockOnHand sohOrderable1lot2Day1 = createCalculatedStockOnHand(orderable1lot2, day0701, 140);
    CalculatedStockOnHand sohOrderable2lot1Day1 = createCalculatedStockOnHand(orderable2lot1, day0701, 80);
    CalculatedStockOnHand sohOrderable2lot2Day1 = createCalculatedStockOnHand(orderable2lot2, day0701, 160);

    // 2021-07-02
    CalculatedStockOnHand sohOrderable1lot1Day2 = createCalculatedStockOnHand(orderable1lot1, day0702, 170);
    CalculatedStockOnHand sohOrderable1lot2Day2 = createCalculatedStockOnHand(orderable1lot2, day0702, 60);
    CalculatedStockOnHand sohOrderable2lot1Day2 = createCalculatedStockOnHand(orderable2lot1, day0702, 40);

    List<CalculatedStockOnHand> stockOnHandList = Arrays
        .asList(sohOrderable1lot1Day1, sohOrderable1lot2Day1, sohOrderable2lot1Day1, sohOrderable2lot2Day1,
            sohOrderable1lot1Day2, sohOrderable1lot2Day2, sohOrderable2lot1Day2);

    when(stockCardLineItemRepository
        .findByFacilityIdAndIdStartTimeEndTime(any(UUID.class), any(String.class), any(String.class)))
        .thenReturn(stockCardLineItemList);
    when(calculatedStockOnHandRepository
        .findByFacilityIdAndIdStartTimeEndTime(any(UUID.class), any(String.class), any(String.class)))
        .thenReturn(stockOnHandList);
  }

  private void createNoLotStockMovements() {
    StockCard orderableNolot = createStockCardToOrderableThreeAndNoLot();

    // 2021-07-01 08:00:00 physicalInventory positive
    StockCardLineItem physicalInventoryItem1 = createStockCardLineItem(null, null, orderableNolot, 200,
        day070108, dayOrigin070108, day0701, positiveAdjustment(), null);

    Node destinationNode = new Node();
    destinationNode.setReferenceId(orgDestinationId);
    // 2021-07-01 09:00:00 issue
    StockCardLineItem issueItem1 = createStockCardLineItem(destinationNode, null, orderableNolot, 10,
        day070109, dayOrigin070109, day0701, null, null);

    Node sourceNode = new Node();
    sourceNode.setReferenceId(orgSourceId);
    // 2021-07-01 10:00:00 receive
    StockCardLineItem receiveItem1 = createStockCardLineItem(null, sourceNode, orderableNolot, 30,
        day070110, dayOrigin070110, day0701, null, null);

    // 2021-07-01 11:00:00 physicalInventory inventory
    StockCardLineItem physicalInventoryItem2 = createStockCardLineItem(null, null, orderableNolot, 220,
        day070111, dayOrigin070111, day0701, null, null);

    // 2021-07-01 12:00:00 adjustment positive
    StockCardLineItemReason reason1 = StockCardLineItemReason.builder()
        .name(adjustmentName).reasonType(ReasonType.CREDIT).build();
    StockCardLineItem adjustmentItem1 = createStockCardLineItem(null, null, orderableNolot, 50,
        day070112, dayOrigin070112, day0701, null, reason1);

    // 2021-07-02 08:00:00 adjustment negative
    StockCardLineItemReason reason2 = StockCardLineItemReason.builder()
        .name(adjustmentName).reasonType(ReasonType.DEBIT).build();
    StockCardLineItem adjustmentItem3 = createStockCardLineItem(null, null, orderableNolot, 10,
        day070208, dayOrigin070208, day0702, null, reason2);

    // 2021-07-02 09:00:00 physicalInventory negative
    StockCardLineItem physicalInventoryItem3 = createStockCardLineItem(null, null, orderableNolot, 180,
        day070209, dayOrigin070209, day0702, negativeAdjustment(), null);

    // 2021-07-02 10:00:00 Unpack Kit
    StockCardLineItemReason reason3 = StockCardLineItemReason.builder()
        .name("Unpack Kit").reasonType(ReasonType.CREDIT).build();
    StockCardLineItem uppackKit = createStockCardLineItem(null, null, orderableNolot, 10,
        day070210, dayOrigin070210, day0702, null, reason3);

    List<StockCardLineItem> stockCardLineItemList = Arrays
        .asList(physicalInventoryItem1, issueItem1, receiveItem1, physicalInventoryItem2,
            adjustmentItem1, adjustmentItem3, physicalInventoryItem3,
            uppackKit);

    // 2021-07-01
    CalculatedStockOnHand sohOrderableNoLotDay1 = createCalculatedStockOnHand(orderableNolot, day0701, 270);

    // 2021-07-02
    CalculatedStockOnHand sohOrderableNoLot1Day2 = createCalculatedStockOnHand(orderableNolot, day0702, 190);

    List<CalculatedStockOnHand> stockOnHandList = Arrays
        .asList(sohOrderableNoLotDay1, sohOrderableNoLot1Day2);

    when(stockCardLineItemRepository
        .findByFacilityIdAndIdStartTimeEndTime(any(UUID.class), any(String.class), any(String.class)))
        .thenReturn(stockCardLineItemList);
    when(calculatedStockOnHandRepository
        .findByFacilityIdAndIdStartTimeEndTime(any(UUID.class), any(String.class), any(String.class)))
        .thenReturn(stockOnHandList);
  }

  private List<PhysicalInventoryLineItemAdjustment> positiveAdjustment() {
    List<PhysicalInventoryLineItemAdjustment> positiveAdjustments = new ArrayList<>();
    positiveAdjustments.add(PhysicalInventoryLineItemAdjustment.builder().quantity(20)
        .reason(StockCardLineItemReason.builder().reasonType(ReasonType.CREDIT).build()).build());
    positiveAdjustments.add(PhysicalInventoryLineItemAdjustment.builder().quantity(30)
        .reason(StockCardLineItemReason.builder().reasonType(ReasonType.CREDIT).build()).build());
    return positiveAdjustments;
  }

  private List<PhysicalInventoryLineItemAdjustment> negativeAdjustment() {
    List<PhysicalInventoryLineItemAdjustment> positiveAdjustments = new ArrayList<>();
    positiveAdjustments.add(PhysicalInventoryLineItemAdjustment.builder().quantity(30)
        .reason(StockCardLineItemReason.builder().reasonType(ReasonType.DEBIT).build()).build());
    positiveAdjustments.add(PhysicalInventoryLineItemAdjustment.builder().quantity(50)
        .reason(StockCardLineItemReason.builder().reasonType(ReasonType.DEBIT).build()).build());
    return positiveAdjustments;
  }

  private CalculatedStockOnHand createCalculatedStockOnHand(StockCard stockCard, String occurredDate, Integer soh) {
    return new CalculatedStockOnHandDataBuilder().withStockCard(stockCard).withOccurredDate(getLocalDate(occurredDate))
        .withStockOnHand(soh).build();
  }

  private void initSiglusLotResponse() {
    siglusLotResponseByLotId.put(lotId1FromOrderable1,
        SiglusLotResponse.builder().lotCode(lotId1FromOrderable1Code).expirationDate(getLocalDate(day0701))
            .build());
    String lotId2FromOrderable1Code = "lotId2FromOrderable1Code";
    siglusLotResponseByLotId.put(lotId2FromOrderable1,
        SiglusLotResponse.builder().lotCode(lotId2FromOrderable1Code).expirationDate(getLocalDate(day0701))
            .build());
    siglusLotResponseByLotId.put(lotId1FromOrderable2,
        SiglusLotResponse.builder().lotCode(lotId1FromOrderable2Code).expirationDate(getLocalDate(day0701))
            .build());
    siglusLotResponseByLotId.put(lotId2FromOrderable2,
        SiglusLotResponse.builder().lotCode(lotId2FromOrderable2Code).expirationDate(getLocalDate(day0701))
            .build());
  }

  private StockCardLineItem createStockCardLineItem(Node destination, Node source, StockCard stockcard,
      Integer quantity, String processedDate, String originEventTime, String occurredDate,
      List<PhysicalInventoryLineItemAdjustment> adjustments, StockCardLineItemReason reason) {
    Map<String, String> extraData = new HashMap<>();
    extraData.put("originEventTime", originEventTime);
    return StockCardLineItem.builder()
        .destination(destination)
        .source(source)
        .stockCard(stockcard)
        .processedDate(getProcessedTime(processedDate))
        .occurredDate(getLocalDate(occurredDate))
        .quantity(quantity)
        .reason(reason)
        .stockAdjustments(adjustments)
        .documentNumber(documentNumber)
        .signature(signature)
        .originEvent(event)
        .extraData(extraData)
        .build();
  }

  private StockCard createStockCardToOrderableOneAndLotOne() {
    StockCard stockCard = new StockCard();
    stockCard.setId(stockCardId1);
    stockCard.setOrderableId(orderableId1);
    stockCard.setLotId(lotId1FromOrderable1);
    return stockCard;
  }

  private StockCard createStockCardToOrderableOneAndLotTwo() {
    StockCard stockCard = new StockCard();
    stockCard.setId(stockCardId2);
    stockCard.setOrderableId(orderableId1);
    stockCard.setLotId(lotId2FromOrderable1);
    return stockCard;
  }

  private StockCard createStockCardToOrderableTwoAndLotOne() {
    StockCard stockCard = new StockCard();
    stockCard.setId(stockCardId3);
    stockCard.setOrderableId(orderableId2);
    stockCard.setLotId(lotId1FromOrderable2);
    return stockCard;
  }

  private StockCard createStockCardToOrderableTwoAndLotTwo() {
    StockCard stockCard = new StockCard();
    stockCard.setId(stockCardId4);
    stockCard.setOrderableId(orderableId2);
    stockCard.setLotId(lotId2FromOrderable2);
    return stockCard;
  }

  private StockCard createStockCardToOrderableThreeAndNoLot() {
    StockCard stockCard = new StockCard();
    stockCard.setId(stockCardId5);
    stockCard.setOrderableId(orderableId3);
    return stockCard;
  }

  private List<Organization> createOrganization() {
    Organization organization1 = new Organization();
    organization1.setId(orgSourceId);
    organization1.setName("District(DDM)");

    Organization organization2 = new Organization();
    organization2.setId(orgDestinationId);
    organization2.setName("Farmácia");
    return Arrays.asList(organization1, organization2);
  }

  private ZonedDateTime getProcessedTime(String beijingDateTimeStr) {
    DateTimeFormatter beijingFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        .withZone(ZoneId.of("Asia/Shanghai"));
    if (StringUtils.isBlank(beijingDateTimeStr)) {
      return null;
    }
    ZonedDateTime beijingDateTime = ZonedDateTime.parse(beijingDateTimeStr, beijingFormatter);
    return beijingDateTime.withZoneSameInstant(ZoneId.of("UTC"));
  }

  private LocalDate getLocalDate(String time) {
    return LocalDate.parse(time, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
  }


}
