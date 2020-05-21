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

package org.openlmis.requisition.domain.requisition;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.UUID;
import org.junit.Test;

public class StockDataTest {
  private UUID orderableId = UUID.randomUUID();
  private int stockOnHand = 10;
  private int beginningBalance = 15;

  private Map<UUID, Integer> stockOnHands = ImmutableMap.of(orderableId, stockOnHand);
  private Map<UUID, Integer> beginningBalances = ImmutableMap.of(orderableId, beginningBalance);

  private StockData stockData = new StockData(stockOnHands, beginningBalances);

  @Test
  public void shouldFindStockOnHandByOrderableId() {
    assertThat(stockData.getStockOnHand(orderableId), is(stockOnHand));
  }

  @Test
  public void shouldReturnNullIfStockOnHandForOrderableDoesNotExist() {
    assertThat(stockData.getStockOnHand(UUID.randomUUID()), is(nullValue()));
  }

  @Test
  public void shouldFindBeginningBalanceByOrderableId() {
    assertThat(stockData.getBeginningBalance(orderableId), is(beginningBalance));
  }

  @Test
  public void shouldReturnNullIfBeginningBalanceForOrderableDoesNotExist() {
    assertThat(stockData.getStockOnHand(UUID.randomUUID()), is(nullValue()));
  }

  @Test
  public void shouldHaveDataForOrderableIdInMaps() {
    assertTrue(stockData.hasDataFor(orderableId));
  }

  @Test
  public void shouldNotHaveDataForOrderableIdNotInMaps() {
    assertFalse(stockData.hasDataFor(UUID.randomUUID()));
  }
}
