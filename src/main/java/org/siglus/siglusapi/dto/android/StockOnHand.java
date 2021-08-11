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

package org.siglus.siglusapi.dto.android;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import javax.annotation.Nullable;
import org.siglus.common.constant.KitConstants;

public class StockOnHand {

  private final Map<ProductLotCode, ProductLotStock> lotStockMap;

  private final Map<ProductLotCode, ProductLotStock> noStockMap;

  private final Map<ProductLotCode, ProductLotStock> kitStockMap;

  public StockOnHand(List<ProductLotStock> allLotStocks) {
    List<ProductLotStock> lotStocks = allLotStocks.stream().filter(s -> s.getCode().getLotCode() != null)
        .collect(toList());
    List<ProductLotStock> noStocks = allLotStocks.stream()
        .filter(s -> s.getCode().getLotCode() == null && !KitConstants.isKit(s.getCode().getProductCode()))
        .collect(toList());
    for (Iterator<ProductLotStock> iterator = noStocks.iterator(); iterator.hasNext(); ) {
      ProductLotStock noStock = iterator.next();
      List<ProductLotStock> matchedLots = lotStocks.stream()
          .filter(s -> noStock.getCode().getProductCode().equals(s.getCode().getProductCode())).collect(toList());
      if (matchedLots.stream().allMatch(s -> s.getEventTime().compareTo(noStock.getEventTime()) < 0)) {
        allLotStocks.removeIf(matchedLots::contains);
      } else {
        iterator.remove();
        matchedLots.stream()
            .filter(s -> s.getEventTime().compareTo(noStock.getEventTime()) < 0)
            .forEach(s -> s.setEventTime(noStock.getEventTime()));
      }
    }
    this.noStockMap = noStocks.stream().collect(toMap(ProductLotStock::getCode, Function.identity()));
    this.lotStockMap = lotStocks.stream().collect(toMap(ProductLotStock::getCode, Function.identity()));
    kitStockMap = allLotStocks.stream()
        .filter(s -> KitConstants.isKit(s.getCode().getProductCode()))
        .collect(toMap(ProductLotStock::getCode, Function.identity()));
  }

  @Nullable
  public ProductLotStock findInventory(ProductLotCode code) {
    if (kitStockMap.containsKey(code)) {
      return kitStockMap.get(code);
    }
    if (lotStockMap.containsKey(code)) {
      return lotStockMap.get(code);
    }
    return noStockMap.get(ProductLotCode.of(code.getProductCode(), null));
  }

}
