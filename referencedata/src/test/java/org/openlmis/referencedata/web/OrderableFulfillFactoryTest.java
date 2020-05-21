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

package org.openlmis.referencedata.web;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import static org.openlmis.referencedata.domain.Orderable.COMMODITY_TYPE;
import static org.openlmis.referencedata.domain.Orderable.TRADE_ITEM;

import com.google.common.collect.Lists;
import java.util.Collections;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.referencedata.domain.CommodityType;
import org.openlmis.referencedata.domain.Orderable;
import org.openlmis.referencedata.domain.TradeItem;
import org.openlmis.referencedata.repository.OrderableRepository;
import org.openlmis.referencedata.testbuilder.CommodityTypeDataBuilder;
import org.openlmis.referencedata.testbuilder.OrderableDataBuilder;
import org.openlmis.referencedata.testbuilder.TradeItemDataBuilder;
import org.openlmis.referencedata.util.EntityCollection;

@RunWith(MockitoJUnitRunner.class)
public class OrderableFulfillFactoryTest {

  @Mock
  private OrderableRepository orderableRepository;

  @InjectMocks
  private OrderableFulfillFactory factory;

  private CommodityType commodityType = new CommodityTypeDataBuilder().build();
  private TradeItem tradeItem = new TradeItemDataBuilder()
      .withClassification(commodityType)
      .build();

  private Orderable tradeItemOrderable = new OrderableDataBuilder()
      .withIdentifier(TRADE_ITEM, tradeItem.getId())
      .build();

  private Orderable commodityTypeOrderable = new OrderableDataBuilder()
      .withIdentifier(COMMODITY_TYPE, commodityType.getId())
      .build();

  private EntityCollection<TradeItem> tradeItems = new EntityCollection<>(
      Collections.singletonList(tradeItem));

  private EntityCollection<CommodityType> commodityTypes = new EntityCollection<>(
      Collections.singletonList(commodityType));

  @Test
  public void shouldCreateResourceForTradeItem() {
    when(orderableRepository.findAllLatestByIdentifier(
        COMMODITY_TYPE, commodityType.getId().toString()))
        .thenReturn(Lists.newArrayList(commodityTypeOrderable));

    OrderableFulfill response = factory.createFor(tradeItemOrderable, tradeItems, commodityTypes);
    assertThat(response.getCanFulfillForMe(), hasSize(0));
    assertThat(response.getCanBeFulfilledByMe(), hasSize(1));
    assertThat(response.getCanBeFulfilledByMe(), hasItem(commodityTypeOrderable.getId()));
  }

  @Test
  public void shouldCreateResourceForCommodityType() {
    when(orderableRepository.findAllLatestByIdentifier(TRADE_ITEM, tradeItem.getId().toString()))
        .thenReturn(Lists.newArrayList(tradeItemOrderable));

    OrderableFulfill response = factory.createFor(commodityTypeOrderable,
        tradeItems, commodityTypes);
    assertThat(response.getCanFulfillForMe(), hasSize(1));
    assertThat(response.getCanFulfillForMe(), hasItem(tradeItemOrderable.getId()));
    assertThat(response.getCanBeFulfilledByMe(), hasSize(0));
  }

  @Test
  public void shouldNotCreateResourceIfThereAreNoIdentifiers() {
    OrderableFulfill response = factory.createFor(new OrderableDataBuilder().build(),
        tradeItems, commodityTypes);
    assertThat(response, is(nullValue()));
  }
}
