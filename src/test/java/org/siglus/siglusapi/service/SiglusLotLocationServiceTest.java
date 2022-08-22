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
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.siglus.siglusapi.constant.FieldConstants.MONTHLY;
import static org.siglus.siglusapi.constant.FieldConstants.QUARTERLY;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_LOCATIONS_BY_FACILITY_NOT_FOUND;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.referencedata.domain.Lot;
import org.openlmis.referencedata.repository.LotRepository;
import org.openlmis.stockmanagement.domain.card.StockCard;
import org.openlmis.stockmanagement.domain.event.CalculatedStockOnHand;
import org.openlmis.stockmanagement.repository.CalculatedStockOnHandRepository;
import org.openlmis.stockmanagement.repository.StockCardRepository;
import org.siglus.siglusapi.domain.CalculatedStockOnHandByLocation;
import org.siglus.siglusapi.domain.FacilityLocations;
import org.siglus.siglusapi.dto.FacilityLocationsDto;
import org.siglus.siglusapi.dto.LotLocationDto;
import org.siglus.siglusapi.dto.LotsDto;
import org.siglus.siglusapi.dto.UserDto;
import org.siglus.siglusapi.exception.NotFoundException;
import org.siglus.siglusapi.repository.CalculatedStockOnHandByLocationRepository;
import org.siglus.siglusapi.repository.FacilityLocationsRepository;
import org.siglus.siglusapi.repository.FacilityNativeRepository;
import org.siglus.siglusapi.repository.dto.FacilityProgramPeriodScheduleDto;
import org.siglus.siglusapi.util.SiglusAuthenticationHelper;

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
  private LotRepository lotRepository;

  @Mock
  private CalculatedStockOnHandByLocationRepository calculatedStockOnHandByLocationRepository;

  @Mock
  private StockCardRepository stockCardRepository;

  @Mock
  private CalculatedStockOnHandRepository calculatedStocksOnHandRepository;

  @Mock
  private FacilityNativeRepository facilityNativeRepository;

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

    List<FacilityLocationsDto> facilityLocationsDtos = service.searchLocationsByFacility();

    assertEquals(facilityId, facilityLocationsDtos.get(0).getFacilityId());
    assertEquals(area1, facilityLocationsDtos.get(0).getArea());
    assertEquals(locationCode1, facilityLocationsDtos.get(0).getLocationCode());
  }

  @Test
  public void shouldThrowExceptionWhenFacilityLocationsNotFound() {
    exception.expect(NotFoundException.class);
    exception.expectMessage(ERROR_LOCATIONS_BY_FACILITY_NOT_FOUND);

    when(facilityLocationsRepository.findByFacilityId(facilityId)).thenReturn(Collections.emptyList());

    service.searchLocationsByFacility();
  }

  @Test
  public void shouldReturnFacilityLocationByRightWhenExtraDataIsFalse() {

    // given
    when(facilityLocationsRepository.findByFacilityId(facilityId)).thenReturn(newArrayList(facilityLocations));
    LotLocationDto expectedLotLocationDto =
        LotLocationDto.builder().area(area1).locationCode(locationCode1).build();

    // when
    List<LotLocationDto> lotLocationDtos = service
        .searchLotLocationDtos(null, false, false);

    // then
    assertEquals(expectedLotLocationDto, lotLocationDtos.get(0));
  }

  @Test
  public void shouldReturnLotLocationDtosByOrderableIds() {
    // given
    StockCard stockCard = StockCard.builder().lotId(lotId1).build();
    stockCard.setId(stockCardId1);
    when(stockCardRepository.findByFacilityIdAndOrderableId(facilityId,
        orderableId)).thenReturn(Collections.singletonList(stockCard));
    CalculatedStockOnHandByLocation sohLocation1 = CalculatedStockOnHandByLocation.builder().stockOnHand(100)
        .locationCode(locationCode1).area(area1).build();
    CalculatedStockOnHandByLocation sohLocation2 = CalculatedStockOnHandByLocation.builder().stockOnHand(200)
        .locationCode(locationCode2).area(area2).build();
    when(calculatedStockOnHandByLocationRepository.findRecentlyLocationSohByStockCardId(stockCard.getId())).thenReturn(
        Arrays.asList(sohLocation1, sohLocation2));
    Lot lot = new Lot();
    lot.setExpirationDate(LocalDate.of(2022, 3, 22));
    lot.setLotCode(lotCode);
    when(lotRepository.findOne(lotId1)).thenReturn(lot);

    LotsDto lot1 = LotsDto.builder().lotCode(lotCode).lotId(lotId1).expirationDate(lot.getExpirationDate())
        .orderableId(orderableId)
        .stockOnHand(100).build();
    LotsDto lot2 = LotsDto.builder().lotCode(lotCode).lotId(lotId1).expirationDate(lot.getExpirationDate())
        .orderableId(orderableId)
        .stockOnHand(200).build();
    LotLocationDto lotLocation1 = LotLocationDto.builder().locationCode(locationCode1).area(area1)
        .lots(Collections.singletonList(lot1)).build();
    LotLocationDto lotLocation2 = LotLocationDto.builder().locationCode(locationCode2).area(area2)
        .lots(Collections.singletonList(lot2)).build();
    List<LotLocationDto> expectedLotLocationDtos = Arrays.asList(lotLocation2, lotLocation1);

    // when
    List<LotLocationDto> lotLocationDtos = service
        .searchLotLocationDtos(Collections.singletonList(orderableId), true, false);

    // then
    assertEquals(expectedLotLocationDtos, lotLocationDtos);
  }

  @Test
  public void shouldReturnRestrictedLotLocationDtosByOrderableIds() {
    // given
    StockCard stockCard1 = StockCard.builder().lotId(lotId1).programId(programId1).build();
    stockCard1.setId(stockCardId1);
    StockCard stockCard2 = StockCard.builder().lotId(lotId2).programId(programId2).build();
    stockCard2.setId(stockCardId2);
    when(stockCardRepository.findByFacilityIdAndOrderableId(facilityId,
        orderableId)).thenReturn(Arrays.asList(stockCard1, stockCard2));
    CalculatedStockOnHandByLocation sohLocation1 = CalculatedStockOnHandByLocation.builder().stockOnHand(0)
        .locationCode(locationCode1).area(area1).build();
    when(calculatedStockOnHandByLocationRepository.findRecentlyLocationSohByStockCardId(stockCard1.getId())).thenReturn(
        Collections.singletonList(sohLocation1));
    when(calculatedStockOnHandByLocationRepository.findRecentlyLocationSohByStockCardId(stockCard2.getId())).thenReturn(
        Collections.singletonList(sohLocation1));
    Lot lot = new Lot();
    lot.setExpirationDate(LocalDate.of(2022, 3, 22));
    lot.setLotCode(lotCode);
    when(lotRepository.findOne(any())).thenReturn(lot);

    when(facilityNativeRepository.findFacilityProgramPeriodScheduleByFacilityId(facilityId)).thenReturn(
        Arrays.asList(
            FacilityProgramPeriodScheduleDto
                .builder()
                .programId(programId1)
                .schedulesCode(MONTHLY)
                .build(), FacilityProgramPeriodScheduleDto
                .builder()
                .programId(programId2)
                .schedulesCode(QUARTERLY)
                .build())
    );
    CalculatedStockOnHand calculatedStockOnHand1 = new CalculatedStockOnHand();
    calculatedStockOnHand1.setStockOnHand(0);
    calculatedStockOnHand1.setOccurredDate(LocalDate.of(2022, 7, 22));
    CalculatedStockOnHand calculatedStockOnHand2 = new CalculatedStockOnHand();
    calculatedStockOnHand2.setStockOnHand(100);
    calculatedStockOnHand2.setOccurredDate(LocalDate.of(2022, 4, 22));
    when(calculatedStocksOnHandRepository
        .findByStockCardIdInAndOccurredDateLessThanEqual(any(), any()))
        .thenReturn(Arrays.asList(calculatedStockOnHand1, calculatedStockOnHand2));
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
        .searchLotLocationDtos(Collections.singletonList(orderableId), true, true);

    // then
    assertEquals(Collections.singletonList(lotLocation2), lotLocationDtos);
  }

}