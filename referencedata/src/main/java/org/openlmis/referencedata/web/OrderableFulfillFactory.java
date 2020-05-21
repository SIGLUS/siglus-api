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

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.openlmis.referencedata.domain.Orderable.COMMODITY_TYPE;
import static org.openlmis.referencedata.domain.Orderable.TRADE_ITEM;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.UUID;
import org.openlmis.referencedata.domain.BaseEntity;
import org.openlmis.referencedata.domain.CommodityType;
import org.openlmis.referencedata.domain.Dispensable;
import org.openlmis.referencedata.domain.Orderable;
import org.openlmis.referencedata.domain.TradeItem;
import org.openlmis.referencedata.repository.OrderableRepository;
import org.openlmis.referencedata.util.EntityCollection;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;
import org.slf4j.profiler.Profiler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class OrderableFulfillFactory {

  private static final XLogger XLOGGER = XLoggerFactory.getXLogger(OrderableFulfillFactory.class);

  @Autowired
  private OrderableRepository orderableRepository;

  /**
   * Create new instance of {@link OrderableFulfill} for the given orderable.
   */
  public OrderableFulfill createFor(Orderable orderable, EntityCollection<TradeItem> tradeItems,
                                    EntityCollection<CommodityType> commodityTypes) {
    Profiler profiler = new Profiler("CREATE_ORDERABLE_FULFILL");
    profiler.setLogger(XLOGGER);
    profiler.start("GET_IDENTIFIERS");
    String tradeItemId = orderable.getTradeItemIdentifier();
    String commodityTypeId = orderable.getCommodityTypeIdentifier();

    OrderableFulfill result = null;
    if (isNotBlank(tradeItemId)) {
      result = createForTradeItem(tradeItems, commodityTypes, tradeItemId, orderable, profiler);
    } else if (isNotBlank(commodityTypeId)) {
      result = createForCommodityType(tradeItems, commodityTypes, commodityTypeId, orderable,
          profiler);
    }

    profiler.stop().log();
    return result;
  }

  private OrderableFulfill createForTradeItem(EntityCollection<TradeItem> tradeItems,
      EntityCollection<CommodityType> commodityTypes, String id,
      Orderable tradeItemOrderable, Profiler profiler) {
    profiler.start("FETCH_TRADE_ITEM");
    TradeItem tradeItem = tradeItems.getById(UUID.fromString(id));

    profiler.start("CREATE_ORDERABLE_FULFILL_FOR_TRADE_ITEM");
    List<UUID> canBeFulfilledByMe = Lists.newArrayList();
    commodityTypes.values().forEach(
        commodityType -> {
          if (tradeItem.canFulfill(commodityType)) {
            addToListIfDispensableMatches(canBeFulfilledByMe, COMMODITY_TYPE, commodityType,
                tradeItemOrderable.getDispensable());
          }
        }
    );

    return OrderableFulfill.ofTradeItem(canBeFulfilledByMe);
  }

  private OrderableFulfill createForCommodityType(EntityCollection<TradeItem> tradeItems,
      EntityCollection<CommodityType> commodityTypes, String id,
      Orderable commodityTypeOrderable, Profiler profiler) {
    profiler.start("FETCH_COMMODITY_TYPE");
    CommodityType commodityType = commodityTypes.getById(UUID.fromString(id));

    profiler.start("CREATE_ORDERABLE_FULFILL_FOR_COMMODITY_TYPE");
    List<UUID> canFulfillForMe = Lists.newArrayList();
    tradeItems.values().forEach(
        tradeItem -> {
          if (tradeItem.canFulfill(commodityType)) {
            addToListIfDispensableMatches(canFulfillForMe, TRADE_ITEM, tradeItem,
                commodityTypeOrderable.getDispensable());
          }
        }
    );

    return OrderableFulfill.ofCommodityType(canFulfillForMe);
  }

  private void addToListIfDispensableMatches(List<UUID> list, String key, BaseEntity entity,
                                             Dispensable dispensableToMatch) {
    List<Orderable> orderables = orderableRepository
        .findAllLatestByIdentifier(key, entity.getId().toString());

    orderables.forEach(
        item -> {
          if (item.hasDispensable(dispensableToMatch)) {
            list.add(item.getId());
          }
        }
    );
  }

}
