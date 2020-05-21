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

package org.openlmis.requisition.service.stockmanagement;

import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.requisition.dto.ApprovedProductDto;
import org.openlmis.requisition.dto.stockmanagement.StockCardSummaryDto;
import org.openlmis.requisition.service.referencedata.ApproveProductsAggregator;
import org.openlmis.requisition.testutils.ApprovedProductDtoDataBuilder;
import org.openlmis.requisition.testutils.OrderableDtoDataBuilder;
import org.openlmis.requisition.testutils.StockCardSummaryDtoDataBuilder;

@RunWith(MockitoJUnitRunner.class)
public class StandardStockOnHandRetrieverTest extends StockOnHandRetrieverTest {

  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Mock
  private StockCardSummariesStockManagementService stockCardSummariesStockManagementService;

  private UUID programId = UUID.randomUUID();
  private UUID facilityId = UUID.randomUUID();
  private UUID orderable = UUID.randomUUID();
  private UUID orderable2 = UUID.randomUUID();
  private LocalDate asOfDate = LocalDate.now();

  private ApproveProductsAggregator products;

  private StockCardSummaryDto stockCardSummary = new StockCardSummaryDtoDataBuilder()
      .withOrderableId(orderable)
      .withStockOnHand(15)
      .buildAsDto();
  private StockCardSummaryDto stockCardSummary2 = new StockCardSummaryDtoDataBuilder()
      .withOrderableId(orderable2)
      .withStockOnHand(20)
      .buildAsDto();

  @Before
  public void setUp() {
    ApprovedProductDto approvedProduct = new ApprovedProductDtoDataBuilder()
        .withOrderable(new OrderableDtoDataBuilder()
            .withId(orderable)
            .withProgramOrderable(programId, true)
            .buildAsDto())
        .buildAsDto();

    ApprovedProductDto approvedProduct2 = new ApprovedProductDtoDataBuilder()
        .withOrderable(new OrderableDtoDataBuilder()
            .withId(orderable2)
            .withProgramOrderable(programId, true)
            .buildAsDto())
        .buildAsDto();

    products = new ApproveProductsAggregator(
        Lists.newArrayList(approvedProduct, approvedProduct2), programId);

    when(stockCardSummariesStockManagementService
        .search(programId, facilityId, products.getFullSupplyOrderableIds(), asOfDate))
        .thenReturn(Lists.newArrayList(stockCardSummary, stockCardSummary2));
  }

  @Override
  StockOnHandRetriever getRetriever() {
    return new StandardStockOnHandRetriever(
        stockCardSummariesStockManagementService,
        products, programId,
        facilityId, asOfDate
    );
  }

  @Override
  void assertStockOnHands(Map<UUID, Integer> stockOnHands) {
    assertThat(stockOnHands.size(), is(2));
    assertThat(stockOnHands, hasEntry(orderable, stockCardSummary.getStockOnHand()));
    assertThat(stockOnHands, hasEntry(orderable2, stockCardSummary2.getStockOnHand()));
  }

  @Test
  public void shouldRetrieveAndReturnStockCardsEvenIfNotAllAreAvailable() {
    when(stockCardSummariesStockManagementService
        .search(programId, facilityId, products.getFullSupplyOrderableIds(), asOfDate))
        .thenReturn(Lists.newArrayList(stockCardSummary));

    Map<UUID, Integer> stockOnHands = getRetriever().get();

    assertThat(stockOnHands.size(), is(1));
    assertThat(stockOnHands, hasEntry(orderable, stockCardSummary.getStockOnHand()));
  }
}
