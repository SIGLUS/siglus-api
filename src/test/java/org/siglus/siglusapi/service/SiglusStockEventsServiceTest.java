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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.siglus.siglusapi.constant.ProgramConstants.ALL_PRODUCTS_PROGRAM_ID;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_ADJUSTMENT_LOCATION_IS_RESTRICTED;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_MOVEMENT_QUANTITY_MORE_THAN_STOCK_ON_HAND;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_STOCK_MANAGEMENT_DRAFT_IS_SUBMITTED;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_STOCK_MANAGEMENT_SUB_DRAFTS_QUANTITY_NOT_MATCH;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_STOCK_MANAGEMENT_SUB_DRAFT_EMPTY;

import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
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
import org.openlmis.referencedata.dto.OrderableDto;
import org.openlmis.stockmanagement.domain.card.StockCard;
import org.openlmis.stockmanagement.domain.card.StockCardLineItem;
import org.openlmis.stockmanagement.domain.event.StockEvent;
import org.openlmis.stockmanagement.domain.reason.ReasonType;
import org.openlmis.stockmanagement.domain.reason.StockCardLineItemReason;
import org.openlmis.stockmanagement.dto.StockEventDto;
import org.openlmis.stockmanagement.dto.StockEventLineItemDto;
import org.openlmis.stockmanagement.repository.StockCardLineItemReasonRepository;
import org.openlmis.stockmanagement.repository.StockCardLineItemRepository;
import org.openlmis.stockmanagement.repository.StockCardRepository;
import org.openlmis.stockmanagement.repository.StockEventsRepository;
import org.openlmis.stockmanagement.service.StockEventProcessor;
import org.siglus.siglusapi.constant.FieldConstants;
import org.siglus.siglusapi.domain.FacilityLocations;
import org.siglus.siglusapi.domain.StockCardExtension;
import org.siglus.siglusapi.domain.StockManagementDraft;
import org.siglus.siglusapi.dto.LotDto;
import org.siglus.siglusapi.dto.StockEventForMultiUserDto;
import org.siglus.siglusapi.dto.StockManagementDraftDto;
import org.siglus.siglusapi.dto.UserDto;
import org.siglus.siglusapi.exception.BusinessDataException;
import org.siglus.siglusapi.exception.ValidationMessageException;
import org.siglus.siglusapi.repository.CalculatedStockOnHandByLocationRepository;
import org.siglus.siglusapi.repository.FacilityLocationsRepository;
import org.siglus.siglusapi.repository.StockCardExtensionRepository;
import org.siglus.siglusapi.repository.StockCardLineItemExtensionRepository;
import org.siglus.siglusapi.repository.StockManagementDraftRepository;
import org.siglus.siglusapi.service.client.SiglusLotReferenceDataService;
import org.siglus.siglusapi.service.client.SiglusOrderableReferenceDataService;
import org.siglus.siglusapi.service.client.StockEventsStockManagementService;
import org.siglus.siglusapi.testutils.StockCardDataBuilder;
import org.siglus.siglusapi.testutils.StockCardLineItemDataBuilder;
import org.siglus.siglusapi.testutils.StockEventLineItemDtoDataBuilder;
import org.siglus.siglusapi.util.SiglusAuthenticationHelper;
import org.siglus.siglusapi.validator.ActiveDraftValidator;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings({"PMD.UnusedPrivateField", "PMD.TooManyMethods", "PMD.AvoidDuplicateLiterals"})
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
  private SiglusAuthenticationHelper authenticationHelper;

  @Mock
  private StockEventProcessor stockEventProcessor;

  @Mock
  private StockManagementDraftRepository stockManagementDraftRepository;

  @Mock
  private ActiveDraftValidator draftValidator;

  @Mock
  private SiglusArchiveProductService archiveProductService;

  @Mock
  private StockCardLineItemExtensionRepository stockCardLineItemExtensionRepository;

  @Mock
  private StockCardLineItemRepository stockCardLineItemRepository;

  @Mock
  private StockEventsRepository stockEventsRepository;

  @Mock
  private SiglusLotService siglusLotService;

  @Mock
  private FacilityLocationsRepository facilityLocationsRepository;

  @Mock
  private StockCardLineItemReasonRepository stockCardLineItemReasonRepository;

  @Mock
  private CalculatedStockOnHandByLocationRepository calculatedStockOnHandByLocationRepository;

  private final UUID orderableId1 = UUID.randomUUID();

  private final UUID tradeItemId1 = UUID.randomUUID();

  private final UUID orderableId2 = UUID.randomUUID();

  private final UUID tradeItemId2 = UUID.randomUUID();

  private final UUID lotId = UUID.randomUUID();

  private final UUID subDraftId = UUID.randomUUID();

  private final UUID initialDraftId = UUID.randomUUID();

  private final String locationCode = "33A33";

  private final String area = "Frios";

  private final UUID positiveReasonId = UUID.randomUUID();

  private final UUID negativeReasonId = UUID.randomUUID();

  private final UUID facilityId = UUID.randomUUID();

  private final UUID stockCardId = UUID.randomUUID();

  private final StockEventLineItemDto lineItemDto1 = new StockEventLineItemDtoDataBuilder().buildForPhysicalInventory();
  private final StockEventLineItemDto lineItemDto2 = new StockEventLineItemDtoDataBuilder().buildForPhysicalInventory();

  private final UUID unpackReasonId = UUID.randomUUID();
  private final UUID unpackDestinationNodeId = UUID.randomUUID();

  private final StockEventForMultiUserDto stockEventForMultiUserDto = new StockEventForMultiUserDto();

  @Before
  public void prepare() {
    UserDto userDto = new UserDto();
    userDto.setId(UUID.randomUUID());
    userDto.setHomeFacilityId(facilityId);
    when(authenticationHelper.getCurrentUser()).thenReturn(userDto);

    OrderableDto orderableDto = createOrderable(orderableId1, tradeItemId1);
    OrderableDto orderableDto2 = createOrderable(orderableId2, tradeItemId2);
    when(orderableReferenceDataService.findByIds(any())).thenReturn(newArrayList(orderableDto, orderableDto2));
    LotDto lotDto = new LotDto();
    lotDto.setId(lotId);
    when(lotReferenceDataService.saveLot(any())).thenReturn(lotDto);
    when(stockEventProcessor.process(any())).thenReturn(UUID.randomUUID());
    lineItemDto1.setOrderableId(orderableId1);
    lineItemDto1.setExtraData(getExtraData());
    lineItemDto2.setOrderableId(orderableId2);
    lineItemDto2.setExtraData(getExtraData());
    ReflectionTestUtils.setField(siglusStockEventsService, "unpackDestinationNodeId", unpackDestinationNodeId);
    doNothing().when(siglusLotService).createAndFillLotId(any());
    StockCardLineItemReason positiveReason = StockCardLineItemReason
        .builder()
        .reasonType(ReasonType.CREDIT)
        .build();
    positiveReason.setId(positiveReasonId);
    StockCardLineItemReason negativeReason = StockCardLineItemReason
        .builder()
        .reasonType(ReasonType.DEBIT)
        .build();
    negativeReason.setId(negativeReasonId);
    when(stockCardLineItemReasonRepository.findAll()).thenReturn(Arrays.asList(positiveReason, negativeReason));
  }

  @Test
  public void shouldCallV3WhenCreateStockEventForAdjustment() {
    // given
    StockEventLineItemDto lineItemDto1 = new StockEventLineItemDtoDataBuilder()
        .buildForAdjustment();
    lineItemDto1.setOrderableId(orderableId1);
    StockEventLineItemDto lineItemDto2 = new StockEventLineItemDtoDataBuilder()
        .buildForAdjustment();
    lineItemDto2.setOrderableId(orderableId2);
    StockEventDto eventDto = StockEventDto.builder()
        .lineItems(newArrayList(lineItemDto1, lineItemDto2))
        .programId(ALL_PRODUCTS_PROGRAM_ID).build();
    when(stockManagementDraftService.findStockManagementDraft(any(), any(), any())).thenReturn(
        Collections.singletonList(new StockManagementDraftDto()));

    // when
    siglusStockEventsService.processStockEvent(eventDto, false);

    // then
    verify(stockEventProcessor, times(2)).process(any());
  }

  @Test
  public void shouldAddStockCardCreateTimeWhenStockCardExtensionIsNull() {
    // given
    StockEventLineItemDto lineItemDto = new StockEventLineItemDtoDataBuilder().buildForAdjustment();
    lineItemDto.setOrderableId(orderableId1);
    StockEventDto eventDto = StockEventDto.builder()
        .lineItems(newArrayList(lineItemDto))
        .programId(ALL_PRODUCTS_PROGRAM_ID).build();
    StockCardLineItem stockCardLineItem = new StockCardLineItemDataBuilder().build();
    StockCard stockCard = new StockCardDataBuilder(new StockEvent()).withLineItem(stockCardLineItem)
        .build();
    when(stockCardRepository.findByProgramIdAndFacilityId(any(), any()))
        .thenReturn(newArrayList(stockCard));
    when(stockManagementDraftService.findStockManagementDraft(any(), any(), any())).thenReturn(
        Collections.singletonList(new StockManagementDraftDto()));

    // when
    siglusStockEventsService.processStockEvent(eventDto, false);

    // then
    verify(stockCardExtensionRepository).save(stockCardExtensionArgumentCaptor.capture());
    StockCardExtension stockCardExtension = stockCardExtensionArgumentCaptor.getValue();
    assertEquals(stockCard.getId(), stockCardExtension.getStockCardId());
    assertEquals(stockCardLineItem.getOccurredDate(), stockCardExtension.getCreateDate());
  }

  @Test
  public void shouldThrowExceptionWhenAdjustmentIsSubmitted() {
    // then
    exception.expect(ValidationMessageException.class);
    exception.expectMessage(containsString("stockmanagement.error.physicalInventory.isSubmitted"));

    // given
    StockEventDto eventDto = StockEventDto.builder()
        .lineItems(newArrayList(lineItemDto1, lineItemDto2))
        .programId(ALL_PRODUCTS_PROGRAM_ID).build();
    when(siglusPhysicalInventoryService.getPhysicalInventoryDtosDirectly(any(), any(), any()))
        .thenReturn(Collections.emptyList());

    // when
    siglusStockEventsService.processStockEvent(eventDto, false);
  }

  @Test
  public void shouldThrowExceptionWhenPhysicalInventoryIsSubmitted() {
    // then
    exception.expect(ValidationMessageException.class);
    exception.expectMessage(containsString(ERROR_STOCK_MANAGEMENT_DRAFT_IS_SUBMITTED));

    // given
    StockEventLineItemDto lineItemDto1 = new StockEventLineItemDtoDataBuilder()
        .buildForAdjustment();
    lineItemDto1.setOrderableId(orderableId1);
    StockEventLineItemDto lineItemDto2 = new StockEventLineItemDtoDataBuilder()
        .buildForAdjustment();
    lineItemDto2.setOrderableId(orderableId2);
    StockEventDto eventDto = StockEventDto.builder()
        .lineItems(newArrayList(lineItemDto1, lineItemDto2))
        .programId(ALL_PRODUCTS_PROGRAM_ID).build();
    when(stockManagementDraftService.findStockManagementDraft(any(), any(), any()))
        .thenReturn(Collections.emptyList());

    // when
    siglusStockEventsService.processStockEvent(eventDto, false);
  }

  @Test
  public void shouldThrowExceptionWhenIssueIsSubmitted() {
    // then
    exception.expect(ValidationMessageException.class);
    exception.expectMessage(containsString(ERROR_STOCK_MANAGEMENT_DRAFT_IS_SUBMITTED));

    // given
    StockEventLineItemDto lineItemDtoIssue = new StockEventLineItemDtoDataBuilder().build();
    lineItemDtoIssue.setOrderableId(orderableId1);
    lineItemDtoIssue.setReasonId(null);
    lineItemDtoIssue.setSourceId(null);

    StockEventDto eventDto = StockEventDto.builder().lineItems(newArrayList(lineItemDtoIssue))
        .programId(ALL_PRODUCTS_PROGRAM_ID).build();
    when(stockManagementDraftService.findStockManagementDraft(any(), any(), any()))
        .thenReturn(Collections.emptyList());

    // when
    siglusStockEventsService.processStockEvent(eventDto, false);
  }

  @Test
  public void shouldCallStockEventApiWhenUnpackKitEvent() {
    // given
    StockEventLineItemDto lineItemDtoUnpackKit = new StockEventLineItemDtoDataBuilder().build();
    lineItemDtoUnpackKit.setDestinationId(unpackDestinationNodeId);
    lineItemDtoUnpackKit.setSourceId(null);
    lineItemDtoUnpackKit.setReasonId(unpackReasonId);
    lineItemDtoUnpackKit.setOrderableId(orderableId1);
    lineItemDtoUnpackKit.setExtraData(getExtraData());

    StockEventDto eventDto = StockEventDto.builder().lineItems(newArrayList(lineItemDtoUnpackKit))
        .programId(ALL_PRODUCTS_PROGRAM_ID).build();

    // when
    siglusStockEventsService.processStockEvent(eventDto, false);
    // then
    verify(stockManagementDraftService, times(0)).findStockManagementDraft(any(), any(), any());
  }

  @Test
  public void shouldThrowExceptionWhenSubDraftIdsIsEmpty() {
    exception.expect(BusinessDataException.class);
    exception.expectMessage(containsString(ERROR_STOCK_MANAGEMENT_SUB_DRAFT_EMPTY));

    stockEventForMultiUserDto.setSubDrafts(Collections.emptyList());

    siglusStockEventsService.processStockEventForMultiUser(stockEventForMultiUserDto, false);
  }

  @Test
  public void shouldThrowExceptionWhenSubDraftIdsNotEqualsSubDrafts() {
    exception.expect(BusinessDataException.class);
    exception.expectMessage(containsString(ERROR_STOCK_MANAGEMENT_SUB_DRAFTS_QUANTITY_NOT_MATCH));

    ArrayList<UUID> subDraftIds = newArrayList(subDraftId);
    StockManagementDraft subDraft = StockManagementDraft.builder().initialDraftId(initialDraftId)
        .build();
    stockEventForMultiUserDto.setSubDrafts(subDraftIds);

    doNothing().when(draftValidator).validateSubDraft(subDraft);
    when(stockManagementDraftRepository.findOne(subDraftId)).thenReturn(subDraft);
    when(stockManagementDraftRepository.countByInitialDraftId(initialDraftId)).thenReturn(2);

    siglusStockEventsService.processStockEventForMultiUser(stockEventForMultiUserDto, false);
  }

  @Test
  public void shouldThrowBusinessExceptionWhenPositiveAdjustmentLocationCodeIsRestricted() {
    //then
    exception.expect(BusinessDataException.class);
    exception.expectMessage(containsString(ERROR_ADJUSTMENT_LOCATION_IS_RESTRICTED));

    // given
    StockEventLineItemDto stockEventLineItemDto = new StockEventLineItemDtoDataBuilder().buildForAdjustment();
    stockEventLineItemDto.setLocationCode(locationCode);
    stockEventLineItemDto.setArea(area);
    stockEventLineItemDto.setReasonId(positiveReasonId);
    StockEventDto eventDto = StockEventDto
        .builder().lineItems(Collections.singletonList(stockEventLineItemDto))
        .programId(ALL_PRODUCTS_PROGRAM_ID).build();
    when(facilityLocationsRepository.findByFacilityId(any())).thenReturn(
        Collections.singletonList(FacilityLocations.builder()
            .locationCode("44A44").area("Armazem Principal").build())
    );
    when(stockManagementDraftService.findStockManagementDraft(any(), any(), any())).thenReturn(
        Collections.singletonList(new StockManagementDraftDto()));

    // when
    siglusStockEventsService.processStockEvent(eventDto, true);
  }


  @Test
  public void shouldThrowBusinessExceptionWhenNegativeAdjustmentQuantityIsOverload() {
    //then
    exception.expect(BusinessDataException.class);
    exception.expectMessage(containsString(ERROR_MOVEMENT_QUANTITY_MORE_THAN_STOCK_ON_HAND));

    // given
    when(stockManagementDraftService.findStockManagementDraft(any(), any(), any())).thenReturn(
        Collections.singletonList(new StockManagementDraftDto()));
    StockCard stockCard = StockCard.builder().build();
    stockCard.setId(stockCardId);
    when(stockCardRepository.findByProgramIdAndFacilityIdAndOrderableIdAndLotId(
        ALL_PRODUCTS_PROGRAM_ID,
        facilityId,
        orderableId1,
        lotId)).thenReturn(stockCard);
    when(calculatedStockOnHandByLocationRepository
        .findRecentlySohByStockCardIdAndLocationCode(
            stockCardId,
            locationCode)).thenReturn(Optional.of(20));
    StockEventLineItemDto stockEventLineItemDto = new StockEventLineItemDtoDataBuilder().buildForAdjustment();
    stockEventLineItemDto.setLocationCode(locationCode);
    stockEventLineItemDto.setArea(area);
    stockEventLineItemDto.setReasonId(negativeReasonId);
    stockEventLineItemDto.setOrderableId(orderableId1);
    stockEventLineItemDto.setLotId(lotId);
    stockEventLineItemDto.setQuantity(30);
    StockEventDto eventDto = StockEventDto
        .builder().lineItems(Collections.singletonList(stockEventLineItemDto))
        .programId(ALL_PRODUCTS_PROGRAM_ID).build();

    // when
    siglusStockEventsService.processStockEvent(eventDto, true);
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

  private ImmutableMap<String, String> getExtraData() {
    return ImmutableMap
        .of(FieldConstants.LOT_CODE, "lotCode", FieldConstants.EXPIRATION_DATE, "2020-06-16");
  }



}
