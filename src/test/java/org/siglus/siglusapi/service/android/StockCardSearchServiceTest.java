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
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import org.siglus.siglusapi.dto.UserDto;
import org.siglus.siglusapi.dto.android.MovementDetail;
import org.siglus.siglusapi.dto.android.PeriodOfProductMovements;
import org.siglus.siglusapi.dto.android.ProductLotCode;
import org.siglus.siglusapi.dto.android.ProductLotStock;
import org.siglus.siglusapi.dto.android.ProductMovement;
import org.siglus.siglusapi.dto.android.StocksOnHand;
import org.siglus.siglusapi.dto.android.enumeration.MovementType;
import org.siglus.siglusapi.dto.android.response.FacilityProductMovementsResponse;
import org.siglus.siglusapi.repository.StockManagementRepository;
import org.siglus.siglusapi.service.android.mapper.ProductMovementMapper;
import org.siglus.siglusapi.util.SiglusAuthenticationHelper;

@RunWith(MockitoJUnitRunner.class)
public class StockCardSearchServiceTest {
  @InjectMocks
  private StockCardSearchService stockCardSearchService;
  @Mock
  private SiglusAuthenticationHelper authHelper;
  @Mock
  private StockManagementRepository stockManagementRepository;
  @Spy
  private ProductMovementMapper mapper = Mappers.getMapper(ProductMovementMapper.class);

  private UUID userId = UUID.randomUUID();
  private UUID facilityId = UUID.randomUUID();

  @Test
  public void shouldGetProductMovementsByTime() {
    List<ProductMovement> movements = new ArrayList<>();
    movements.add(mockMovement());
    List<ProductLotStock> allLotStocks = new ArrayList<>();
    allLotStocks.add(mockProductLotStock());
    StocksOnHand stocksOnHand = new StocksOnHand(allLotStocks);
    PeriodOfProductMovements productMovements = new PeriodOfProductMovements(movements, stocksOnHand);
    when(authHelper.getCurrentUser()).thenReturn(mockUserDto());
    when(stockManagementRepository.getAllProductMovements(any(), any(), any()))
        .thenReturn(productMovements);

    FacilityProductMovementsResponse result =
        stockCardSearchService.getProductMovementsByTime(null, LocalDate.now());

    verify(mapper).toAndroidResponse(any());
    assertEquals(movements.size(), result.getProductMovements().size());
  }

  private UserDto mockUserDto() {
    UserDto userDto = new UserDto();
    userDto.setId(userId);
    userDto.setHomeFacilityId(facilityId);
    return userDto;
  }

  private ProductMovement mockMovement() {
    return ProductMovement.builder()
        .productCode("productCode")
        .movementDetail(new MovementDetail(1, MovementType.ADJUSTMENT, "Transferência de produtos expirados"))
        .build();
  }

  private ProductLotStock mockProductLotStock() {
    return ProductLotStock.builder()
        .code(ProductLotCode.of("productCode", "lotCode"))
        .stockQuantity(1)
        .build();
  }
}
