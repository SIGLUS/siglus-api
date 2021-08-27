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

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

import java.util.List;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.siglus.siglusapi.dto.android.InventoryDetail;
import org.siglus.siglusapi.dto.android.Lot;
import org.siglus.siglusapi.dto.android.LotMovement;
import org.siglus.siglusapi.dto.android.ProductLotCode;
import org.siglus.siglusapi.dto.android.ProductMovement;
import org.siglus.siglusapi.dto.android.StocksOnHand;
import org.siglus.siglusapi.dto.android.response.LotBasicResponse;
import org.siglus.siglusapi.dto.android.response.LotMovementItemResponse;
import org.siglus.siglusapi.dto.android.response.LotsOnHandResponse;
import org.siglus.siglusapi.dto.android.response.ProductMovementResponse;
import org.siglus.siglusapi.dto.android.response.SiglusStockMovementItemResponse;

@Mapper(componentModel = "spring")
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public interface ProductMovementMapper {

  default List<ProductMovementResponse> toResponses(List<ProductMovement> allProductMovements,
      @Context StocksOnHand stocksOnHand) {
    return allProductMovements.stream().collect(groupingBy(ProductMovement::getProductCode)).entrySet().stream()
        .map(e -> toResponse(e.getKey(), e.getValue(), stocksOnHand))
        .collect(toList());
  }

  @Mapping(target = "productCode", source = "productCode")
  @Mapping(target = "stockMovementItems", source = "productMovements")
  @Mapping(target = "stockOnHand", source = "productCode", qualifiedByName = "getStockOnHand")
  @Mapping(target = "lotsOnHand", source = "productCode", qualifiedByName = "getLotsOnHand")
  ProductMovementResponse toResponse(String productCode, List<ProductMovement> productMovements,
      @Context StocksOnHand stocksOnHand);

  @Mapping(target = ".", source = "movementDetail")
  @Mapping(target = "movementQuantity", source = "movementDetail.adjustment")
  @Mapping(target = "stockOnHand", source = "stockQuantity")
  @Mapping(target = "requested", source = "requestedQuantity")
  @Mapping(target = "occurredDate", source = "eventTime.occurredDate")
  @Mapping(target = "processedDate", source = "eventTime.recordedAt")
  @Mapping(target = "serverProcessedDate", source = "eventTime.processedAt")
  @Mapping(target = "lotMovementItems", source = "lotMovements")
  SiglusStockMovementItemResponse fromProductMovement(ProductMovement movement);

  @Mapping(target = "quantity", source = "movementDetail.adjustment")
  @Mapping(target = "reason", source = "movementDetail.reason")
  @Mapping(target = "stockOnHand", source = "stockQuantity")
  @Mapping(target = "lotCode", source = "lot.code")
  @Mapping(target = "documentNumber", source = "documentNumber")
  LotMovementItemResponse fromLotMovement(LotMovement movement);

  @Named("getStockOnHand")
  default Integer getStockOnHand(String productCode, @Context StocksOnHand stocksOnHand) {
    return stocksOnHand.getStockQuantityByProduct(productCode);
  }


  @Named("getLot")
  default LotBasicResponse getLot(ProductLotCode productLotCode, @Context StocksOnHand stocksOnHand) {
    Lot lot = stocksOnHand.getLot(productLotCode);
    return LotBasicResponse.builder().code(lot.getCode()).expirationDate(lot.getExpirationDate()).build();
  }

  @Named("getLotsOnHand")
  default List<LotsOnHandResponse> getLotsOnHand(String productCode, @Context StocksOnHand stocksOnHand) {
    return stocksOnHand.getLotInventoriesByProduct(productCode).entrySet().stream()
        .map(e -> toLotOnHand(e.getKey(), e.getValue(), stocksOnHand))
        .sorted(comparing(r -> r.getLot().getCode()))
        .collect(toList());
  }

  @Mapping(target = "lot", source = "productLotCode", qualifiedByName = "getLot")
  @Mapping(target = "quantityOnHand", source = "inventoryDetail.stockQuantity")
  @Mapping(target = "effectiveDate", source = "inventoryDetail.eventTime.occurredDate")
  LotsOnHandResponse toLotOnHand(ProductLotCode productLotCode, InventoryDetail inventoryDetail,
      @Context StocksOnHand stocksOnHand);

}
