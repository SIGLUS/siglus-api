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

import static java.util.Comparator.naturalOrder;
import static java.util.Comparator.nullsLast;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.reducing;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import org.siglus.common.constant.KitConstants;

public class StocksOnHand {

  private final Map<ProductLotCode, InventoryDetail> lotInventories;
  private final Map<ProductLotCode, InventoryDetail> noStockInventories;
  private final Map<ProductLotCode, InventoryDetail> kitInventories;
  private final List<ProductLotCode> allLotCodes;
  private final List<String> allProductCodes;
  private final Map<ProductLotCode, Lot> lots;
  private final Map<String, String> productCodeToName;

  public StocksOnHand(List<ProductLotStock> allLotStocks) {
    this.allLotCodes = allLotStocks.stream()
        .map(ProductLotStock::getCode)
        .filter(productLotCode -> !productLotCode.isNoStock())
        .collect(toList());
    productCodeToName = allLotStocks.stream()
        .collect(
            groupingBy(s -> s.getCode().getProductCode(),
                reducing(null, ProductLotStock::getProductName, (s1, s2) -> s2)));
    this.allProductCodes = allLotStocks.stream()
        .map(ProductLotStock::getCode)
        .map(ProductLotCode::getProductCode)
        .distinct()
        .collect(toList());
    List<ProductLotStock> lotStocks = allLotStocks.stream().filter(s -> s.getCode().isLot()).collect(toList());
    this.lots = lotStocks.stream().collect(toMap(ProductLotStock::getCode, ProductLotStock::getLot));
    List<ProductLotStock> noStocks = allLotStocks.stream()
        .filter(s -> !s.getCode().isLot() && !KitConstants.isKit(s.getCode().getProductCode()))
        .collect(toList());
    this.lotInventories = lotStocks.stream()
        .collect(toMap(ProductLotStock::getCode, ProductLotStock::getInventoryDetail));
    for (Iterator<ProductLotStock> iterator = noStocks.iterator(); iterator.hasNext(); ) {
      ProductLotStock noStock = iterator.next();
      String productCode = noStock.getCode().getProductCode();
      EventTime noStockTime = noStock.getInventoryDetail().getEventTime();
      List<ProductLotStock> matchedLots = lotStocks.stream()
          .filter(s -> productCode.equals(s.getCode().getProductCode())).collect(toList());
      matchedLots.stream()
          .filter(s -> s.getInventoryDetail().getEventTime().compareTo(noStockTime) >= 0)
          .forEach(s -> lotInventories.put(s.getCode(), s.getInventoryDetail()));
      if (matchedLots.stream().allMatch(s -> s.getInventoryDetail().getEventTime().compareTo(noStockTime) < 0)) {
        matchedLots.stream()
            .forEach(s -> lotInventories.remove(s.getCode()));
      } else {
        iterator.remove();
        matchedLots.stream()
            .filter(s -> s.getInventoryDetail().getEventTime().compareTo(noStockTime) < 0)
            .forEach(s -> lotInventories.put(s.getCode(), noStock.getInventoryDetail()));
      }
    }

    this.noStockInventories = noStocks.stream()
        .collect(toMap(ProductLotStock::getCode, ProductLotStock::getInventoryDetail));
    kitInventories = allLotStocks.stream().filter(s -> KitConstants.isKit(s.getCode().getProductCode()))
        .collect(toMap(ProductLotStock::getCode, ProductLotStock::getInventoryDetail));
  }

  public InventoryDetail findInventory(ProductLotCode code) {
    if (kitInventories.containsKey(code)) {
      return kitInventories.get(code);
    }
    if (lotInventories.containsKey(code)) {
      return lotInventories.get(code);
    }
    return noStockInventories.getOrDefault(ProductLotCode.of(code.getProductCode(), null), InventoryDetail.NO_RECORD);
  }

  public Map<String, Integer> getProductInventories() {
    return allProductCodes.stream().collect(toMap(Function.identity(), this::getStockQuantityByProduct));
  }

  public Collection<String> getAllProductCodes() {
    return Collections.unmodifiableList(allProductCodes);
  }

  public Map<ProductLotCode, Integer> getLotInventories() {
    return allLotCodes.stream().collect(toMap(Function.identity(), c -> findInventory(c).getStockQuantity()));
  }

  public Map<ProductLotCode, InventoryDetail> getLotInventoriesByProduct(String productCode) {
    return allLotCodes.stream()
        .filter(c -> c.getProductCode().equals(productCode))
        .collect(toMap(Function.identity(), this::findInventory));
  }

  public Integer getStockQuantityByProduct(String productCode) {
    return allLotCodes.stream()
        .filter(c -> c.getProductCode().equals(productCode))
        .map(this::findInventory)
        .mapToInt(InventoryDetail::getStockQuantity)
        .sum();
  }

  public Lot getLot(ProductLotCode productLotCode) {
    return lots.get(productLotCode);
  }

  public LocalDate getTheEarliestDate(@Nonnull LocalDate incoming) {
    requireNonNull(incoming);
    LocalDate fromKit = kitInventories.values().stream().map(InventoryDetail::getEventTime)
        .map(EventTime::getOccurredDate).min(naturalOrder()).orElse(null);
    LocalDate fromNoStock = noStockInventories.values().stream().map(InventoryDetail::getEventTime)
        .map(EventTime::getOccurredDate).min(naturalOrder()).orElse(null);
    LocalDate fromLot = lotInventories.values().stream().map(InventoryDetail::getEventTime)
        .map(EventTime::getOccurredDate).min(naturalOrder()).orElse(null);
    return Stream.of(fromKit, fromNoStock, fromLot, incoming).min(nullsLast(naturalOrder()))
        .orElseThrow(IllegalStateException::new);
  }

  public String getProductName(String productCode) {
    return productCodeToName.get(productCode);
  }

}
