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

import static java.util.Collections.emptyList;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.siglus.siglusapi.dto.android.InventoryDetail;
import org.siglus.siglusapi.dto.android.Lot;
import org.siglus.siglusapi.dto.android.LotMovement;
import org.siglus.siglusapi.dto.android.MovementDetail;
import org.siglus.siglusapi.dto.android.PeriodOfProductMovements;
import org.siglus.siglusapi.dto.android.ProductLotCode;
import org.siglus.siglusapi.dto.android.ProductMovement;
import org.siglus.siglusapi.dto.android.StocksOnHand;
import org.siglus.siglusapi.dto.android.response.FacilityProductMovementsResponse;
import org.siglus.siglusapi.dto.android.response.LotBasicResponse;
import org.siglus.siglusapi.dto.android.response.LotMovementItemResponse;
import org.siglus.siglusapi.dto.android.response.LotsOnHandResponse;
import org.siglus.siglusapi.dto.android.response.ProductMovementResponse;
import org.siglus.siglusapi.dto.android.response.SiglusStockMovementItemResponse;

@Mapper(componentModel = "spring")
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public interface ProductMovementMapper {

  default FacilityProductMovementsResponse toAndroidResponse(PeriodOfProductMovements period) {
    Map<String, List<ProductMovement>> movementsByProductCode = period.getProductMovements().stream()
        .collect(groupingBy(ProductMovement::getProductCode));
    StocksOnHand stocksOnHand = period.getStocksOnHand();
    List<ProductMovementResponse> responses = stocksOnHand.getAllProductCodes().stream()
        .map(c -> toResponse(c, movementsByProductCode.getOrDefault(c, emptyList()), stocksOnHand,
            m -> m.getType().getReason(m.getReason(), m.getAdjustment())))
        .collect(toList());
    return FacilityProductMovementsResponse.builder().productMovements(responses).build();
  }

  default List<ProductMovementResponse> toResponses(PeriodOfProductMovements period) {
    Map<String, List<ProductMovement>> movementsByProductCode = period.getProductMovements().stream()
        .collect(groupingBy(ProductMovement::getProductCode));
    StocksOnHand stocksOnHand = period.getStocksOnHand();
    return stocksOnHand.getAllProductCodes().stream()
        .map(c -> toResponse(c, movementsByProductCode.getOrDefault(c, emptyList()), stocksOnHand,
            MovementDetail::getReason))
        .collect(toList());
  }

  @Mapping(target = "productCode", source = "productCode")
  @Mapping(target = "stockMovementItems", source = "productMovements")
  @Mapping(target = "stockOnHand", source = "productCode", qualifiedByName = "getStockOnHand")
  @Mapping(target = "lotsOnHand", source = "productCode", qualifiedByName = "getLotsOnHand")
  ProductMovementResponse toResponse(String productCode, List<ProductMovement> productMovements,
      @Context StocksOnHand stocksOnHand, @Context Function<MovementDetail, String> reasonMapper);

  @Mapping(target = ".", source = "movementDetail")
  @Mapping(target = "reason", source = "movementDetail", qualifiedByName = "getReason")
  @Mapping(target = "movementQuantity", source = "movementDetail.adjustment")
  @Mapping(target = "stockOnHand", source = "stockQuantity")
  @Mapping(target = "requested", source = "requestedQuantity")
  @Mapping(target = "occurredDate", source = "eventTime.occurredDate")
  @Mapping(target = "processedDate", source = "eventTime.recordedAt")
  @Mapping(target = "serverProcessedDate", source = "processedAt")
  @Mapping(target = "lotMovementItems", source = "lotMovements")
  SiglusStockMovementItemResponse fromProductMovement(ProductMovement movement,
      @Context Function<MovementDetail, String> reasonMapper);

  @Mapping(target = "quantity", source = "movementDetail.adjustment")
  @Mapping(target = "reason", source = "movementDetail", qualifiedByName = "getReason")
  @Mapping(target = "stockOnHand", source = "stockQuantity")
  @Mapping(target = "lotCode", source = "lot.code")
  @Mapping(target = "documentNumber", source = "documentNumber")
  LotMovementItemResponse fromLotMovement(LotMovement movement, @Context Function<MovementDetail, String> reasonMapper);

  @Named("getReason")
  default String getReason(MovementDetail movementDetail, @Context Function<MovementDetail, String> reasonMapper) {
    return reasonMapper.apply(movementDetail);
  }

  @Named("getStockOnHand")
  default Integer getStockOnHand(String productCode, @Context StocksOnHand stocksOnHand) {
    return stocksOnHand.getStockQuantityByProduct(productCode);
  }


  @Named("getLot")
  default LotBasicResponse getLot(ProductLotCode productLotCode, @Context StocksOnHand stocksOnHand) {
    Lot lot = stocksOnHand.getLot(productLotCode);
    if (lot == null) {
      return null;
    }
    return LotBasicResponse.builder().code(lot.getCode()).expirationDate(lot.getExpirationDate()).build();
  }

  @Named("getLotsOnHand")
  default List<LotsOnHandResponse> getLotsOnHand(String productCode, @Context StocksOnHand stocksOnHand) {
    return stocksOnHand.getLotInventoriesByProduct(productCode).entrySet().stream()
        .filter(e -> !e.getKey().isNoStock())
        .map(e -> toLotOnHand(e.getKey(), e.getValue(), stocksOnHand))
        .filter(s -> s.getLot() != null)
        .sorted(comparing(r -> r.getLot().getCode()))
        .collect(toList());
  }

  @Mapping(target = "lot", source = "productLotCode", qualifiedByName = "getLot")
  @Mapping(target = "quantityOnHand", source = "inventoryDetail.stockQuantity")
  @Mapping(target = "effectiveDate", source = "inventoryDetail.eventTime.occurredDate")
  LotsOnHandResponse toLotOnHand(ProductLotCode productLotCode, InventoryDetail inventoryDetail,
      @Context StocksOnHand stocksOnHand);

}
