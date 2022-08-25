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

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;
import org.siglus.siglusapi.dto.StockMovementResDto;
import org.siglus.siglusapi.dto.android.EventTime;
import org.siglus.siglusapi.dto.android.Lot;
import org.siglus.siglusapi.dto.android.LotMovement;
import org.siglus.siglusapi.dto.android.MovementDetail;
import org.siglus.siglusapi.dto.android.PeriodOfProductMovements;
import org.siglus.siglusapi.dto.android.ProductLotCode;
import org.siglus.siglusapi.dto.android.ProductLotStock;
import org.siglus.siglusapi.dto.android.ProductMovement;
import org.siglus.siglusapi.dto.android.StocksOnHand;
import org.siglus.siglusapi.dto.android.enumeration.MovementType;
import org.siglus.siglusapi.repository.StockManagementRepository;

@SuppressWarnings("PMD.TooManyMethods")
@RunWith(MockitoJUnitRunner.class)
public class SiglusStockMovementServiceTest {

  @Rule
  public final ExpectedException expectedEx = ExpectedException.none();

  @Mock
  private StockManagementRepository stockManagementRepository;

  @InjectMocks
  private StockMovementService stockMovementService;
  private UUID facilityId;

  private UUID orderableId;

  @Before
  public void prepare() {
    MockitoAnnotations.initMocks(this);
    facilityId = UUID.randomUUID();
    orderableId = UUID.randomUUID();
  }

  @Test
  public void shouldReturnNullWhenNoMovements() {
    List<StockMovementResDto> productMovements = stockMovementService.getProductMovements(null, facilityId, null,
        null);
    assertEquals(new ArrayList<>(), productMovements);
  }

  @Test
  public void shouldReturnMovementWhenFindByFacilityId() {
    PeriodOfProductMovements period = new PeriodOfProductMovements(mockProductMovements(), mockStocksOnHand());
    when(stockManagementRepository.getAllProductMovements(any(), any(), any(), any()))
        .thenReturn(period);
    List<StockMovementResDto> productMovements = stockMovementService.getProductMovements(null, null, null, null);
    assertEquals(3, productMovements.size());
  }

  @Test
  public void shouldReturnStockMovementByProductWhenGetStockMovementByServiceWhenGivenFacilityIdAndOrderableiId() {
    PeriodOfProductMovements periodOfProductMovements = mockPeriodOfProductMovements();
    HashSet<UUID> orderableIds = new HashSet<>();
    orderableIds.add(orderableId);
    when(stockManagementRepository.getAllProductMovements(facilityId, orderableIds)).thenReturn(
        periodOfProductMovements);
    List<StockMovementResDto> movementsByProduct = stockMovementService.getMovementsByProduct(facilityId, orderableId);
    assertEquals(movementsByProduct.size(), 3);
  }

  private PeriodOfProductMovements mockPeriodOfProductMovements() {
    List<ProductMovement> productMovementList = mockProductMovements();
    StocksOnHand stocksOnHand = mockStocksOnHand();
    PeriodOfProductMovements periodOfProductMovements = new PeriodOfProductMovements(productMovementList, stocksOnHand);
    return periodOfProductMovements;
  }


  private StocksOnHand mockStocksOnHand() {
    ProductLotStock lot1 = ProductLotStock.builder()
        .code(ProductLotCode.of("test1", "lot1"))
        .productName("Test1")
        .stockQuantity(10)
        .eventTime(EventTime.fromRequest(LocalDate.of(2021, 10, 1), Instant.now()))
        .expirationDate(java.sql.Date.valueOf(LocalDate.of(2023, 12, 31)))
        .build();
    ProductLotStock lot2 = ProductLotStock.builder()
        .code(ProductLotCode.of("test2", "lot2"))
        .productName("Test2")
        .stockQuantity(9)
        .eventTime(EventTime.fromRequest(LocalDate.of(2021, 9, 15), Instant.now()))
        .expirationDate(java.sql.Date.valueOf(LocalDate.of(2023, 12, 31)))
        .build();
    ProductLotStock lot3 = ProductLotStock.builder()
        .code(ProductLotCode.of("test3", "lot3"))
        .productName("Test3")
        .stockQuantity(8)
        .eventTime(EventTime.fromRequest(LocalDate.of(2021, 8, 31), Instant.now()))
        .expirationDate(java.sql.Date.valueOf(LocalDate.of(2023, 12, 31)))
        .build();
    ProductLotStock kitLot = ProductLotStock.builder()
        .code(ProductLotCode.of("26A01", null))
        .productName("Some kit")
        .stockQuantity(10)
        .eventTime(EventTime.fromRequest(LocalDate.of(2021, 10, 31), Instant.now()))
        .build();
    return new StocksOnHand(asList(lot1, lot2, lot3, kitLot));
  }

  private List<ProductMovement> mockProductMovements() {
    LotMovement movement1Lot1 = LotMovement.builder()
        .stockQuantity(10)
        .movementDetail(new MovementDetail(10, MovementType.ISSUE, "MATERNITY"))
        .lot(Lot.fromDatabase("lot1", java.sql.Date.valueOf("2023-12-31")))
        .build();
    Timestamp serverProccessAt = Timestamp.valueOf("2021-11-01 01:23:45");
    ProductMovement movement1 = ProductMovement.builder()
        .productCode("test")
        .stockQuantity(10)
        .eventTime(
            EventTime.fromDatabase(java.sql.Date.valueOf("2021-10-01"), "2021-09-15T01:23:45Z", serverProccessAt))
        .movementDetail(new MovementDetail(10, MovementType.ISSUE, "Outros"))
        .lotMovements(singletonList(movement1Lot1))
        .processedAt(serverProccessAt.toInstant())
        .sourcefreetext("123")
        .build();
    LotMovement movement2Lot1 = LotMovement.builder()
        .stockQuantity(10)
        .movementDetail(new MovementDetail(10, MovementType.RECEIVE, "DISTRICT_DDM"))
        .lot(Lot.fromDatabase("lot1", java.sql.Date.valueOf("2023-12-31")))
        .build();
    ProductMovement movement2 = ProductMovement.builder()
        .productCode("test")
        .stockQuantity(10)
        .eventTime(
            EventTime.fromDatabase(java.sql.Date.valueOf("2021-10-01"), "2021-10-01T01:23:45Z", serverProccessAt))
        .movementDetail(new MovementDetail(10, MovementType.RECEIVE, "Outros"))
        .lotMovements(singletonList(movement2Lot1))
        .processedAt(serverProccessAt.toInstant())
        .build();
    ProductMovement movement3 = ProductMovement.builder()
        .productCode("26A01")
        .stockQuantity(10)
        .eventTime(EventTime.fromDatabase(Date.valueOf("2021-10-31"), "2021-11-01T01:23:45Z", serverProccessAt))
        .movementDetail(new MovementDetail(10, MovementType.ADJUSTMENT, "DISTRICT_DDM"))
        .processedAt(serverProccessAt.toInstant())
        .build();
    return asList(movement1, movement2, movement3);
  }
}
