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

package org.openlmis.referencedata.service;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.referencedata.domain.Lot;
import org.openlmis.referencedata.domain.TradeItem;
import org.openlmis.referencedata.repository.LotRepository;
import org.openlmis.referencedata.repository.TradeItemRepository;
import org.openlmis.referencedata.testbuilder.LotDataBuilder;
import org.openlmis.referencedata.testbuilder.TradeItemDataBuilder;
import org.openlmis.referencedata.util.Pagination;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@RunWith(MockitoJUnitRunner.class)
public class LotServiceTest {

  @Mock
  private LotRepository lotRepository;

  @Mock
  private TradeItemRepository tradeItemRepository;

  @InjectMocks
  private LotService lotService = new LotService();

  private Pageable pageable;

  private TradeItem tradeItem;

  private Lot lot;

  private Page<Lot> expected;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    pageable = new PageRequest(0, 2);
    tradeItem = new TradeItemDataBuilder().build();
    lot = new LotDataBuilder()
        .withTradeItem(tradeItem)
        .build();

    expected = Pagination.getPage(ImmutableList.of(lot), pageable);
  }

  @Test
  public void searchShouldReturnRepositoryResult() {
    List<UUID> tradeItemIds = singletonList(tradeItem.getId());
    LotSearchParams lotSearchParams = new LotSearchParams(
        LocalDate.now(),
        tradeItemIds,
        lot.getLotCode(),
        ImmutableList.of(lot.getId())
    );

    List<TradeItem> tradeItems = singletonList(tradeItem);
    when(tradeItemRepository.findAll(tradeItemIds))
        .thenReturn(tradeItems);

    when(lotRepository.search(
        tradeItems,
        lotSearchParams.getExpirationDate(),
        lotSearchParams.getLotCode(),
        lotSearchParams.getId(),
        pageable
    )).thenReturn(expected);

    Page<Lot> result = lotService.search(lotSearchParams, pageable);

    assertEquals(expected, result);
  }

  @Test
  public void searchShouldPassNullsToRepository() {
    LotSearchParams lotSearchParams = new LotSearchParams(
        LocalDate.now(),
        null,
        lot.getLotCode(),
        null
    );

    when(lotRepository.search(
        eq(emptyList()),
        eq(lotSearchParams.getExpirationDate()),
        eq(lotSearchParams.getLotCode()),
        eq(null),
        eq(pageable)
    )).thenReturn(expected);

    Page<Lot> result = lotService.search(lotSearchParams, pageable);

    assertEquals(expected, result);
  }

  @Test
  public void searchShouldReturnEmptyListIfTradeItemDoesNotExist() {
    when(tradeItemRepository.findAll(singletonList(tradeItem.getId()))).thenReturn(emptyList());

    LotSearchParams lotSearchParams = new LotSearchParams(
        LocalDate.now(),
        singletonList(tradeItem.getId()),
        lot.getLotCode(),
        ImmutableList.of(lot.getId())
    );

    Page<Lot> result = lotService.search(lotSearchParams, pageable);
    assertThat(result.getContent(), hasSize(0));
  }

  @Test(expected = NullPointerException.class)
  public void searchShouldThrowExceptionIfRequestParamsAreNotGiven() {
    lotService.search(null, pageable);
  }

}
