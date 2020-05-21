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

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class StockData {
  private final Map<UUID, Integer> stockOnHands;
  private final Map<UUID, Integer> beginningBalances;

  public StockData() {
    this(Collections.emptyMap(), Collections.emptyMap());
  }

  public StockData(Map<UUID, Integer> stockOnHands, Map<UUID, Integer> beginningBalances) {
    this.stockOnHands = Optional.ofNullable(stockOnHands).orElse(Collections.emptyMap());
    this.beginningBalances = Optional.ofNullable(beginningBalances).orElse(Collections.emptyMap());
  }

  public Integer getStockOnHand(UUID orderableId) {
    return stockOnHands.get(orderableId);
  }

  public Integer getBeginningBalance(UUID orderableId) {
    return beginningBalances.get(orderableId);
  }

  public boolean hasDataFor(UUID orderableId) {
    return stockOnHands.get(orderableId) != null || beginningBalances.get(orderableId) != null;
  }

}
