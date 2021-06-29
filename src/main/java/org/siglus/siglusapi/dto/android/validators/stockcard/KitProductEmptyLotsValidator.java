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

import static org.apache.commons.collections.CollectionUtils.isEmpty;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext;
import org.siglus.common.dto.referencedata.OrderableDto;
import org.siglus.siglusapi.dto.android.constraints.stockcard.KitProductEmptyLots;
import org.siglus.siglusapi.dto.android.request.StockCardCreateRequest;
import org.siglus.siglusapi.service.SiglusOrderableService;

@Slf4j
@RequiredArgsConstructor
public class KitProductEmptyLotsValidator implements
    ConstraintValidator<KitProductEmptyLots, StockCardCreateRequest> {

  private final SiglusOrderableService orderableService;

  @Override
  public void initialize(KitProductEmptyLots constraintAnnotation) {
    // nothing to do
  }

  @Override
  public boolean isValid(StockCardCreateRequest value, ConstraintValidatorContext context) {
    // this validator is supposed to running after the default group, so the value will not be null or empty
    OrderableDto product = orderableService.getOrderableByCode(value.getProductCode());
    if (!product.getIsKit()) {
      return true;
    }
    HibernateConstraintValidatorContext actualContext = context
        .unwrap(HibernateConstraintValidatorContext.class);
    actualContext.addExpressionVariable("productCode", value.getProductCode());
    return isEmpty(value.getLotEvents());
  }

}
