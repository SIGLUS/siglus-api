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

package org.siglus.siglusapi.dto.android.validator;

import java.time.LocalDate;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext;
import org.siglus.siglusapi.dto.android.constraint.RequisitionValidEndDate;
import org.siglus.siglusapi.dto.android.request.RequisitionCreateRequest;

@NoArgsConstructor
public class RequisitionValidEndDateValidator implements
    ConstraintValidator<RequisitionValidEndDate, RequisitionCreateRequest> {

  @Override
  public void initialize(RequisitionValidEndDate constraintAnnotation) {
    // do nothing
  }

  @Override
  public boolean isValid(RequisitionCreateRequest value, ConstraintValidatorContext context) {
    HibernateConstraintValidatorContext actualContext = context.unwrap(HibernateConstraintValidatorContext.class);
    LocalDate actualStartDate = value.getActualStartDate();
    LocalDate actualEndDate = value.getActualEndDate();
    if (actualStartDate == null || actualEndDate == null) {
      return true;
    }
    actualContext.addExpressionVariable("startDate", actualStartDate);
    actualContext.addExpressionVariable("endDate", actualEndDate);
    return !actualEndDate.isBefore(actualStartDate);
  }

}
