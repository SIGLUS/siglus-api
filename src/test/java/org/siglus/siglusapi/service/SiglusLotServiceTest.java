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
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_LOT_ID_AND_CODE_SHOULD_EMPTY;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.referencedata.domain.Lot;
import org.openlmis.referencedata.domain.TradeItem;
import org.openlmis.referencedata.dto.OrderableChildDto;
import org.openlmis.referencedata.dto.OrderableDto;
import org.openlmis.referencedata.repository.LotRepository;
import org.openlmis.stockmanagement.domain.reason.StockCardLineItemReason;
import org.openlmis.stockmanagement.dto.StockEventDto;
import org.openlmis.stockmanagement.dto.StockEventLineItemDto;
import org.openlmis.stockmanagement.repository.StockCardLineItemReasonRepository;
import org.siglus.siglusapi.constant.FieldConstants;
import org.siglus.siglusapi.dto.LotDto;
import org.siglus.siglusapi.dto.RemovedLotDto;
import org.siglus.siglusapi.dto.UserDto;
import org.siglus.siglusapi.exception.BusinessDataException;
import org.siglus.siglusapi.exception.ValidationMessageException;
import org.siglus.siglusapi.repository.SiglusLotRepository;
import org.siglus.siglusapi.repository.dto.LotStockDto;
import org.siglus.siglusapi.repository.dto.StockCardStockDto;
import org.siglus.siglusapi.service.client.SiglusLotReferenceDataService;
import org.siglus.siglusapi.service.client.SiglusOrderableReferenceDataService;
import org.siglus.siglusapi.testutils.StockEventLineItemDtoDataBuilder;
import org.siglus.siglusapi.util.FacilityConfigHelper;
import org.siglus.siglusapi.util.SiglusAuthenticationHelper;
import org.siglus.siglusapi.util.SiglusDateHelper;

@RunWith(MockitoJUnitRunner.class)
public class SiglusLotServiceTest {

  @InjectMocks
  private SiglusLotService siglusLotService;

  @Mock
  private SiglusOrderableReferenceDataService orderableReferenceDataService;

  @Mock
  private SiglusAuthenticationHelper authenticationHelper;

  @Mock
  private SiglusDateHelper dateHelper;

  @Mock
  private SiglusLotReferenceDataService lotReferenceDataService;

  @Mock
  private LotRepository lotRepository;

  @Mock
  private SiglusLotRepository siglusLotRepository;

  @Mock
  private SiglusStockCardSummariesService siglusStockCardSummariesService;

  @Mock
  private SiglusStockEventsService siglusStockEventsService;

  @Mock
  private StockCardLineItemReasonRepository stockCardLineItemReasonRepository;

  @Mock
  private FacilityConfigHelper facilityConfigHelper;

  @Mock
  private SiglusArchiveProductService siglusArchiveProductService;
  @Mock
  private SiglusStockCardService siglusStockCardService;

  @Rule
  public ExpectedException exception = ExpectedException.none();

  private final UUID orderableId1 = UUID.randomUUID();

  private final UUID tradeItemId1 = UUID.randomUUID();

  private final UUID orderableId2 = UUID.randomUUID();

  private final UUID tradeItemId2 = UUID.randomUUID();

  private final UUID lotId = UUID.randomUUID();

  private static final LocalDate CURRENT_DATE = LocalDate.now();

  @Before
  public void prepare() {
    UserDto userDto = new UserDto();
    userDto.setId(UUID.randomUUID());
    userDto.setHomeFacilityId(UUID.randomUUID());
    when(authenticationHelper.getCurrentUser()).thenReturn(userDto);
    OrderableDto orderableDto = createOrderable(orderableId1, tradeItemId1);
    OrderableDto orderableDto2 = createOrderable(orderableId2, tradeItemId2);
    when(orderableReferenceDataService.findByIds(any())).thenReturn(newArrayList(orderableDto, orderableDto2));
    LotDto lotDto = new LotDto();
    lotDto.setId(lotId);
    when(lotReferenceDataService.saveLot(any())).thenReturn(lotDto);
  }

  @Test
  public void shouldCreateAndFillLotIdWhenLotIdIsNull() {
    // given
    OrderableDto orderableDto = createOrderable(orderableId1, tradeItemId1);
    when(orderableReferenceDataService.findByIds(any())).thenReturn(newArrayList(orderableDto));
    LotDto lotDto = new LotDto();
    lotDto.setId(lotId);
    when(lotReferenceDataService.saveLot(any())).thenReturn(lotDto);
    StockEventLineItemDto lineItemDto = new StockEventLineItemDtoDataBuilder().withOrderableId(orderableId1).build();
    lineItemDto.setLotId(null);
    Map<String, String> extraData = newHashMap();
    extraData.put(FieldConstants.LOT_CODE, "lotCode");
    extraData.put(FieldConstants.EXPIRATION_DATE, "2020-06-16");
    lineItemDto.setExtraData(extraData);
    StockEventDto eventDto = StockEventDto.builder().lineItems(newArrayList(lineItemDto)).build();
    when(dateHelper.getCurrentDate()).thenReturn(CURRENT_DATE);

    // when
    siglusLotService.createAndFillLotId(eventDto);

    // then
    assertEquals(lotId, lineItemDto.getLotId());
  }

  @Test
  public void shouldReturnExistedLotAndFillLotIdWhenLotIdIsNullAndLotExisted() {
    // given
    OrderableDto orderableDto = new OrderableDto();
    orderableDto.setId(orderableId1);
    orderableDto.setChildren(newHashSet());
    Map<String, String> identifiers = newHashMap();
    identifiers.put(FieldConstants.TRADE_ITEM, tradeItemId1.toString());
    orderableDto.setIdentifiers(identifiers);
    StockEventLineItemDto lineItemDto = new StockEventLineItemDtoDataBuilder().withOrderableId(orderableId1).build();
    lineItemDto.setLotId(null);
    Map<String, String> extraData = newHashMap();
    extraData.put(FieldConstants.LOT_CODE, "lotCode");
    extraData.put(FieldConstants.EXPIRATION_DATE, "2020-06-16");
    lineItemDto.setExtraData(extraData);
    StockEventDto eventDto = StockEventDto.builder().lineItems(newArrayList(lineItemDto)).build();

    // when
    siglusLotService.createAndFillLotId(eventDto);

    // then
    assertEquals(lotId, lineItemDto.getLotId());
  }

  @Test
  public void shouldThrowExceptionWhenKitOrderableContainLotInfo() {
    // then
    exception.expect(ValidationMessageException.class);
    exception.expectMessage(containsString(ERROR_LOT_ID_AND_CODE_SHOULD_EMPTY));

    // given
    OrderableDto orderableDto = new OrderableDto();
    orderableDto.setId(orderableId1);
    orderableDto.setChildren(newHashSet(new OrderableChildDto()));
    when(orderableReferenceDataService.findByIds(any())).thenReturn(newArrayList(orderableDto));
    StockEventLineItemDto lineItemDto = new StockEventLineItemDtoDataBuilder().build();
    lineItemDto.setOrderableId(orderableId1);
    StockEventDto eventDto = StockEventDto.builder().lineItems(newArrayList(lineItemDto)).build();

    // when
    siglusLotService.createAndFillLotId(eventDto);
  }

  @Test
  public void shouldReturnExistedLotAndFillLotIdWhenFillLotId() {
    // given
    OrderableDto orderableDto = new OrderableDto();
    orderableDto.setId(orderableId1);
    orderableDto.setChildren(newHashSet());
    Map<String, String> identifiers = newHashMap();
    identifiers.put(FieldConstants.TRADE_ITEM, tradeItemId1.toString());
    orderableDto.setIdentifiers(identifiers);
    StockEventLineItemDto lineItemDto = new StockEventLineItemDtoDataBuilder().withOrderableId(orderableId1).build();
    lineItemDto.setLotId(null);
    Map<String, String> extraData = newHashMap();
    extraData.put(FieldConstants.LOT_CODE, "lotCode");
    extraData.put(FieldConstants.EXPIRATION_DATE, "2020-06-16");
    lineItemDto.setExtraData(extraData);
    StockEventDto eventDto = StockEventDto.builder().lineItems(newArrayList(lineItemDto)).build();

    // when
    siglusLotService.createAndFillLotId(eventDto);

    // then
    assertEquals(lotId, lineItemDto.getLotId());
  }

  @Test
  public void shouldReturnLotListWhenGet() {
    // given
    ArrayList<UUID> ids = newArrayList(UUID.randomUUID());
    org.openlmis.referencedata.domain.Lot lot = new Lot();
    lot.setTradeItem(new TradeItem());
    when(lotRepository.findAll(ids)).thenReturn(newArrayList(lot));
    // when
    List<LotDto> lotList = siglusLotService.getLotList(ids);
    // then
    assertEquals(1, lotList.size());
  }

  @Test
  public void shouldReturnLotListWhenGetByOrderable() {
    // given
    UUID facilityId = UUID.randomUUID();
    UUID orderableId = UUID.randomUUID();
    ArrayList<UUID> ids = newArrayList(UUID.randomUUID());
    when(siglusStockCardService.findLotIdsByFacilityAndOrderable(facilityId, orderableId)).thenReturn(ids);
    org.openlmis.referencedata.domain.Lot lot = new Lot();
    lot.setTradeItem(new TradeItem());
    when(lotRepository.findAll(ids)).thenReturn(newArrayList(lot));
    // when
    List<LotDto> lotList = siglusLotService.getLotsByOrderable(facilityId, orderableId);
    // then
    assertEquals(1, lotList.size());
  }

  @Test
  public void shouldCallQueryExpiredLotsWithLocationGivenFacilityIsLocationEnabled() {
    UUID facilityId = UUID.randomUUID();
    when(facilityConfigHelper.isLocationManagementEnabled(facilityId)).thenReturn(true);
    when(siglusArchiveProductService.searchArchivedProductsByFacilityId(facilityId)).thenReturn(newHashSet());

    siglusLotService.getExpiredLots(facilityId);

    verify(siglusLotRepository, Mockito.times(1)).queryExpiredLotsWithLocation(facilityId);
  }

  @Test
  public void shouldCallQueryExpiredLotsGivenFacilityIsNotLocationEnabled() {
    UUID facilityId = UUID.randomUUID();
    when(facilityConfigHelper.isLocationManagementEnabled(facilityId)).thenReturn(false);
    when(siglusArchiveProductService.searchArchivedProductsByFacilityId(facilityId)).thenReturn(newHashSet());

    siglusLotService.getExpiredLots(facilityId);

    verify(siglusLotRepository, Mockito.times(1)).queryExpiredLots(facilityId);
  }

  @Test
  public void shouldNotCallArchiveProductServiceGivenNoExpiredLotExist() {
    UUID facilityId = UUID.randomUUID();
    when(facilityConfigHelper.isLocationManagementEnabled(facilityId)).thenReturn(false);
    when(siglusLotRepository.queryExpiredLots(facilityId)).thenReturn(newArrayList());

    siglusLotService.getExpiredLots(facilityId);

    verify(siglusArchiveProductService, Mockito.times(0)).searchArchivedProductsByFacilityId(facilityId);
  }

  @Test
  public void shouldRemoveArchivedProductsWhenGetExpiredLots() {
    UUID facilityId = UUID.randomUUID();
    LotStockDto dto1 = LotStockDto.builder()
        .orderableId(UUID.randomUUID())
        .build();
    LotStockDto dto2 = LotStockDto.builder()
        .orderableId(UUID.randomUUID())
        .build();
    when(facilityConfigHelper.isLocationManagementEnabled(facilityId)).thenReturn(false);
    when(siglusLotRepository.queryExpiredLots(facilityId)).thenReturn(newArrayList(dto1, dto2));
    when(siglusArchiveProductService.searchArchivedProductsByFacilityId(facilityId))
        .thenReturn(newHashSet(dto2.getOrderableId().toString()));

    List<LotStockDto> result = siglusLotService.getExpiredLots(facilityId);

    assertEquals(result.size(), 1);
    assertEquals(result.get(0).getOrderableId(), dto1.getOrderableId());
  }

  @Test(expected = BusinessDataException.class)
  public void shouldThrowExceptionWhenRemoveExpiredLotsGivenLotIsNotExpired() {
    when(siglusLotRepository.existsNotExpiredLotsByIds(anyList())).thenReturn(true);

    siglusLotService.removeExpiredLots(new ArrayList<>(), false);
  }

  @Test(expected = BusinessDataException.class)
  public void shouldThrowExceptionWhenRemoveExpiredLotsGivenLotQuantityBiggerThanSoh() {
    RemovedLotDto lot = buildRemovedLotDto(UUID.randomUUID(), UUID.randomUUID(), 10);
    List<RemovedLotDto> lots = new ArrayList<>();
    lots.add(lot);
    List<StockCardStockDto> stockCardStockDtos = new ArrayList<>();
    StockCardStockDto stockDto = StockCardStockDto.builder()
        .stockOnHand(5)
        .stockCardId(lot.getStockCardId())
        .build();
    stockCardStockDtos.add(stockDto);
    when(siglusLotRepository.existsNotExpiredLotsByIds(anyList())).thenReturn(false);
    when(siglusStockCardSummariesService.getLatestStockOnHandByIds(anyList(), anyBoolean()))
        .thenReturn(stockCardStockDtos);

    siglusLotService.removeExpiredLots(lots, false);
  }

  @Test(expected = BusinessDataException.class)
  public void shouldThrowExceptionWhenRemoveExpiredLotsWhenCanNotGetExpiredDiscardReason() {
    RemovedLotDto lot = buildRemovedLotDto(UUID.randomUUID(), UUID.randomUUID(), 10);
    List<RemovedLotDto> lots = new ArrayList<>();
    lots.add(lot);
    List<StockCardStockDto> stockCardStockDtos = new ArrayList<>();
    StockCardStockDto stockDto = StockCardStockDto.builder()
        .stockOnHand(10)
        .stockCardId(lot.getStockCardId())
        .build();
    stockCardStockDtos.add(stockDto);
    when(siglusLotRepository.existsNotExpiredLotsByIds(anyList())).thenReturn(false);
    when(siglusStockCardSummariesService.getLatestStockOnHandByIds(anyList(), anyBoolean()))
        .thenReturn(stockCardStockDtos);
    when(stockCardLineItemReasonRepository.findByName(anyString())).thenReturn(null);

    siglusLotService.removeExpiredLots(lots, false);
  }

  @Test
  public void shouldSendStockEventWhenRemoveExpiredLots() {
    RemovedLotDto lot = buildRemovedLotDto(UUID.randomUUID(), UUID.randomUUID(), 10);
    List<RemovedLotDto> lots = new ArrayList<>();
    lots.add(lot);
    List<StockCardStockDto> stockCardStockDtos = new ArrayList<>();
    StockCardStockDto stockDto = StockCardStockDto.builder()
        .stockOnHand(10)
        .stockCardId(lot.getStockCardId())
        .build();
    stockCardStockDtos.add(stockDto);
    when(siglusLotRepository.existsNotExpiredLotsByIds(anyList())).thenReturn(false);
    when(siglusStockCardSummariesService.getLatestStockOnHandByIds(anyList(), anyBoolean()))
        .thenReturn(stockCardStockDtos);
    StockCardLineItemReason reason = new StockCardLineItemReason();
    reason.setId(UUID.randomUUID());
    when(stockCardLineItemReasonRepository.findByName(anyString())).thenReturn(reason);
    doNothing().when(siglusStockEventsService).processStockEvent(any(), anyBoolean());

    siglusLotService.removeExpiredLots(lots, false);

    verify(siglusStockEventsService, Mockito.times(1)).processStockEvent(any(), anyBoolean());
  }

  @Test
  public void shouldSendStockEventWhenRemoveExpiredLotsWithLocation() {
    RemovedLotDto lot = buildRemovedLotDto(UUID.randomUUID(), UUID.randomUUID(), 10);
    lot.setLocationCode("code1");
    RemovedLotDto lot2 = buildRemovedLotDto(UUID.randomUUID(), UUID.randomUUID(), 20);
    lot2.setStockCardId(lot.getStockCardId());
    lot2.setLocationCode("code2");
    List<RemovedLotDto> lots = new ArrayList<>();
    lots.add(lot);
    lots.add(lot2);
    List<StockCardStockDto> stockCardStockDtos = new ArrayList<>();
    StockCardStockDto stockDto = StockCardStockDto.builder()
        .stockOnHand(10)
        .stockCardId(lot.getStockCardId())
        .locationCode(lot.getLocationCode())
        .build();
    StockCardStockDto stockDto2 = StockCardStockDto.builder()
        .stockOnHand(20)
        .stockCardId(lot2.getStockCardId())
        .locationCode(lot2.getLocationCode())
        .build();
    stockCardStockDtos.add(stockDto);
    stockCardStockDtos.add(stockDto2);
    when(siglusLotRepository.existsNotExpiredLotsByIds(anyList())).thenReturn(false);
    when(siglusStockCardSummariesService.getLatestStockOnHandByIds(anyList(), anyBoolean()))
        .thenReturn(stockCardStockDtos);
    StockCardLineItemReason reason = new StockCardLineItemReason();
    reason.setId(UUID.randomUUID());
    when(stockCardLineItemReasonRepository.findByName(anyString())).thenReturn(reason);
    doNothing().when(siglusStockEventsService).processStockEvent(any(), anyBoolean());

    siglusLotService.removeExpiredLots(lots, true);

    verify(siglusStockEventsService, Mockito.times(2)).processStockEvent(any(), anyBoolean());
  }

  @Test
  public void shouldGetEmptyLotsGivenFacilityWithoutStockCards() {
    UUID facilityId = UUID.randomUUID();
    List<UUID> orderableIds = newArrayList(UUID.randomUUID(), UUID.randomUUID());
    when(siglusStockCardService.findStockCardIdByFacilityAndOrderables(facilityId, orderableIds))
        .thenReturn(newArrayList());

    List<LotStockDto> lots = siglusLotService.getLotStocksByOrderables(facilityId, orderableIds);

    assertEquals(0, lots.size());
  }

  @Test
  public void shouldGetLotsGivenFacilityWithoutLocation() {
    UUID facilityId = UUID.randomUUID();
    List<UUID> orderableIds = newArrayList(UUID.randomUUID(), UUID.randomUUID());
    List<UUID> stockCardIds = newArrayList(UUID.randomUUID(), UUID.randomUUID());
    when(siglusStockCardService.findStockCardIdByFacilityAndOrderables(facilityId, orderableIds))
        .thenReturn(stockCardIds);
    when(facilityConfigHelper.isLocationManagementEnabled(facilityId)).thenReturn(false);

    siglusLotService.getLotStocksByOrderables(facilityId, orderableIds);

    verify(siglusLotRepository, Mockito.times(1)).queryLotStockDtoByStockCardIds(stockCardIds);
  }

  @Test
  public void shouldGetLotsGivenFacilityWithLocation() {
    UUID facilityId = UUID.randomUUID();
    List<UUID> orderableIds = newArrayList(UUID.randomUUID(), UUID.randomUUID());
    List<UUID> stockCardIds = newArrayList(UUID.randomUUID(), UUID.randomUUID());
    when(siglusStockCardService.findStockCardIdByFacilityAndOrderables(facilityId, orderableIds))
        .thenReturn(stockCardIds);
    when(facilityConfigHelper.isLocationManagementEnabled(facilityId)).thenReturn(true);

    siglusLotService.getLotStocksByOrderables(facilityId, orderableIds);

    verify(siglusLotRepository, Mockito.times(1))
        .queryLotStockDtoByStockCardIdsWithLocation(stockCardIds);
  }

  private OrderableDto createOrderable(UUID orderableId, UUID tradeItemId) {
    OrderableDto orderableDto = new OrderableDto();
    orderableDto.setId(orderableId);
    orderableDto.setChildren(newHashSet());
    Map<String, String> identifiers = newHashMap();
    identifiers.put(FieldConstants.TRADE_ITEM, tradeItemId.toString());
    orderableDto.setIdentifiers(identifiers);
    return orderableDto;
  }

  private RemovedLotDto buildRemovedLotDto(UUID facilityId, UUID programId, int quantity) {
    return RemovedLotDto.builder()
        .stockCardId(UUID.randomUUID())
        .facilityId(facilityId)
        .programId(programId)
        .orderableId(UUID.randomUUID())
        .lotId(UUID.randomUUID())
        .quantity(quantity)
        .build();
  }
}
