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

package org.siglus.siglusapi.dto.android.validator.stockcard;

import static java.util.stream.Collectors.groupingBy;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext;
import org.siglus.siglusapi.dto.android.EventTimeContainer;
import org.siglus.siglusapi.dto.android.constraint.stockcard.ProductConsistentWithAllLots;
import org.siglus.siglusapi.dto.android.request.StockCardCreateRequest;

@Slf4j
public class ProductConsistentWithAllLotsValidator implements
    ConstraintValidator<ProductConsistentWithAllLots, List<StockCardCreateRequest>> {

  @Override
  public void initialize(ProductConsistentWithAllLots constraintAnnotation) {
    // nothing to do
  }

  @Override
  public boolean isValid(List<StockCardCreateRequest> value, ConstraintValidatorContext context) {
    if (value == null || value.isEmpty()) {
      return true;
    }
    HibernateConstraintValidatorContext actualContext = context
        .unwrap(HibernateConstraintValidatorContext.class);
    return value.stream()
        .filter(r -> r.getStockOnHand() != null)
        .filter(r -> r.getStockOnHand() >= 0)
        .filter(r -> isNotEmpty(r.getLotEvents()))
        .filter(r -> r.getLotEvents().stream().allMatch(l -> l.getQuantity() != null))
        .filter(r -> r.getLotEvents().stream().allMatch(l -> l.getStockOnHand() != null))
        .sorted(EventTimeContainer.ASCENDING)
        .collect(groupingBy(StockCardCreateRequest::getProductCode)).entrySet().stream()
        .allMatch(e -> checkConsistentByProduct(e.getKey(), e.getValue(), actualContext));
  }

  private boolean checkConsistentByProduct(String productCode,
      List<StockCardCreateRequest> requests, HibernateConstraintValidatorContext context) {
    context.addExpressionVariable("productCode", productCode);
    Map<String, Integer> sohByLotCode = new HashMap<>();
    for (StockCardCreateRequest request : requests) {
      request.getLotEvents().forEach(lot -> {
        sohByLotCode.computeIfPresent(lot.getLotCode(), (lotCode, sum) -> sum + lot.getQuantity());
        sohByLotCode.putIfAbsent(lot.getLotCode(), lot.getStockOnHand());
      });
      if (request.getStockOnHand() < sohByLotCode.values().stream().mapToInt(Integer::intValue).sum()) {
        context.addExpressionVariable("date", request.getOccurredDate());
        context.addExpressionVariable("createdAt", request.getRecordedAt());
        return false;
      }
    }
    return true;
  }


}
