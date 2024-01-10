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
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.siglus.siglusapi.constant.LocationConstants.VIRTUAL_LOCATION_CODE;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_MOVEMENT_DRAFT_NOT_FOUND;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_MOVEMENT_QUANTITY_LESS_THAN_STOCK_ON_HAND;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_MOVEMENT_QUANTITY_MORE_THAN_STOCK_ON_HAND;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_STOCK_CARD_NOT_FOUND;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
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
import org.openlmis.stockmanagement.domain.card.StockCard;
import org.openlmis.stockmanagement.dto.StockCardDto;
import org.openlmis.stockmanagement.dto.referencedata.DispensableDto;
import org.openlmis.stockmanagement.dto.referencedata.FacilityDto;
import org.openlmis.stockmanagement.dto.referencedata.LotDto;
import org.openlmis.stockmanagement.dto.referencedata.OrderableDto;
import org.openlmis.stockmanagement.dto.referencedata.ProgramDto;
import org.openlmis.stockmanagement.repository.StockCardRepository;
import org.openlmis.stockmanagement.service.referencedata.FacilityReferenceDataService;
import org.openlmis.stockmanagement.service.referencedata.LotReferenceDataService;
import org.openlmis.stockmanagement.service.referencedata.OrderableReferenceDataService;
import org.openlmis.stockmanagement.service.referencedata.ProgramReferenceDataService;
import org.siglus.siglusapi.domain.CalculatedStockOnHandByLocation;
import org.siglus.siglusapi.domain.StockCardLocationMovementDraft;
import org.siglus.siglusapi.domain.StockCardLocationMovementLineItem;
import org.siglus.siglusapi.dto.FacilitySearchResultDto;
import org.siglus.siglusapi.dto.LocationMovementDto;
import org.siglus.siglusapi.dto.LocationMovementLineItemDto;
import org.siglus.siglusapi.dto.StockCardLocationMovementDto;
import org.siglus.siglusapi.dto.StockCardLocationMovementLineItemDto;
import org.siglus.siglusapi.dto.UserDto;
import org.siglus.siglusapi.exception.BusinessDataException;
import org.siglus.siglusapi.exception.NotFoundException;
import org.siglus.siglusapi.repository.CalculatedStockOnHandByLocationRepository;
import org.siglus.siglusapi.repository.SiglusStockCardRepository;
import org.siglus.siglusapi.repository.StockCardLocationMovementDraftRepository;
import org.siglus.siglusapi.repository.StockCardLocationMovementLineItemRepository;
import org.siglus.siglusapi.util.SiglusAuthenticationHelper;

@SuppressWarnings({"PMD.UnusedPrivateField"})
@RunWith(MockitoJUnitRunner.class)
public class SiglusStockCardLocationMovementServiceTest {

  @Rule
  public ExpectedException exception = ExpectedException.none();

  @InjectMocks
  private SiglusStockCardLocationMovementService service;

  @Mock
  private SiglusStockCardRepository siglusStockCardRepository;

  @Mock
  private StockCardRepository stockCardRepository;

  @Mock
  private StockCardLocationMovementLineItemRepository movementLineItemRepository;

  @Mock
  private StockCardLocationMovementDraftRepository movementDraftRepository;

  @Mock
  private CalculatedStocksOnHandByLocationService calculatedStocksOnHandByLocationService;

  @Mock
  private CalculatedStockOnHandByLocationRepository calculatedStockOnHandByLocationRepository;
  @Mock
  private SiglusAdministrationsService administrationsService;

  @Mock
  private SiglusAuthenticationHelper authenticationHelper;

  @Mock
  private SiglusStockCardService siglusStockCardService;

  @Mock
  private FacilityReferenceDataService facilityReferenceDataService;

  @Mock
  private OrderableReferenceDataService orderableReferenceDataService;

  @Mock
  private ProgramReferenceDataService programReferenceDataService;

  @Mock
  private LotReferenceDataService lotReferenceDataService;

  @Captor
  private ArgumentCaptor<List<UUID>> argumentCaptor;

  private final UUID allProgramId = UUID.randomUUID();
  private final UUID programId = UUID.randomUUID();
  private final UUID facilityId = UUID.randomUUID();
  private final UUID userId = UUID.randomUUID();
  private final UUID orderableId = UUID.randomUUID();
  private final UUID lotId = UUID.randomUUID();
  private final UUID stockCardId = UUID.randomUUID();
  private final UUID movementDraftId = UUID.randomUUID();
  private final LocalDate localDate = LocalDate.now();
  private final String locationCode = "AAA";
  private final StockCard stockCard = new StockCard();
  private final StockCardLocationMovementDraft movementDraft = new StockCardLocationMovementDraft();
  private final StockCardLocationMovementLineItem lineItem = StockCardLocationMovementLineItem.builder()
      .stockCardId(stockCardId)
      .srcArea("A")
      .srcLocationCode("AA20B")
      .destArea("B")
      .destLocationCode("BB30F")
      .userId(userId)
      .signature("Jimmy")
      .occurredDate(localDate)
      .quantity(10)
      .build();
  private final StockCardLocationMovementLineItemDto movementLineItemDto1 = StockCardLocationMovementLineItemDto
      .builder()
      .programId(programId)
      .orderableId(orderableId)
      .lotId(lotId)
      .isKit(false)
      .srcArea("A")
      .srcLocationCode("AA20B")
      .destArea("B")
      .destLocationCode("BB30F")
      .quantity(10)
      .stockOnHand(20)
      .build();
  private final StockCardLocationMovementLineItemDto movementLineItemDto2 = StockCardLocationMovementLineItemDto
      .builder()
      .programId(programId)
      .orderableId(orderableId)
      .lotId(lotId)
      .isKit(true)
      .srcArea("A")
      .srcLocationCode("AA20B")
      .destArea("B")
      .destLocationCode("BB30F")
      .quantity(30)
      .stockOnHand(20)
      .build();
  private final StockCardLocationMovementDto movementDto1 = StockCardLocationMovementDto.builder()
      .programId(allProgramId)
      .facilityId(facilityId)
      .occurredDate(localDate)
      .signature("Jimmy")
      .userId(userId)
      .movementLineItems(newArrayList(movementLineItemDto1))
      .build();
  private final StockCardLocationMovementDto movementDto2 = StockCardLocationMovementDto.builder()
      .programId(allProgramId)
      .facilityId(facilityId)
      .occurredDate(localDate)
      .signature("Jimmy")
      .userId(userId)
      .movementLineItems(newArrayList(movementLineItemDto2))
      .build();

  @Before
  public void setup() {
    UserDto userDto = new UserDto();
    userDto.setHomeFacilityId(facilityId);
    when(authenticationHelper.getCurrentUser()).thenReturn(userDto);

    FacilitySearchResultDto facilityDto = new FacilitySearchResultDto();
    facilityDto.setEnableLocationManagement(true);
    when(administrationsService.getFacility(facilityId)).thenReturn(facilityDto);
  }

  @Test
  public void shouldCreateMovementLineItems() {
    stockCard.setId(stockCardId);
    movementDraft.setId(movementDraftId);
    when(siglusStockCardRepository
        .findByFacilityIdAndOrderableIdAndLotId(facilityId, orderableId, lotId))
        .thenReturn(newArrayList(stockCard));
    when(siglusStockCardRepository.findByFacilityIdAndOrderableId(facilityId, orderableId))
        .thenReturn(newArrayList(stockCard));
    when(movementLineItemRepository.save(newArrayList(lineItem))).thenReturn(newArrayList(lineItem));
    when(movementDraftRepository.findByProgramIdAndFacilityId(allProgramId, facilityId))
        .thenReturn(newArrayList(movementDraft));

    service.createMovementLineItems(movementDto1);

    verify(movementDraftRepository).delete(movementDraft);
  }

  @Test
  public void shouldThrowExceptionWhenQuantityMoreThanStockOnHand() {
    // then
    exception.expect(BusinessDataException.class);
    exception.expectMessage(containsString(ERROR_MOVEMENT_QUANTITY_MORE_THAN_STOCK_ON_HAND));

    // given
    when(stockCardRepository.findByFacilityIdIn(facilityId)).thenReturn(Collections.emptyList());

    // when
    service.createMovementLineItems(movementDto2);
  }

  @Test
  public void shouldDeleteAllPreviousEmptyVirtualSohByLocationWhenMovement() {
    // given
    StockCard stockCard = new StockCard();
    stockCard.setId(stockCardId);
    when(stockCardRepository.findByFacilityIdIn(facilityId)).thenReturn(Arrays.asList(stockCard));

    UUID sohId1 = UUID.randomUUID();
    CalculatedStockOnHandByLocation soh1 = CalculatedStockOnHandByLocation
            .builder()
            .stockOnHand(0)
            .stockCardId(stockCardId)
            .locationCode(VIRTUAL_LOCATION_CODE)
            .occurredDate(Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant()))
            .build();
    soh1.setId(sohId1);
    UUID sohId2 = UUID.randomUUID();
    CalculatedStockOnHandByLocation soh2 = CalculatedStockOnHandByLocation
            .builder()
            .stockOnHand(100)
            .stockCardId(stockCardId)
            .locationCode(VIRTUAL_LOCATION_CODE)
            .occurredDate(Date.from(localDate.minusDays(10).atStartOfDay(ZoneId.systemDefault()).toInstant()))
            .build();
    soh2.setId(sohId2);

    UUID sohId3 = UUID.randomUUID();
    CalculatedStockOnHandByLocation soh3 = CalculatedStockOnHandByLocation
            .builder()
            .stockOnHand(100)
            .stockCardId(stockCardId)
            .locationCode(locationCode)
            .occurredDate(Date.from(localDate.minusDays(10).atStartOfDay(ZoneId.systemDefault()).toInstant()))
            .build();
    soh3.setId(sohId3);

    when(calculatedStockOnHandByLocationRepository.findAllByStockCardIds(Arrays.asList(stockCardId)))
            .thenReturn(Arrays.asList(soh1, soh2));

    // when
    service.deleteEmptySohVirtualLocation(facilityId);

    // then
    verify(calculatedStockOnHandByLocationRepository).deleteAllByIdIn(argumentCaptor.capture());
    List<UUID> toBeDeletedCalculatedSohByLocationIds = argumentCaptor.getValue();
    assertEquals(2, toBeDeletedCalculatedSohByLocationIds.size());
    assertTrue(toBeDeletedCalculatedSohByLocationIds.contains(sohId1));
    assertTrue(toBeDeletedCalculatedSohByLocationIds.contains(sohId2));
  }

  @Test
  public void shouldThrowExceptionWhenQuantityLessThanStockOnHandInLocationManagement() {
    // then
    exception.expect(BusinessDataException.class);
    exception.expectMessage(containsString(ERROR_MOVEMENT_QUANTITY_LESS_THAN_STOCK_ON_HAND));

    // given
    StockCard stockCard = new StockCard();
    stockCard.setId(stockCardId);
    when(stockCardRepository.findByFacilityIdIn(facilityId)).thenReturn(Collections.singletonList(stockCard));
    CalculatedStockOnHandByLocation calculatedStockOnHandByLocation = CalculatedStockOnHandByLocation
        .builder()
        .locationCode(VIRTUAL_LOCATION_CODE)
        .stockOnHand(10)
        .build();
    when(calculatedStockOnHandByLocationRepository.findLatestLocationSohByStockCardIds(
        Collections.singletonList(stockCardId)))
        .thenReturn(Collections.singletonList(calculatedStockOnHandByLocation));

    // when
    service.createMovementLineItems(movementDto1);
  }

  @Test
  public void shouldThrowExceptionWhenCannotFindStockCard() {
    exception.expect(NotFoundException.class);
    exception.expectMessage(containsString(ERROR_STOCK_CARD_NOT_FOUND));

    when(siglusStockCardRepository
        .findByFacilityIdAndOrderableIdAndLotId(facilityId, orderableId, lotId))
        .thenReturn(Collections.emptyList());

    service.createMovementLineItems(movementDto1);
  }

  @Test
  public void shouldThrowExceptionWhenCannotFindMovementDraft() {
    exception.expect(NotFoundException.class);
    exception.expectMessage(containsString(ERROR_MOVEMENT_DRAFT_NOT_FOUND));

    when(siglusStockCardRepository
        .findByFacilityIdAndOrderableIdAndLotId(facilityId, orderableId, lotId))
        .thenReturn(newArrayList(stockCard));
    when(movementLineItemRepository.save(newArrayList(lineItem))).thenReturn(newArrayList(lineItem));
    when(movementDraftRepository.findByProgramIdAndFacilityId(programId, facilityId))
        .thenReturn(Collections.emptyList());

    service.createMovementLineItems(movementDto1);
  }

  @Test
  public void shouldReturnLocationMovementInfoWhenGetByStockCardIdAndLocationCode() {
    List<LocationMovementLineItemDto> locationMovementLineItemDtos = createLocationMovementLineItemDtos();
    when(calculatedStockOnHandByLocationRepository.getStockMovementWithLocation(stockCardId, locationCode)).thenReturn(
        locationMovementLineItemDtos);
    when(calculatedStockOnHandByLocationRepository.getProductLocationMovement(stockCardId, locationCode)).thenReturn(
        locationMovementLineItemDtos);

    when(stockCardRepository.getOne(stockCardId)).thenReturn(createStockCard());
    when(facilityReferenceDataService.findOne(facilityId)).thenReturn(createFacilityDto());
    when(programReferenceDataService.findOne(programId)).thenReturn(createProgramDto());
    when(orderableReferenceDataService.findOne(orderableId)).thenReturn(createOrderableDto());
    when(lotReferenceDataService.findOne(lotId)).thenReturn(createLotDto());
    when(siglusStockCardService.findStockCardById(stockCardId)).thenReturn(createStockCardDto());
    when(calculatedStockOnHandByLocationRepository.findRecentlySohByStockCardIdAndLocationCode(stockCardId,
        locationCode)).thenReturn(
        Optional.of(200));
    LocationMovementDto locationMovementDto = service.getLocationMovementDto(stockCardId, locationCode);
    assertThat(locationMovementDto.getLineItems().size()).isEqualTo(11);
  }

  private List<LocationMovementLineItemDto> createLocationMovementLineItemDtos() {
    LocationMovementLineItemDto locationMovementLineItemDto1 = new LocationMovementLineItemDto();
    locationMovementLineItemDto1.setAdjustment("Move to 1000");
    locationMovementLineItemDto1.setReasonCategory("ADJUSTMENT");
    locationMovementLineItemDto1.setReasonType("DEBIT");
    locationMovementLineItemDto1.setLocationCode("BA01B");
    locationMovementLineItemDto1.setArea("Armazem Princip1");
    locationMovementLineItemDto1.setQuantity(5);
    locationMovementLineItemDto1.setLineItemQuantity(5);
    locationMovementLineItemDto1.setOccurredDate(LocalDate.of(2022, 8, 30));
    locationMovementLineItemDto1.setProcessedDate(ZonedDateTime.now());
    locationMovementLineItemDto1.setSoh(10);
    LocationMovementLineItemDto locationMovementLineItemDto2 = new LocationMovementLineItemDto();
    locationMovementLineItemDto2.setAdjustment("Move to 400");
    locationMovementLineItemDto2.setReasonCategory("ADJUSTMENT");
    locationMovementLineItemDto2.setReasonType("CREDIT");
    locationMovementLineItemDto2.setLocationCode("BA098");
    locationMovementLineItemDto2.setArea("Armazem Prncipal");
    locationMovementLineItemDto2.setQuantity(5);
    locationMovementLineItemDto2.setLineItemQuantity(5);
    locationMovementLineItemDto2.setOccurredDate(LocalDate.of(2022, 8, 30));
    locationMovementLineItemDto2.setProcessedDate(ZonedDateTime.now());
    locationMovementLineItemDto2.setSoh(10);
    LocationMovementLineItemDto locationMovementLineItemDto3 = new LocationMovementLineItemDto();
    locationMovementLineItemDto3.setReasonCategory("INVENTORY");
    locationMovementLineItemDto3.setLocationCode("BA01D");
    locationMovementLineItemDto3.setArea("Armazem Principal");
    locationMovementLineItemDto3.setQuantity(5);
    locationMovementLineItemDto3.setLineItemQuantity(10);
    locationMovementLineItemDto3.setOccurredDate(LocalDate.of(2022, 8, 30));
    locationMovementLineItemDto3.setProcessedDate(ZonedDateTime.now());
    locationMovementLineItemDto3.setSoh(10);
    locationMovementLineItemDto3.setSignature("fxf");
    LocationMovementLineItemDto locationMovementLineItemDto4 = new LocationMovementLineItemDto();
    locationMovementLineItemDto4.setReasonCategory("RECEIVE");
    locationMovementLineItemDto4.setLocationCode("BA01D");
    locationMovementLineItemDto4.setArea("Armazem Principal");
    locationMovementLineItemDto4.setQuantity(5);
    locationMovementLineItemDto4.setLineItemQuantity(5);
    locationMovementLineItemDto4.setOccurredDate(LocalDate.of(2022, 8, 30));
    locationMovementLineItemDto4.setProcessedDate(ZonedDateTime.now());
    locationMovementLineItemDto4.setSoh(10);
    locationMovementLineItemDto4.setSignature("fxf");
    LocationMovementLineItemDto locationMovementLineItemDto5 = new LocationMovementLineItemDto();
    locationMovementLineItemDto5.setReasonCategory("ISSUE");
    locationMovementLineItemDto5.setLocationCode("BA01D");
    locationMovementLineItemDto5.setArea("Armazem Principal");
    locationMovementLineItemDto5.setQuantity(5);
    locationMovementLineItemDto5.setLineItemQuantity(5);
    locationMovementLineItemDto5.setOccurredDate(LocalDate.of(2022, 8, 30));
    locationMovementLineItemDto5.setProcessedDate(ZonedDateTime.now());
    locationMovementLineItemDto5.setSoh(10);
    locationMovementLineItemDto5.setSignature("fxf");
    LinkedList<LocationMovementLineItemDto> locationMovementLineItemDtos = new LinkedList<>();
    locationMovementLineItemDtos.add(locationMovementLineItemDto1);
    locationMovementLineItemDtos.add(locationMovementLineItemDto2);
    locationMovementLineItemDtos.add(locationMovementLineItemDto3);
    locationMovementLineItemDtos.add(locationMovementLineItemDto4);
    locationMovementLineItemDtos.add(locationMovementLineItemDto5);
    return locationMovementLineItemDtos;
  }

  private StockCardDto createStockCardDto() {
    FacilityDto facilityDto = FacilityDto.builder().name("facility").build();
    ProgramDto programDto = ProgramDto.builder().id(programId).name("via").build();
    DispensableDto dispensableDto = new DispensableDto();
    dispensableDto.setDisplayUnit("each");
    LotDto lotDto = LotDto.builder().lotCode("lotcode").build();
    OrderableDto orderableDto = OrderableDto.builder().id(orderableId).fullProductName("product").productCode("aaaddd")
        .dispensable(dispensableDto).build();
    return StockCardDto.builder().lot(lotDto).orderable(orderableDto).facility(facilityDto)
        .program(programDto).build();
  }

  private StockCard createStockCard() {
    return StockCard.builder()
        .facilityId(facilityId)
        .programId(programId)
        .orderableId(orderableId)
        .lotId(lotId)
        .build();
  }

  private FacilityDto createFacilityDto() {
    return FacilityDto.builder().name("facility").build();
  }

  private ProgramDto createProgramDto() {
    return ProgramDto.builder().id(programId).name("via").build();
  }

  private OrderableDto createOrderableDto() {
    DispensableDto dispensableDto = new DispensableDto();
    dispensableDto.setDisplayUnit("each");
    return OrderableDto.builder()
        .id(orderableId)
        .fullProductName("product")
        .productCode("aaaddd")
        .dispensable(dispensableDto)
        .build();
  }

  private LotDto createLotDto() {
    return LotDto.builder().lotCode("lotcode").build();
  }
}