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
import static java.util.Arrays.asList;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_LOCATIONS_BY_FACILITY_NOT_FOUND;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import junit.framework.TestCase;
import org.javers.common.collections.Sets;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.referencedata.dto.LotDto;
import org.openlmis.referencedata.web.LotController;
import org.openlmis.stockmanagement.domain.card.StockCard;
import org.openlmis.stockmanagement.domain.card.StockCardLineItem;
import org.openlmis.stockmanagement.domain.event.CalculatedStockOnHand;
import org.openlmis.stockmanagement.repository.CalculatedStockOnHandRepository;
import org.openlmis.stockmanagement.repository.StockCardRepository;
import org.siglus.siglusapi.constant.FieldConstants;
import org.siglus.siglusapi.constant.PeriodConstants;
import org.siglus.siglusapi.domain.CalculatedStockOnHandByLocation;
import org.siglus.siglusapi.domain.FacilityLocations;
import org.siglus.siglusapi.domain.OrderableIdentifiers;
import org.siglus.siglusapi.dto.FacilityLocationsDto;
import org.siglus.siglusapi.dto.InitialMoveProductFieldDto;
import org.siglus.siglusapi.dto.LotLocationDto;
import org.siglus.siglusapi.dto.LotsDto;
import org.siglus.siglusapi.dto.UserDto;
import org.siglus.siglusapi.exception.NotFoundException;
import org.siglus.siglusapi.repository.CalculatedStockOnHandByLocationRepository;
import org.siglus.siglusapi.repository.FacilityLocationsRepository;
import org.siglus.siglusapi.repository.FacilityNativeRepository;
import org.siglus.siglusapi.repository.OrderableIdentifiersRepository;
import org.siglus.siglusapi.repository.SiglusStockCardLineItemRepository;
import org.siglus.siglusapi.repository.dto.FacilityProgramPeriodScheduleDto;
import org.siglus.siglusapi.util.SiglusAuthenticationHelper;
import org.springframework.data.domain.PageImpl;

@RunWith(MockitoJUnitRunner.class)
public class SiglusLotLocationServiceTest extends TestCase {

  @Rule
  public ExpectedException exception = ExpectedException.none();

  @InjectMocks
  private SiglusLotLocationService service;

  @Mock
  private FacilityLocationsRepository facilityLocationsRepository;

  @Mock
  private SiglusAuthenticationHelper authenticationHelper;

  @Mock
  private CalculatedStockOnHandByLocationRepository calculatedStockOnHandByLocationRepository;

  @Mock
  private StockCardRepository stockCardRepository;

  @Mock
  private CalculatedStockOnHandRepository calculatedStockOnHandRepository;

  @Mock
  private FacilityNativeRepository facilityNativeRepository;

  @Mock
  private SiglusStockCardLocationMovementService stockCardLocationMovementService;

  @Mock
  private LotController lotController;

  @Mock
  private SiglusStockCardLineItemRepository siglusStockCardLineItemRepository;

  @Mock
  private SiglusStockCardSummariesService siglusStockCardSummariesService;

  @Mock
  private OrderableIdentifiersRepository orderableIdentifiersRepository;

  private final UUID facilityId = UUID.randomUUID();

  private final UUID orderableId = UUID.randomUUID();

  private final String area1 = "Armazem Principal";

  private final String area2 = "Frios";

  private final String locationCode1 = "BA01C";

  private final String locationCode2 = "AA25F";

  private final UUID stockCardId1 = UUID.randomUUID();

  private final UUID stockCardId2 = UUID.randomUUID();

  private final UUID programId1 = UUID.randomUUID();

  private final UUID lotId1 = UUID.randomUUID();

  private final UUID programId2 = UUID.randomUUID();

  private final UUID lotId2 = UUID.randomUUID();

  private final String lotCode = "08L10-2021-10-22-22/10/2021";

  private final FacilityLocations facilityLocations = FacilityLocations.builder()
      .facilityId(facilityId)
      .area(area1)
      .locationCode(locationCode1)
      .build();

  @Before
  public void setup() {
    UserDto userDto = new UserDto();
    userDto.setHomeFacilityId(facilityId);
    when(authenticationHelper.getCurrentUser()).thenReturn(userDto);
  }

  @Test
  public void shouldSearchLocationsByFacility() {
    when(facilityLocationsRepository.findByFacilityId(facilityId)).thenReturn(newArrayList(facilityLocations));

    List<FacilityLocationsDto> facilityLocationsDtos = service.searchLocationsByFacility(false);

    assertEquals(facilityId, facilityLocationsDtos.get(0).getFacilityId());
    assertEquals(area1, facilityLocationsDtos.get(0).getArea());
    assertEquals(locationCode1, facilityLocationsDtos.get(0).getLocationCode());
  }

  @Test
  public void shouldThrowExceptionWhenFacilityLocationsNotFound() {
    exception.expect(NotFoundException.class);
    exception.expectMessage(ERROR_LOCATIONS_BY_FACILITY_NOT_FOUND);

    when(facilityLocationsRepository.findByFacilityId(facilityId)).thenReturn(Collections.emptyList());

    service.searchLocationsByFacility(false);
  }

  @Test
  public void shouldReturnFacilityLocationByRightWhenExtraDataIsFalse() {

    // given
    when(facilityLocationsRepository.findByFacilityId(facilityId)).thenReturn(newArrayList(facilityLocations));
    LotLocationDto expectedLotLocationDto =
        LotLocationDto.builder().area(area1).locationCode(locationCode1).build();

    // when
    List<LotLocationDto> lotLocationDtos = service
        .searchLotLocationDtos(null, false, false, false);

    // then
    assertEquals(expectedLotLocationDto, lotLocationDtos.get(0));
  }

  @Test
  public void shouldReturnLotLocationDtosByOrderableIds() {
    // given
    StockCard stockCard = StockCard.builder().orderableId(orderableId).lotId(lotId1).build();
    stockCard.setId(stockCardId1);
    when(stockCardRepository.findByOrderableIdInAndFacilityId(Arrays.asList(orderableId), facilityId))
        .thenReturn(Collections.singletonList(stockCard));
    CalculatedStockOnHandByLocation sohLocation1 = CalculatedStockOnHandByLocation.builder()
        .stockOnHand(100).stockCardId(stockCardId1)
        .locationCode(locationCode1).area(area1).build();
    CalculatedStockOnHandByLocation sohLocation2 = CalculatedStockOnHandByLocation.builder()
        .stockOnHand(200).stockCardId(stockCardId1)
        .locationCode(locationCode2).area(area2).build();
    when(calculatedStockOnHandByLocationRepository.findRecentlyLocationSohByStockCardIds(Sets.asSet(stockCard.getId())))
        .thenReturn(Arrays.asList(sohLocation1, sohLocation2));
    when(stockCardLocationMovementService.canInitialMoveProduct(facilityId)).thenReturn(
        new InitialMoveProductFieldDto(false));
    LotDto lotDto1 = new LotDto();
    lotDto1.setId(lotId1);
    lotDto1.setExpirationDate(LocalDate.of(2022, 3, 22));
    lotDto1.setActive(true);
    lotDto1.setLotCode(lotCode);
    when(lotController.getLots(any(), any())).thenReturn(new PageImpl<>(asList(lotDto1)));

    LotsDto lot1 = LotsDto.builder().lotCode(lotCode).lotId(lotId1).expirationDate(lotDto1.getExpirationDate())
        .orderableId(orderableId)
        .stockOnHand(100).build();
    LotsDto lot2 = LotsDto.builder().lotCode(lotCode).lotId(lotId1).expirationDate(lotDto1.getExpirationDate())
        .orderableId(orderableId)
        .stockOnHand(200).build();
    LotLocationDto lotLocation1 = LotLocationDto.builder().locationCode(locationCode1).area(area1)
        .lots(Collections.singletonList(lot1)).build();
    LotLocationDto lotLocation2 = LotLocationDto.builder().locationCode(locationCode2).area(area2)
        .lots(Collections.singletonList(lot2)).build();
    List<LotLocationDto> expectedLotLocationDtos = Arrays.asList(lotLocation2, lotLocation1);

    // when
    List<LotLocationDto> lotLocationDtos = service
        .searchLotLocationDtos(Collections.singletonList(orderableId), true, false, false);

    // then
    assertEquals(expectedLotLocationDtos, lotLocationDtos);
  }

  @Test
  public void shouldReturnEmptyLotLocationDtosByOrderableIdsWhenLotIsInactive() {
    // given
    StockCard stockCard = StockCard.builder().orderableId(orderableId).lotId(lotId1).build();
    stockCard.setId(stockCardId1);
    when(stockCardRepository.findByOrderableIdInAndFacilityId(Arrays.asList(orderableId), facilityId))
        .thenReturn(Collections.singletonList(stockCard));
    CalculatedStockOnHandByLocation sohLocation1 = CalculatedStockOnHandByLocation.builder()
        .stockOnHand(100).stockCardId(stockCardId1)
        .locationCode(locationCode1).area(area1).build();
    CalculatedStockOnHandByLocation sohLocation2 = CalculatedStockOnHandByLocation.builder()
        .stockOnHand(200).stockCardId(stockCardId1)
        .locationCode(locationCode2).area(area2).build();
    when(calculatedStockOnHandByLocationRepository.findRecentlyLocationSohByStockCardIds(Sets.asSet(stockCard.getId())))
        .thenReturn(Arrays.asList(sohLocation1, sohLocation2));
    when(stockCardLocationMovementService.canInitialMoveProduct(facilityId)).thenReturn(
        new InitialMoveProductFieldDto(false));
    LotDto lotDto1 = new LotDto();
    lotDto1.setId(lotId1);
    lotDto1.setExpirationDate(LocalDate.of(2022, 3, 22));
    lotDto1.setActive(false);
    lotDto1.setLotCode(lotCode);
    when(lotController.getLots(any(), any())).thenReturn(new PageImpl<>(Collections.singletonList(lotDto1)));
    LotLocationDto lotLocation1 = LotLocationDto.builder().locationCode(locationCode1).area(area1)
        .lots(Collections.emptyList()).build();
    LotLocationDto lotLocation2 = LotLocationDto.builder().locationCode(locationCode2).area(area2)
        .lots(Collections.emptyList()).build();
    List<LotLocationDto> expectedLotLocationDtos = Arrays.asList(lotLocation2, lotLocation1);

    // when
    List<LotLocationDto> lotLocationDtos = service
        .searchLotLocationDtos(Collections.singletonList(orderableId), true, false, false);

    // then
    assertEquals(expectedLotLocationDtos, lotLocationDtos);
  }

  @Test
  public void shouldReturnRestrictedLotLocationDtosByOrderableIds() {
    // given
    StockCard stockCard1 = StockCard.builder().orderableId(orderableId).lotId(lotId1)
        .programId(programId1).build();
    stockCard1.setId(stockCardId1);
    StockCard stockCard2 = StockCard.builder().orderableId(orderableId).lotId(lotId2)
        .programId(programId2).build();
    stockCard2.setId(stockCardId2);
    when(stockCardRepository.findByOrderableIdInAndFacilityId(Collections.singletonList(orderableId),
        facilityId)).thenReturn(Arrays.asList(stockCard1, stockCard2));
    CalculatedStockOnHandByLocation sohLocation1 = CalculatedStockOnHandByLocation.builder().stockOnHand(0)
        .stockCardId(stockCardId2).locationCode(locationCode1).area(area1).build();
    when(calculatedStockOnHandByLocationRepository
        .findRecentlyLocationSohByStockCardIds(Sets.asSet(stockCard2.getId())))
        .thenReturn(Collections.singletonList(sohLocation1));
    when(stockCardLocationMovementService.canInitialMoveProduct(facilityId)).thenReturn(
        new InitialMoveProductFieldDto(false));

    LotDto lot = new LotDto();
    lot.setId(lotId2);
    lot.setExpirationDate(LocalDate.of(2022, 3, 22));
    lot.setActive(true);
    lot.setLotCode(lotCode);
    when(lotController.getLots(any(), any())).thenReturn(new PageImpl<>(Collections.singletonList(lot)));

    when(facilityNativeRepository.findFacilityProgramPeriodScheduleByFacilityId(facilityId)).thenReturn(
        Arrays.asList(
            FacilityProgramPeriodScheduleDto
                .builder()
                .programId(programId1)
                .schedulesCode(PeriodConstants.MONTH_SCHEDULE_CODE.toString())
                .build(), FacilityProgramPeriodScheduleDto
                .builder()
                .programId(programId2)
                .schedulesCode(PeriodConstants.QUARTERLY_SCHEDULE_CODE.toString())
                .build())
    );

    LocalDate outTimeRangeDate = LocalDate.now().minusMonths(12);
    CalculatedStockOnHand calculatedStockOnHand1 = new CalculatedStockOnHand();
    calculatedStockOnHand1.setStockOnHand(0);
    calculatedStockOnHand1.setOccurredDate(outTimeRangeDate);
    calculatedStockOnHand1.setStockCardId(stockCardId1);
    LocalDate inTimeRangeDate = LocalDate.now().minusMonths(2);
    CalculatedStockOnHand calculatedStockOnHand2 = new CalculatedStockOnHand();
    calculatedStockOnHand2.setStockOnHand(120);
    calculatedStockOnHand2.setOccurredDate(inTimeRangeDate);
    calculatedStockOnHand2.setStockCardId(stockCardId2);

    StockCardLineItem stockCardLineItem1 =
        StockCardLineItem.builder().stockCard(stockCard1).occurredDate(outTimeRangeDate)
            .quantity(100).build();
    StockCardLineItem stockCardLineItem2 =
        StockCardLineItem.builder().stockCard(stockCard1).occurredDate(outTimeRangeDate)
            .quantity(100).build();
    StockCardLineItem stockCardLineItem3 =
        StockCardLineItem.builder().stockCard(stockCard2).occurredDate(inTimeRangeDate)
            .quantity(120).build();

    when(siglusStockCardLineItemRepository
        .findAllByStockCardIn(Arrays.asList(stockCard1, stockCard2))).thenReturn(
        Arrays.asList(stockCardLineItem1, stockCardLineItem2, stockCardLineItem3));

    when(calculatedStockOnHandRepository.findLatestStockOnHands(any(), any())).thenReturn(Arrays.asList(
        calculatedStockOnHand1, calculatedStockOnHand2
    ));

    LotsDto lot2 = LotsDto
        .builder()
        .lotCode(lotCode)
        .lotId(lotId2)
        .expirationDate(lot.getExpirationDate())
        .orderableId(orderableId)
        .stockOnHand(0).build();
    LotLocationDto lotLocation2 = LotLocationDto.builder().locationCode(locationCode1).area(area1)
        .lots(Collections.singletonList(lot2)).build();
    // when
    List<LotLocationDto> lotLocationDtos = service
        .searchLotLocationDtos(Collections.singletonList(orderableId), true, true, false);

    // then
    assertEquals(Collections.singletonList(lotLocation2), lotLocationDtos);
  }

  @Test
  public void shouldReturnNoMovementLotsByOrderableIdsWhenReturnNoMovementFlagIsTrue() {
    // given
    StockCard stockCard = StockCard.builder().orderableId(orderableId).lotId(lotId1).build();
    stockCard.setId(stockCardId1);
    when(stockCardRepository.findByOrderableIdInAndFacilityId(Arrays.asList(orderableId), facilityId))
        .thenReturn(Collections.singletonList(stockCard));
    CalculatedStockOnHandByLocation sohLocation1 = CalculatedStockOnHandByLocation.builder()
        .stockOnHand(100).stockCardId(stockCardId1)
        .locationCode(locationCode1).area(area1).build();
    CalculatedStockOnHandByLocation sohLocation2 = CalculatedStockOnHandByLocation.builder()
        .stockOnHand(200).stockCardId(stockCardId1)
        .locationCode(locationCode2).area(area2).build();
    when(calculatedStockOnHandByLocationRepository.findRecentlyLocationSohByStockCardIds(Sets.asSet(stockCard.getId())))
        .thenReturn(Arrays.asList(sohLocation1, sohLocation2));
    when(stockCardLocationMovementService.canInitialMoveProduct(facilityId)).thenReturn(
        new InitialMoveProductFieldDto(false));
    UUID tradeLineItemId = UUID.randomUUID();
    LotDto lotDto1 = new LotDto();
    lotDto1.setId(lotId1);
    lotDto1.setTradeItemId(tradeLineItemId);
    lotDto1.setExpirationDate(LocalDate.of(2022, 3, 22));
    lotDto1.setActive(false);
    lotDto1.setLotCode(lotCode);
    when(lotController.getLots(any(), any())).thenReturn(new PageImpl<>(Collections.singletonList(lotDto1)));
    LotLocationDto lotLocation1 = LotLocationDto.builder().locationCode(locationCode1).area(area1)
        .lots(Collections.emptyList()).build();
    LotLocationDto lotLocation2 = LotLocationDto.builder().locationCode(locationCode2).area(area2)
        .lots(Collections.emptyList()).build();
    LotsDto lotsDto = LotsDto
        .builder()
        .lotId(lotId1)
        .orderableId(orderableId)
        .lotCode(lotCode)
        .expirationDate(LocalDate.of(2022, 3, 22))
        .build();
    LotLocationDto lotLocation3 = LotLocationDto.builder().lots(Collections.singletonList(lotsDto)).build();
    List<LotLocationDto> expectedLotLocationDtos = Arrays.asList(lotLocation2, lotLocation1, lotLocation3);

    when(siglusStockCardSummariesService.getLotsDataByOrderableIds(any())).thenReturn(
        Collections.singletonList(lotDto1));
    when(orderableIdentifiersRepository
        .findByKeyAndValueIn(FieldConstants.TRADE_ITEM, Collections.singletonList(tradeLineItemId.toString())))
        .thenReturn(Collections.singletonList(OrderableIdentifiers
            .builder()
            .orderableId(orderableId)
            .value(tradeLineItemId.toString())
            .build()));

    // when
    List<LotLocationDto> lotLocationDtos = service
        .searchLotLocationDtos(Collections.singletonList(orderableId), true, false, true);

    // then
    assertEquals(expectedLotLocationDtos, lotLocationDtos);
  }

}