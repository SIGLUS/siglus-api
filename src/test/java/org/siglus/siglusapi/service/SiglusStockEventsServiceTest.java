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
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newHashSet;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.siglus.common.i18n.MessageKeys.ERROR_LOT_ID_AND_CODE_SHOULD_EMPTY;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
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
import org.openlmis.stockmanagement.domain.card.StockCard;
import org.openlmis.stockmanagement.domain.card.StockCardLineItem;
import org.openlmis.stockmanagement.domain.event.StockEvent;
import org.openlmis.stockmanagement.dto.PhysicalInventoryDto;
import org.openlmis.stockmanagement.dto.StockEventDto;
import org.openlmis.stockmanagement.dto.StockEventLineItemDto;
import org.openlmis.stockmanagement.repository.StockCardLineItemRepository;
import org.openlmis.stockmanagement.repository.StockCardRepository;
import org.openlmis.stockmanagement.repository.StockEventsRepository;
import org.openlmis.stockmanagement.service.StockEventProcessor;
import org.openlmis.stockmanagement.testutils.StockCardDataBuilder;
import org.openlmis.stockmanagement.testutils.StockCardLineItemDataBuilder;
import org.openlmis.stockmanagement.testutils.StockEventLineItemDtoDataBuilder;
import org.siglus.common.domain.StockCardExtension;
import org.siglus.common.dto.referencedata.LotDto;
import org.siglus.common.dto.referencedata.OrderableChildDto;
import org.siglus.common.dto.referencedata.OrderableDto;
import org.siglus.common.dto.referencedata.UserDto;
import org.siglus.common.exception.ValidationMessageException;
import org.siglus.common.repository.StockCardExtensionRepository;
import org.siglus.common.util.SiglusDateHelper;
import org.siglus.siglusapi.constant.FieldConstants;
import org.siglus.siglusapi.service.client.SiglusLotReferenceDataService;
import org.siglus.siglusapi.service.client.SiglusOrderableReferenceDataService;
import org.siglus.siglusapi.service.client.StockEventsStockManagementService;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("PMD.UnusedPrivateField")
public class SiglusStockEventsServiceTest {

  @Captor
  private ArgumentCaptor<StockCardExtension> stockCardExtensionArgumentCaptor;

  @Rule
  public ExpectedException exception = ExpectedException.none();

  @InjectMocks
  private SiglusStockEventsService siglusStockEventsService;

  @Mock
  private StockEventsStockManagementService stockEventsStockManagementService;

  @Mock
  private SiglusOrderableReferenceDataService orderableReferenceDataService;

  @Mock
  private SiglusLotReferenceDataService lotReferenceDataService;

  @Mock
  private SiglusPhysicalInventoryService siglusPhysicalInventoryService;

  @Mock
  private SiglusStockManagementDraftService stockManagementDraftService;

  @Mock
  private StockCardRepository stockCardRepository;

  @Mock
  private StockCardExtensionRepository stockCardExtensionRepository;

  @Mock
  private StockCardLineItemRepository stockCardLineItemRepository;

  @Mock
  private StockEventsRepository stockEventsRepository;

  @Mock
  private StockEventProcessor stockEventProcessor;

  @Mock
  private SiglusArchiveProductService archiveProductService;

  @Mock
  private SiglusDateHelper dateHelper;

  private final UUID orderableId = UUID.randomUUID();

  private final UUID tradeItemId = UUID.randomUUID();

  private final UUID lotId = UUID.randomUUID();

  private static final LocalDate CURRENT_DATE = LocalDate.now();

  private UserDto userDto;

  @Before
  public void prepare() {
    userDto = new UserDto();
    userDto.setId(UUID.randomUUID());
    userDto.setHomeFacilityId(UUID.randomUUID());
  }

  @Test
  public void shouldCallStockEventProcessorWhenCreateStockEventForPhysicalInventory() {
    StockEventLineItemDto lineItemDto1 = new StockEventLineItemDtoDataBuilder()
        .buildForPhysicalInventory();
    StockEventLineItemDto lineItemDto2 = new StockEventLineItemDtoDataBuilder()
        .buildForPhysicalInventory();
    StockEventDto eventDto = StockEventDto.builder()
        .lineItems(newArrayList(lineItemDto1, lineItemDto2)).build();
    PhysicalInventoryDto physicalInventoryDto = PhysicalInventoryDto.builder().build();
    when(siglusPhysicalInventoryService.getPhysicalInventoryDtos(any(), any(), any()))
        .thenReturn(newArrayList(physicalInventoryDto));
    ReflectionTestUtils.setField(siglusStockEventsService, "unpackReasonId", UUID.randomUUID());

    siglusStockEventsService.createStockEventForAllProducts(eventDto);

    verify(stockEventProcessor, times(2)).process(any());
  }

  @Test
  public void shouldCallV3WhenCreateStockEventForAdjustment() {
    StockEventLineItemDto lineItemDto1 = new StockEventLineItemDtoDataBuilder()
        .buildForAdjustment();
    StockEventLineItemDto lineItemDto2 = new StockEventLineItemDtoDataBuilder()
        .buildForAdjustment();
    StockEventDto eventDto = StockEventDto.builder()
        .lineItems(newArrayList(lineItemDto1, lineItemDto2)).build();
    ReflectionTestUtils.setField(siglusStockEventsService, "unpackReasonId", UUID.randomUUID());

    siglusStockEventsService.createStockEventForAllProducts(eventDto);

    verify(stockEventProcessor, times(2)).process(any());
  }

  @Test
  public void shouldAddStockCardCreateTimeWhenStockCardExtensionIsNull() {
    StockEventLineItemDto lineItemDto = new StockEventLineItemDtoDataBuilder()
        .buildForAdjustment();
    StockEventDto eventDto = StockEventDto.builder()
        .lineItems(newArrayList(lineItemDto)).build();
    ReflectionTestUtils.setField(siglusStockEventsService, "unpackReasonId", UUID.randomUUID());
    StockCardLineItem stockCardLineItem = new StockCardLineItemDataBuilder().build();
    StockCard stockCard = new StockCardDataBuilder(new StockEvent()).withLineItem(stockCardLineItem)
        .build();
    when(stockCardRepository.findByProgramIdAndFacilityId(any(), any()))
        .thenReturn(newArrayList(stockCard));

    siglusStockEventsService.createStockEventForAllProducts(eventDto);

    verify(stockCardExtensionRepository).save(stockCardExtensionArgumentCaptor.capture());
    StockCardExtension stockCardExtension = stockCardExtensionArgumentCaptor.getValue();
    assertEquals(stockCard.getId(), stockCardExtension.getStockCardId());
    assertEquals(stockCardLineItem.getOccurredDate(), stockCardExtension.getCreateDate());
  }

  @Test
  public void shouldCreateAndFillLotIdWhenLotIdIsNull() {
    OrderableDto orderableDto = new OrderableDto();
    orderableDto.setId(orderableId);
    orderableDto.setChildren(newHashSet());
    Map<String, String> identifiers = newHashMap();
    identifiers.put(FieldConstants.TRADE_ITEM, tradeItemId.toString());
    orderableDto.setIdentifiers(identifiers);
    when(orderableReferenceDataService.findByIds(any())).thenReturn(newArrayList(orderableDto));
    LotDto lotDto = new LotDto();
    lotDto.setId(lotId);
    when(lotReferenceDataService.saveLot(any())).thenReturn(lotDto);
    StockEventLineItemDto lineItemDto = new StockEventLineItemDtoDataBuilder()
        .withOrderableId(orderableId).build();
    lineItemDto.setLotId(null);
    Map<String, String> extraData = newHashMap();
    extraData.put(FieldConstants.LOT_CODE, "lotCode");
    extraData.put(FieldConstants.EXPIRATION_DATE, "2020-06-16");
    lineItemDto.setExtraData(extraData);
    StockEventDto eventDto = StockEventDto.builder().lineItems(newArrayList(lineItemDto)).build();
    when(dateHelper.getCurrentDate()).thenReturn(CURRENT_DATE);

    siglusStockEventsService.createAndFillLotId(eventDto, false, userDto);

    assertEquals(lotId, lineItemDto.getLotId());
  }

  @Test
  public void shouldReturnExistedLotAndFillLotIdWhenLotIdIsNullAndLotExisted() {
    OrderableDto orderableDto = new OrderableDto();
    orderableDto.setId(orderableId);
    orderableDto.setChildren(newHashSet());
    Map<String, String> identifiers = newHashMap();
    identifiers.put(FieldConstants.TRADE_ITEM, tradeItemId.toString());
    orderableDto.setIdentifiers(identifiers);
    when(orderableReferenceDataService.findByIds(any())).thenReturn(newArrayList(orderableDto));
    LotDto lotDto = new LotDto();
    lotDto.setId(lotId);
    when(lotReferenceDataService.getLots(any())).thenReturn(newArrayList(lotDto));
    StockEventLineItemDto lineItemDto = new StockEventLineItemDtoDataBuilder()
        .withOrderableId(orderableId).build();
    lineItemDto.setLotId(null);
    Map<String, String> extraData = newHashMap();
    extraData.put(FieldConstants.LOT_CODE, "lotCode");
    extraData.put(FieldConstants.EXPIRATION_DATE, "2020-06-16");
    lineItemDto.setExtraData(extraData);
    StockEventDto eventDto = StockEventDto.builder().lineItems(newArrayList(lineItemDto)).build();

    siglusStockEventsService.createAndFillLotId(eventDto, false, userDto);

    assertEquals(lotId, lineItemDto.getLotId());
  }

  @Test
  public void shouldThrowExceptionWhenKitOrderableContainLotInfo() {
    exception.expect(ValidationMessageException.class);
    exception.expectMessage(containsString(ERROR_LOT_ID_AND_CODE_SHOULD_EMPTY));

    OrderableDto orderableDto = new OrderableDto();
    orderableDto.setId(orderableId);
    orderableDto.setChildren(newHashSet(new OrderableChildDto()));
    when(orderableReferenceDataService.findByIds(any())).thenReturn(newArrayList(orderableDto));
    StockEventLineItemDto lineItemDto = new StockEventLineItemDtoDataBuilder()
        .withOrderableId(orderableId).build();
    StockEventDto eventDto = StockEventDto.builder().lineItems(newArrayList(lineItemDto)).build();

    siglusStockEventsService.createAndFillLotId(eventDto, false, userDto);
  }
}
