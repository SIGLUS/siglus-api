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

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext;
import org.siglus.siglusapi.constant.ValidatorConstants;
import org.siglus.siglusapi.dto.android.constraint.stockcard.ProductPositiveInitStockOnHand;
import org.siglus.siglusapi.dto.android.request.StockCardCreateRequest;

@Slf4j
public class ProductPositiveInitStockOnHandValidator implements
    ConstraintValidator<ProductPositiveInitStockOnHand, StockCardCreateRequest> {

  @Override
  public void initialize(ProductPositiveInitStockOnHand constraintAnnotation) {
    // nothing to do
  }

  @Override
  public boolean isValid(StockCardCreateRequest value, ConstraintValidatorContext context) {
    if (value == null || value.getStockOnHand() == null || value.getStockOnHand() < 0
        || value.getQuantity() == null) {
      return true;
    }
    HibernateConstraintValidatorContext actualContext = context
        .unwrap(HibernateConstraintValidatorContext.class);
    actualContext.addExpressionVariable("isIssue", "issue".equalsIgnoreCase(value.getType()) ? "true" : "false");
    actualContext.addExpressionVariable(ValidatorConstants.PRODUCT_CODE, value.getProductCode());
    return value.getStockOnHand() - value.getQuantity() >= 0;
  }
}
