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
import org.siglus.siglusapi.dto.android.constraint.stockcard.PositiveInitStockOnHand;
import org.siglus.siglusapi.dto.android.request.StockCardAdjustment;

@Slf4j
public class PositiveInitStockOnHandValidator implements
    ConstraintValidator<PositiveInitStockOnHand, StockCardAdjustment> {

  @Override
  public void initialize(PositiveInitStockOnHand constraintAnnotation) {
    // nothing to do
  }

  @Override
  public boolean isValid(StockCardAdjustment value, ConstraintValidatorContext context) {
    if (value == null || value.getStockOnHand() == null || value.getStockOnHand() < 0
        || value.getQuantity() == null) {
      return true;
    }
    HibernateConstraintValidatorContext actualContext = context
        .unwrap(HibernateConstraintValidatorContext.class);
    actualContext.addExpressionVariable("stockOnHand", value.getStockOnHand());
    actualContext.addExpressionVariable("quantity", value.getQuantity());
    return value.getStockOnHand() - value.getQuantity() >= 0;
  }

}
