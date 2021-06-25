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

package org.siglus.siglusapi.dto.android.validators.stockcard;

import static java.util.stream.Collectors.groupingBy;

import java.util.Comparator;
import java.util.List;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext;
import org.siglus.siglusapi.dto.android.constraints.stockcard.ProductStockOnHandConsistentWithQuantity;
import org.siglus.siglusapi.dto.android.request.StockCardCreateRequest;

@Slf4j
public class ProductStockOnHandConsistentWithQuantityValidator implements
    ConstraintValidator<ProductStockOnHandConsistentWithQuantity, List<StockCardCreateRequest>> {

  @Override
  public void initialize(ProductStockOnHandConsistentWithQuantity constraintAnnotation) {
    // nothing to do
  }

  @Override
  public boolean isValid(List<StockCardCreateRequest> value, ConstraintValidatorContext context) {
    if (value == null || value.isEmpty()) {
      return true;
    }
    return value.stream()
        .filter(r -> r.getProductCode() != null)
        .filter(r -> r.getStockOnHand() != null)
        .filter(r -> r.getQuantity() != null)
        .filter(r -> r.getStockOnHand() >= 0)
        .collect(groupingBy(StockCardCreateRequest::getProductCode)).entrySet().stream()
        .allMatch(e -> checkConsistent(e.getKey(), e.getValue(), context));
  }

  private boolean checkConsistent(String productCode, List<StockCardCreateRequest> requests,
      ConstraintValidatorContext context) {
    requests.sort(Comparator.comparing(StockCardCreateRequest::getOccurredDate));
    HibernateConstraintValidatorContext actualContext = context
        .unwrap(HibernateConstraintValidatorContext.class);
    actualContext.addExpressionVariable("productCode", productCode);
    int initStockOnHand = requests.stream()
        .min(Comparator.comparing(StockCardCreateRequest::getOccurredDate))
        .map(r -> r.getStockOnHand() - r.getQuantity())
        .orElse(0);
    int lastStockOnHand = requests.stream()
        .max(Comparator.comparing(StockCardCreateRequest::getOccurredDate))
        .map(StockCardCreateRequest::getStockOnHand)
        .orElse(0);
    int calculatedGap = requests.stream()
        .sorted(Comparator.comparing(StockCardCreateRequest::getOccurredDate))
        .reduce(0, (gap, req) -> gap + req.getQuantity(), Integer::sum);
    if (lastStockOnHand != initStockOnHand + calculatedGap) {
      log.warn("Inconsistent gap on {}", productCode);
      actualContext.addExpressionVariable("failedByGap", true);
      return false;
    }
    actualContext.addExpressionVariable("failedByGap", false);
    int stock = initStockOnHand;
    for (StockCardCreateRequest request : requests) {
      if (request.getStockOnHand() != stock + request.getQuantity()) {
        log.warn("Inconsistent stock card for product {} on {}", productCode,
            request.getOccurredDate());
        actualContext.addExpressionVariable("date", request.getOccurredDate());
        return false;
      }
      stock = request.getStockOnHand();
    }
    return true;
  }

}
