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

package org.siglus.siglusapi.service.android.context;

import static java.util.Objects.requireNonNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.siglus.siglusapi.dto.android.Lot;
import org.siglus.siglusapi.dto.android.ProductLotCode;
import org.siglus.siglusapi.dto.android.db.ProductLot;
import org.siglus.siglusapi.repository.LotNativeRepository;
import org.siglus.siglusapi.service.LotConflictService;

@Slf4j
@RequiredArgsConstructor
public class LotContext implements Context {

  private final UUID facilityId;
  private final LotNativeRepository repo;
  private final LotConflictService lotConflictService;
  private final Map<ProductLotCode, ProductLot> existed = new HashMap<>();
  private final Map<ProductLotCode, ProductLot> newCreated = new HashMap<>();
  private final Map<String, UUID> productCodeToTradeItemId = new HashMap<>();
  private final Map<String, Set<Lot>> productCodeToLots = new HashMap<>();

  public static LotContext init(UUID facilityId, LotNativeRepository repo, LotConflictService lotConflictService) {
    return new LotContext(facilityId, repo, lotConflictService);
  }

  public <T> void preload(Collection<? extends T> collection, Function<T, String> productCodeGetter,
      Function<T, UUID> tradeItemIdGetter, Function<T, Collection<Lot>> lotsGetter) {
    collection.forEach(anyType -> {
      Collection<Lot> lots = lotsGetter.apply(anyType);
      lots.forEach(lot -> push(productCodeGetter.apply(anyType), tradeItemIdGetter.apply(anyType), lot));
    });
    load();
    create();
  }

  public ProductLot get(UUID lotId) {
    for (ProductLot productLot : existed.values()) {
      if (lotId.equals(productLot.getId())) {
        return productLot;
      }
    }
    for (ProductLot productLot : newCreated.values()) {
      if (lotId.equals(productLot.getId())) {
        return productLot;
      }
    }
    ProductLot productLot = repo.findById(lotId);
    if (productLot != null) {
      existed.put(productLot.toProductLotCode(), productLot);
      return productLot;
    }
    throw new IllegalStateException();
  }

  public ProductLot getLot(String productCode, String lotCode) {
    ProductLotCode productLotCode = ProductLotCode.of(productCode, lotCode);
    ProductLot productLot = existed.get(productLotCode);
    if (productLot != null) {
      return productLot;
    }
    productLot = newCreated.get(productLotCode);
    if (productLot == null) {
      UUID tradeItemId = productCodeToTradeItemId.get(productCode);
      productLot = repo.findOne(productCode, tradeItemId, lotCode);
      if (productLot == null) {
        throw new IllegalStateException();
      }
    }
    return productLot;
  }

  private void push(String productCode, UUID tradeItemId, Lot lot) {
    // should we put the trade item id only once?
    productCodeToTradeItemId.put(productCode, tradeItemId);
    productCodeToLots.putIfAbsent(productCode, new HashSet<>());
    boolean firstAdd = productCodeToLots.get(productCode).add(lot);
    if (firstAdd && !log.isDebugEnabled()) {
      log.debug("load " + lot + " of " + productCode);
    }
  }

  private void load() {
    productCodeToLots.entrySet().stream()
        .flatMap(e -> {
          String productCode = e.getKey();
          UUID tradeItemId = productCodeToTradeItemId.get(productCode);
          return e.getValue().stream().map(lot -> repo.findOne(productCode, tradeItemId, lot.getCode()));
        })
        .filter(Objects::nonNull)
        .forEach(productLot -> existed.put(productLot.toProductLotCode(), productLot));
    // handle conflict
    productCodeToLots.forEach(
        (productCode, lots) -> lots.stream()
            .filter(lot -> existed.containsKey(ProductLotCode.of(productCode, lot.getCode())))
            .forEach(lot -> {
              String lotCode = lot.getCode();
              ProductLot existedLot = existed.get(ProductLotCode.of(productCode, lot.getCode()));
              lotConflictService.handleLotConflict(facilityId, lotCode, existedLot.getId(), lot.getExpirationDate(),
                  requireNonNull(existedLot.getExpirationDate()));
            })
    );
  }

  private void create() {
    productCodeToLots.entrySet().stream()
        .flatMap(e -> {
          String productCode = e.getKey();
          UUID tradeItemId = productCodeToTradeItemId.get(productCode);
          return e.getValue().stream().map(lot -> ProductLot.of(productCode, tradeItemId, lot));
        })
        .filter(productLot -> !existed.containsKey(productLot.toProductLotCode()))
        .forEach(productLot -> newCreated.put(productLot.toProductLotCode(), productLot));
    repo.batchCreateLots(newCreated.values());
  }

}
