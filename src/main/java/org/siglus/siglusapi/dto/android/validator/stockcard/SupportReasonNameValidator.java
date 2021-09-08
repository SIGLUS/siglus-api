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

import static org.siglus.siglusapi.service.android.context.StockCardCreateContextHolder.getContext;

import java.util.UUID;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext;
import org.siglus.siglusapi.constant.ValidatorConstants;
import org.siglus.siglusapi.dto.android.constraint.stockcard.SupportReasonName;
import org.siglus.siglusapi.dto.android.enumeration.MovementType;
import org.siglus.siglusapi.dto.android.request.StockCardCreateRequest;
import org.siglus.siglusapi.service.SiglusOrderableService;

@Slf4j
@RequiredArgsConstructor
public class SupportReasonNameValidator implements
    ConstraintValidator<SupportReasonName, StockCardCreateRequest> {

  private final SiglusOrderableService siglusOrderableService;

  @Override
  public void initialize(SupportReasonName constraintAnnotation) {
    // nothing to do
  }

  @Override
  public boolean isValid(StockCardCreateRequest value, ConstraintValidatorContext context) {
    UUID programId = getContext().getProgramId(value.getProductCode()).orElse(null);
    if (programId == null) {
      return true;
    }
    HibernateConstraintValidatorContext actualContext = context.unwrap(HibernateConstraintValidatorContext.class);
    actualContext.addExpressionVariable(ValidatorConstants.PRODUCT_CODE, value.getProductCode());
    actualContext.addExpressionVariable("type", value.getType());
    try {
      if (value.getType().equals(MovementType.PHYSICAL_INVENTORY.name())) {
        value.getLotEvents()
            .forEach(l -> {
              if ("INVENTORY".equalsIgnoreCase(l.getReasonName())) {
                return;
              }
              MovementType.PHYSICAL_INVENTORY.getInventoryReasonId(programId, l.getReasonName());
            });
      } else if (value.getType().equals(MovementType.ADJUSTMENT.name())) {
        value.getLotEvents()
            .forEach(l -> MovementType.ADJUSTMENT.getAdjustmentReasonId(programId, l.getReasonName()));
      } else if (value.getType().equals(MovementType.ISSUE.name())) {
        value.getLotEvents()
            .forEach(l -> MovementType.ISSUE.getDestinationId(programId, l.getReasonName()));
      } else if (value.getType().equals(MovementType.RECEIVE.name())) {
        value.getLotEvents()
            .forEach(l -> MovementType.RECEIVE.getSourceId(programId, l.getReasonName()));
      } else if (value.getType().equals(MovementType.UNPACK_KIT.name())) {
        value.getLotEvents()
            .forEach(l -> MovementType.UNPACK_KIT.getAdjustmentReasonId(programId, l.getReasonName()));
      } else {
        return false;
      }
    } catch (Exception e) {
      log.warn("validate support reasonName exception {}", e.getMessage() + e.getCause());
      return false;
    }
    return true;
  }
}
