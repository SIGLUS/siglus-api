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

import java.util.UUID;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext;
import org.siglus.common.util.SiglusAuthenticationHelper;
import org.siglus.siglusapi.constant.ValidatorConstants;
import org.siglus.siglusapi.dto.android.constraint.stockcard.FacilityApprovedProduct;
import org.siglus.siglusapi.dto.android.request.StockCardCreateRequest;
import org.siglus.siglusapi.service.android.context.StockCardCreateContextHolder;

@Slf4j
@RequiredArgsConstructor
public class FacilityApprovedProductValidator implements
    ConstraintValidator<FacilityApprovedProduct, StockCardCreateRequest> {

  private final SiglusAuthenticationHelper authHelper;

  @Override
  public void initialize(FacilityApprovedProduct constraintAnnotation) {
    // nothing to do
  }

  @Override
  public boolean isValid(StockCardCreateRequest value, ConstraintValidatorContext context) {
    UUID homeFacilityId = authHelper.getCurrentUser().getHomeFacilityId();
    HibernateConstraintValidatorContext actualContext = context.unwrap(HibernateConstraintValidatorContext.class);
    actualContext.addExpressionVariable(ValidatorConstants.PRODUCT_CODE, value.getProductCode());
    actualContext.addExpressionVariable("facilityId", homeFacilityId);
    return StockCardCreateContextHolder.getContext().isApprovedByCurrentFacility(value.getProductCode());
  }
}
