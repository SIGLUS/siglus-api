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
import org.siglus.siglusapi.dto.android.constraint.RequisitionValidDataSection;
import org.siglus.siglusapi.dto.android.request.RequisitionCreateRequest;
import org.springframework.util.CollectionUtils;

@NoArgsConstructor
public class RequisitionValidNotNullValidator implements
    ConstraintValidator<RequisitionValidDataSection, RequisitionCreateRequest> {

  @Override
  public void initialize(RequisitionValidDataSection constraintAnnotation) {
    // do nothing
  }

  @Override
  public boolean isValid(RequisitionCreateRequest value, ConstraintValidatorContext context) {
    String programCode = value.getProgramCode();
    boolean isEmptyProducts = false;
    boolean isEmptyRegimenLineItems = false;
    boolean isEmptyRegimenSummaryLineItems = false;
    boolean isEmptyPatientLineItems = false;
    boolean isEmptyUsageInfomationLineItems = false;
    boolean isEmptyTestConsumptionLineItems = false;
    if (ProgramConstants.VIA_PROGRAM_CODE.equals(programCode)) {
      isEmptyProducts = CollectionUtils.isEmpty(value.getProducts());
    } else if (ProgramConstants.TARV_PROGRAM_CODE.equals(programCode)) {
      isEmptyProducts = CollectionUtils.isEmpty(value.getProducts());
      isEmptyRegimenLineItems = CollectionUtils.isEmpty(value.getRegimenLineItems());
      isEmptyRegimenSummaryLineItems = CollectionUtils.isEmpty(value.getRegimenSummaryLineItems());
      isEmptyPatientLineItems = CollectionUtils.isEmpty(value.getPatientLineItems());
    } else if (ProgramConstants.MALARIA_PROGRAM_CODE.equals(programCode)) {
      isEmptyUsageInfomationLineItems = CollectionUtils.isEmpty(value.getUsageInformationLineItems());
    } else if (ProgramConstants.RAPIDTEST_PROGRAM_CODE.equals(programCode)) {
      isEmptyProducts = CollectionUtils.isEmpty(value.getProducts());
      isEmptyTestConsumptionLineItems = CollectionUtils.isEmpty(value.getTestConsumptionLineItems());
    }
    HibernateConstraintValidatorContext actualContext = context.unwrap(HibernateConstraintValidatorContext.class);
    actualContext.addExpressionVariable("isEmptyProducts", isEmptyProducts);
    actualContext.addExpressionVariable("isEmptyRegimenLineItems", isEmptyRegimenLineItems);
    actualContext.addExpressionVariable("isEmptyRegimenSummaryLineItems", isEmptyRegimenSummaryLineItems);
    actualContext.addExpressionVariable("isEmptyPatientLineItems", isEmptyPatientLineItems);
    actualContext.addExpressionVariable("isEmptyTestConsumptionLineItems", isEmptyTestConsumptionLineItems);
    actualContext.addExpressionVariable("isEmptyUsageInfomationLineItems", isEmptyUsageInfomationLineItems);
    return !isEmptyProducts && !isEmptyRegimenLineItems && !isEmptyRegimenSummaryLineItems
        && !isEmptyPatientLineItems && !isEmptyTestConsumptionLineItems && !isEmptyUsageInfomationLineItems;
  }
}
