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

import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyList;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.stockmanagement.domain.card.StockCard;
import org.siglus.siglusapi.dto.FacilityRemovedLotDto;
import org.siglus.siglusapi.exception.BusinessDataException;
import org.siglus.siglusapi.util.FacilityConfigHelper;

@RunWith(MockitoJUnitRunner.class)
public class SiglusFacilityServiceTest {

  @InjectMocks
  private SiglusFacilityService siglusFacilityService;
  @Mock
  private FacilityConfigHelper facilityConfigHelper;
  @Mock
  private SiglusLotService siglusLotService;
  @Mock
  private SiglusStockCardService siglusStockCardService;

  @Test
  public void shouldSuccessWhenRemoveExpiredLotsGivenLotsIsEmpty() {
    try {
      siglusFacilityService.removeExpiredLots(UUID.randomUUID(), new ArrayList<>());
    } catch (Exception e) {
      fail("Unexpected exception: " + e.getMessage());
    }
  }

  @Test(expected = BusinessDataException.class)
  public void shouldThrowExceptionWhenRemoveExpiredLotsGivenFacilityHasLocationButLotsHaveNot() {
    Mockito.when(facilityConfigHelper.isLocationManagementEnabled(any())).thenReturn(true);
    List<FacilityRemovedLotDto> lots = new ArrayList<>();
    lots.add(buildFacilityRemovedLotDtoWithoutLocation());

    siglusFacilityService.removeExpiredLots(UUID.randomUUID(), lots);
  }

  @Test(expected = BusinessDataException.class)
  public void shouldThrowExceptionWhenRemoveExpiredLotsGivenFacilityHasNotLocationButLotsHave() {
    Mockito.when(facilityConfigHelper.isLocationManagementEnabled(any())).thenReturn(false);
    List<FacilityRemovedLotDto> lots = new ArrayList<>();
    lots.add(buildFacilityRemovedLotDtoWithLocation());

    siglusFacilityService.removeExpiredLots(UUID.randomUUID(), lots);
  }

  @Test(expected = BusinessDataException.class)
  public void shouldThrowExceptionWhenRemoveExpiredLotsGivenStockCardIdDoesNotExist() {
    Mockito.when(facilityConfigHelper.isLocationManagementEnabled(any())).thenReturn(false);
    List<FacilityRemovedLotDto> lots = new ArrayList<>();
    lots.add(buildFacilityRemovedLotDtoWithoutLocation());
    Mockito.when(siglusStockCardService.findStockCardByIds(any())).thenReturn(new ArrayList<>());

    siglusFacilityService.removeExpiredLots(UUID.randomUUID(), lots);
  }

  @Test(expected = BusinessDataException.class)
  public void shouldThrowExceptionWhenRemoveExpiredLotsGivenStockCardIdDoesNotBelongToFacility() {
    Mockito.when(facilityConfigHelper.isLocationManagementEnabled(any())).thenReturn(false);
    FacilityRemovedLotDto lotDto = buildFacilityRemovedLotDtoWithoutLocation();
    List<FacilityRemovedLotDto> lots = new ArrayList<>();
    lots.add(lotDto);
    StockCard stockCard = StockCard.builder().facilityId(UUID.randomUUID()).build();
    stockCard.setId(lotDto.getStockCardId());
    List<StockCard> stockCards = new ArrayList<>();
    stockCards.add(stockCard);
    Mockito.when(siglusStockCardService.findStockCardByIds(any())).thenReturn(stockCards);

    siglusFacilityService.removeExpiredLots(UUID.randomUUID(), lots);
  }

  @Test
  public void shouldCallRemoveExpiredLotsInLotServiceOneTimeWhenRemoveExpiredLots() {
    UUID facilityId = UUID.randomUUID();
    Mockito.when(facilityConfigHelper.isLocationManagementEnabled(any())).thenReturn(false);
    FacilityRemovedLotDto lotDto = buildFacilityRemovedLotDtoWithoutLocation();
    List<FacilityRemovedLotDto> lots = new ArrayList<>();
    lots.add(lotDto);
    StockCard stockCard = StockCard.builder().facilityId(facilityId).build();
    stockCard.setId(lotDto.getStockCardId());
    List<StockCard> stockCards = new ArrayList<>();
    stockCards.add(stockCard);
    Mockito.when(siglusStockCardService.findStockCardByIds(any())).thenReturn(stockCards);
    Mockito.doNothing().when(siglusLotService).removeExpiredLots(anyList(), anyBoolean());

    siglusFacilityService.removeExpiredLots(facilityId, lots);

    Mockito.verify(siglusLotService, Mockito.times(1)).removeExpiredLots(anyList(), anyBoolean());
  }

  private FacilityRemovedLotDto buildFacilityRemovedLotDtoWithLocation() {
    FacilityRemovedLotDto dto = buildFacilityRemovedLotDtoWithoutLocation();
    dto.setArea("ZONE_A");
    dto.setLocationCode("ABCDEF");
    return dto;
  }

  private FacilityRemovedLotDto buildFacilityRemovedLotDtoWithoutLocation() {
    FacilityRemovedLotDto dto = new FacilityRemovedLotDto();
    dto.setStockCardId(UUID.randomUUID());
    dto.setQuantity(10);
    return dto;
  }
}
