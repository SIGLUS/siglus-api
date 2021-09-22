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

package org.siglus.siglusapi.service.android.mapper;

import static java.util.Comparator.naturalOrder;
import static java.util.stream.Collectors.toList;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.siglus.siglusapi.dto.android.EventTime;
import org.siglus.siglusapi.dto.android.InventoryDetail;
import org.siglus.siglusapi.dto.android.Lot;
import org.siglus.siglusapi.dto.android.ProductLotCode;
import org.siglus.siglusapi.dto.android.StocksOnHand;
import org.siglus.siglusapi.dto.fc.LotStockOnHandResponse;
import org.siglus.siglusapi.dto.fc.ProductStockOnHandResponse;

@Mapper(componentModel = "spring")
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public interface LotOnHandMapper {

  default List<ProductStockOnHandResponse> toResponses(StocksOnHand stocksOnHand) {
    return stocksOnHand.getAllProductCodes().stream()
        .map(productCode -> toProductResponse(productCode, stocksOnHand))
        .collect(toList());
  }

  @Mapping(target = "productCode", source = "productCode")
  @Mapping(target = "productName", source = "productCode", qualifiedByName = "getProductName")
  @Mapping(target = "stockOnHand", source = "productCode", qualifiedByName = "getStockOnHand")
  @Mapping(target = "dateOfStock", source = "productCode", qualifiedByName = "getDateOfStock")
  @Mapping(target = "lots", source = "productCode", qualifiedByName = "getLotResponses")
  ProductStockOnHandResponse toProductResponse(String productCode, @Context StocksOnHand stocksOnHand);

  @Named("getProductName")
  default String getProductName(String productCode, @Context StocksOnHand stocksOnHand) {
    return stocksOnHand.getProductName(productCode);
  }

  @Named("getStockOnHand")
  default Integer getStockOnHand(String productCode, @Context StocksOnHand stocksOnHand) {
    return stocksOnHand.getLotInventoriesByProduct(productCode).values().stream()
        .mapToInt(InventoryDetail::getStockQuantity).sum();
  }

  @Named("getDateOfStock")
  default LocalDate getDateOfStock(String productCode, @Context StocksOnHand stocksOnHand) {
    return stocksOnHand.getLotInventoriesByProduct(productCode).values().stream().map(InventoryDetail::getEventTime)
        .map(EventTime::getOccurredDate).max(naturalOrder()).orElse(null);
  }

  @Named("getLotResponses")
  default List<LotStockOnHandResponse> getLotResponses(String productCode, @Context StocksOnHand stocksOnHand) {
    Map<ProductLotCode, InventoryDetail> lots = stocksOnHand.getLotInventoriesByProduct(productCode);
    return lots.entrySet().stream().map(e -> toLotResponse(e.getKey(), e.getValue(), stocksOnHand)).collect(toList());
  }

  @Mapping(target = "lotCode", source = "code.lotCode")
  @Mapping(target = "expirationDate", source = "code", qualifiedByName = "getExpirationDate")
  @Mapping(target = "stockOnHand", source = "detail.stockQuantity")
  @Mapping(target = "dateOfStock", source = "detail.eventTime.occurredDate")
  LotStockOnHandResponse toLotResponse(ProductLotCode code, InventoryDetail detail, @Context StocksOnHand stocksOnHand);

  @Named("getExpirationDate")
  default LocalDate getExpirationDate(ProductLotCode code, @Context StocksOnHand stocksOnHand) {
    Lot lot = stocksOnHand.getLot(code);
    return lot == null ? null : lot.getExpirationDate();
  }

}
