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

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext;
import org.siglus.siglusapi.dto.android.constraints.stockcard.ProductConsistentWithOwnLots;
import org.siglus.siglusapi.dto.android.request.StockCardCreateRequest;
import org.siglus.siglusapi.dto.android.request.StockCardLotEventRequest;

@Slf4j
public class ProductConsistentWithOwnLotsValidator implements
    ConstraintValidator<ProductConsistentWithOwnLots, StockCardCreateRequest> {

  @Override
  public void initialize(ProductConsistentWithOwnLots constraintAnnotation) {
    // nothing to do
  }

  @Override
  public boolean isValid(StockCardCreateRequest value, ConstraintValidatorContext context) {
    if (value == null
        || value.getStockOnHand() == null || value.getStockOnHand() < 0
        || value.getQuantity() == null
        || value.getLotEvents() == null || value.getLotEvents().isEmpty()) {
      return true;
    }
    if (value.getLotEvents().stream()
        .allMatch(r -> r.getStockOnHand() == null && r.getQuantity() == null)) {
      return true;
    }
    int sohSum = value.getLotEvents().stream()
        .filter(r -> r.getStockOnHand() != null)
        .mapToInt(StockCardLotEventRequest::getStockOnHand)
        .sum();
    int quantitySum = value.getLotEvents().stream()
        .filter(r -> r.getQuantity() != null)
        .mapToInt(StockCardLotEventRequest::getQuantity)
        .sum();
    HibernateConstraintValidatorContext actualContext = context
        .unwrap(HibernateConstraintValidatorContext.class);
    actualContext.addExpressionVariable("productCode", value.getProductCode());
    actualContext.addExpressionVariable("date", value.getOccurredDate());
    actualContext.addExpressionVariable("createdAt", value.getCreatedAt());
    return value.getStockOnHand() >= sohSum && value.getQuantity() == quantitySum;
  }

}
