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

import static java.util.stream.Collectors.groupingBy;

import java.util.List;
import javax.validation.ConstraintValidatorContext;
import lombok.extern.slf4j.Slf4j;
import org.siglus.siglusapi.dto.android.constraint.stockcard.StockOnHandConsistentWithQuantityByProduct;
import org.siglus.siglusapi.dto.android.request.StockCardAdjustment;
import org.siglus.siglusapi.dto.android.request.StockCardCreateRequest;

@Slf4j
public class StockOnHandConsistentWithQuantityByProductValidator extends
    StockOnHandConsistentWithQuantityValidator<StockOnHandConsistentWithQuantityByProduct> {

  @Override
  public void initialize(StockOnHandConsistentWithQuantityByProduct constraintAnnotation) {
    // nothing to do
  }

  @Override
  protected String getGroupName() {
    return "productCode";
  }

  @Override
  public boolean isValid(List<StockCardCreateRequest> value, ConstraintValidatorContext context) {
    if (value == null || value.isEmpty()) {
      return true;
    }
    return value.stream()
        .filter(r -> r.getProductCode() != null)
        .filter(r -> r.getStockOnHand() != null)
        .filter(r -> r.getQuantity() != null)
        .filter(r -> r.getStockOnHand() >= 0)
        .filter(r -> !("PHYSICAL_INVENTORY".equals(r.getType()) && "INVENTORY".equals(r.getReasonName())))
        .sorted(StockCardAdjustment.ASCENDING)
        .collect(groupingBy(StockCardCreateRequest::getProductCode)).entrySet().stream()
        .allMatch(e -> checkConsistentByGroup(e.getKey(), e.getValue(), context));
  }

}
