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

import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.siglus.siglusapi.service.android.context.StockCardCreateContextHolder.getContext;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext;
import org.openlmis.requisition.dto.OrderableDto;
import org.siglus.common.constant.KitConstants;
import org.siglus.siglusapi.dto.android.constraint.stockcard.KitProductEmptyLots;
import org.siglus.siglusapi.dto.android.request.StockCardCreateRequest;

@Slf4j
@RequiredArgsConstructor
public class KitProductEmptyLotsValidator implements
    ConstraintValidator<KitProductEmptyLots, StockCardCreateRequest> {

  @Override
  public void initialize(KitProductEmptyLots constraintAnnotation) {
    // nothing to do
  }

  @Override
  public boolean isValid(StockCardCreateRequest value, ConstraintValidatorContext context) {
    // this validator is supposed to running after the default group, so the value will not be null or empty
    OrderableDto product = getContext().getProduct(value.getProductCode());
    HibernateConstraintValidatorContext actualContext = context.unwrap(HibernateConstraintValidatorContext.class);
    actualContext.addExpressionVariable("productCode", value.getProductCode());
    if (product == null || !KitConstants.isKit(value.getProductCode())) {
      return true;
    }
    actualContext.addExpressionVariable("existed", "true");
    return isEmpty(value.getLotEvents());
  }

}
