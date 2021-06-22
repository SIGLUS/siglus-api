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

package org.siglus.siglusapi.dto.android.validators;

import static java.util.stream.Collectors.groupingBy;

import java.util.Comparator;
import java.util.List;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext;
import org.siglus.siglusapi.dto.android.constraints.StockCardConsistentByProductCode;
import org.siglus.siglusapi.dto.android.request.StockCardCreateRequest;

public class StockCardConsistentByProductCodeValidator implements
    ConstraintValidator<StockCardConsistentByProductCode, List<StockCardCreateRequest>> {

  @Override
  public void initialize(StockCardConsistentByProductCode constraintAnnotation) {
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
    HibernateConstraintValidatorContext actualContext = context
        .unwrap(HibernateConstraintValidatorContext.class);
    actualContext.addExpressionVariable("productCode", productCode);
    int initStockOnHand = requests.stream()
        .min(Comparator.comparing(StockCardCreateRequest::getOccurred))
        .map(r -> r.getStockOnHand() - r.getQuantity())
        .orElse(0);
    if (initStockOnHand < 0) {
      return false;
    }
    int lastStockOnHand = requests.stream()
        .max(Comparator.comparing(StockCardCreateRequest::getOccurred))
        .map(StockCardCreateRequest::getStockOnHand)
        .orElse(0);
    int calculatedGap = requests.stream()
        .sorted(Comparator.comparing(StockCardCreateRequest::getOccurred))
        .reduce(0, (s, r) -> s + r.getQuantity(), Integer::sum);
    if (lastStockOnHand != initStockOnHand + calculatedGap) {
      return false;
    }
    int stock = initStockOnHand;
    for (StockCardCreateRequest request : requests) {
      if (request.getStockOnHand() != stock + request.getQuantity()) {
        return false;
      }
      stock = request.getStockOnHand();
    }
    return true;
  }

}
