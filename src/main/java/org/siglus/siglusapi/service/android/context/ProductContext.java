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

import static java.util.stream.Collectors.toMap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.siglus.common.dto.referencedata.OrderableDto;
import org.siglus.siglusapi.service.SiglusOrderableService;

@Slf4j
@RequiredArgsConstructor
public class ProductContext implements Context {

  private final Map<UUID, OrderableDto> idToProduct = new HashMap<>();
  private final Map<String, OrderableDto> allProducts = new HashMap<>();

  public static ProductContext init(SiglusOrderableService service) {
    ProductContext context = new ProductContext();
    List<OrderableDto> products = service.getAllProducts();
    context.allProducts.putAll(products.stream().collect(toMap(OrderableDto::getProductCode, Function.identity())));
    context.idToProduct.putAll(products.stream().collect(toMap(OrderableDto::getId, Function.identity())));
    return context;
  }

  public OrderableDto getProduct(String productCode) {
    return allProducts.get(productCode);
  }

  public OrderableDto getProduct(UUID productId) {
    return idToProduct.get(productId);
  }

}
