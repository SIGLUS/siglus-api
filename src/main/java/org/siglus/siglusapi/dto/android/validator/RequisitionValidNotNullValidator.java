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

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext;
import org.siglus.siglusapi.constant.ProgramConstants;
import org.siglus.siglusapi.dto.android.constraint.RequisitionValidNotNull;
import org.siglus.siglusapi.dto.android.request.RequisitionCreateRequest;
import org.springframework.util.CollectionUtils;

@NoArgsConstructor
public class RequisitionValidNotNullValidator implements
    ConstraintValidator<RequisitionValidNotNull, RequisitionCreateRequest> {

  @Override
  public void initialize(RequisitionValidNotNull constraintAnnotation) {
    // do nothing
  }

  @Override
  public boolean isValid(RequisitionCreateRequest value, ConstraintValidatorContext context) {
    String programCode = value.getProgramCode();
    boolean nullProducts = false;
    if (ProgramConstants.VIA_PROGRAM_NAME.equals(programCode)) {
      nullProducts = CollectionUtils.isEmpty(value.getProducts());
    }
    boolean nullRegimenLineItems = false;
    boolean nullRegimenSummaryLineItems = false;
    boolean nullPatientLineItems = false;
    if (ProgramConstants.MMIA_PROGRAM_NAME.equals(programCode)) {
      nullProducts = CollectionUtils.isEmpty(value.getProducts());
      nullRegimenLineItems = CollectionUtils.isEmpty(value.getRegimenLineItems());
      nullRegimenSummaryLineItems = CollectionUtils.isEmpty(value.getRegimenSummaryLineItems());
      nullPatientLineItems = CollectionUtils.isEmpty(value.getPatientLineItems());
    }
    boolean nullUsageInfomationLineItems = false;
    if (ProgramConstants.MALARIA_PROGRAM_NAME.equals(programCode)) {
      nullUsageInfomationLineItems = CollectionUtils.isEmpty(value.getUsageInformationLineItems());
    }
    boolean nullTestConsumptionLineItems = false;
    if (ProgramConstants.RAPIDTEST_PROGRAM_NAME.equals(programCode)) {
      nullProducts = CollectionUtils.isEmpty(value.getProducts());
      nullTestConsumptionLineItems = CollectionUtils.isEmpty(value.getTestConsumptionLineItems());
    }
    HibernateConstraintValidatorContext actualContext = context.unwrap(HibernateConstraintValidatorContext.class);
    actualContext.addExpressionVariable("nullProducts", nullProducts);
    actualContext.addExpressionVariable("nullRegimenLineItems", nullRegimenLineItems);
    actualContext.addExpressionVariable("nullRegimenSummaryLineItems", nullRegimenSummaryLineItems);
    actualContext.addExpressionVariable("nullPatientLineItems", nullPatientLineItems);
    actualContext.addExpressionVariable("nullTestConsumptionLineItems", nullTestConsumptionLineItems);
    actualContext.addExpressionVariable("nullUsageInfomationLineItems", nullUsageInfomationLineItems);
    return !nullProducts && !nullRegimenLineItems && !nullRegimenSummaryLineItems
        && !nullPatientLineItems && !nullTestConsumptionLineItems && !nullUsageInfomationLineItems;
  }
}
