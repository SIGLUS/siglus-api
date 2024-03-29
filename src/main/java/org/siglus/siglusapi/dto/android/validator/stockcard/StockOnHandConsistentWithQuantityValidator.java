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

import java.lang.annotation.Annotation;
import java.util.List;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext;
import org.siglus.siglusapi.dto.android.EventTimeContainer;
import org.siglus.siglusapi.dto.android.request.StockCardAdjustment;
import org.siglus.siglusapi.dto.android.request.StockCardCreateRequest;

@Slf4j
abstract class StockOnHandConsistentWithQuantityValidator<A extends Annotation> implements
    ConstraintValidator<A, List<StockCardCreateRequest>> {

  @Override
  public void initialize(A constraintAnnotation) {
    // nothing to do
  }

  protected abstract String getGroupName();

  protected boolean checkConsistentByGroup(String groupName, List<? extends StockCardAdjustment> adjustments,
      ConstraintValidatorContext context) {
    HibernateConstraintValidatorContext actualContext = context.unwrap(HibernateConstraintValidatorContext.class);
    actualContext.addExpressionVariable(getGroupName(), groupName);
    int initStockOnHand = adjustments.stream()
        .min(EventTimeContainer.ASCENDING)
        .map(r -> r.getStockOnHand() - r.getQuantity())
        .orElse(0);
    int lastStockOnHand = adjustments.stream()
        .max(EventTimeContainer.ASCENDING)
        .map(StockCardAdjustment::getStockOnHand)
        .orElse(0);
    int calculatedGap = adjustments.stream()
        .reduce(0, (gap, req) -> gap + req.getQuantity(), Integer::sum);
    if (lastStockOnHand != initStockOnHand + calculatedGap) {
      log.warn("Inconsistent adjustment by gap on {}", groupName);
      actualContext.addExpressionVariable("failedByGap", true);
      return false;
    }
    actualContext.addExpressionVariable("failedByGap", false);
    int stock = initStockOnHand;
    for (StockCardAdjustment request : adjustments) {
      if (request.getStockOnHand() != stock + request.getQuantity()) {
        log.warn("Inconsistent adjustment {} at {}", groupName, request.getEventTime().getRecordedAt());
        actualContext.addExpressionVariable("date", request.getEventTime().getOccurredDate());
        actualContext.addExpressionVariable("createdAt", request.getEventTime().getRecordedAt());
        return false;
      }
      stock = request.getStockOnHand();
    }
    return true;
  }

}
