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

import static java.util.stream.Collectors.groupingBy;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import javax.validation.ConstraintValidatorContext;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext;
import org.siglus.siglusapi.dto.android.constraints.stockcard.StockOnHandConsistentWithQuantityByLot;
import org.siglus.siglusapi.dto.android.request.EventTime;
import org.siglus.siglusapi.dto.android.request.StockCardCreateRequest;
import org.siglus.siglusapi.dto.android.request.StockCardLotEventRequest;

@Slf4j
public class StockOnHandConsistentWithQuantityByLotValidator extends
    StockOnHandConsistentWithQuantityValidator<StockOnHandConsistentWithQuantityByLot> {

  @Override
  public boolean isValid(List<StockCardCreateRequest> value, ConstraintValidatorContext context) {
    if (value == null || value.isEmpty()) {
      return true;
    }
    HibernateConstraintValidatorContext actualContext = context.unwrap(HibernateConstraintValidatorContext.class);
    return value.stream()
        .filter(r -> isNotEmpty(r.getLotEvents()))
        .sorted(EventTime.ASCENDING)
        .collect(groupingBy(StockCardCreateRequest::getProductCode)).entrySet().stream()
        .allMatch(e -> checkConsistentByProduct(e.getKey(), e.getValue(), actualContext));
  }

  @Override
  protected String getGroupName() {
    return "lotNumber";
  }

  private boolean checkConsistentByProduct(String productCode, List<StockCardCreateRequest> lots,
      HibernateConstraintValidatorContext context) {
    context.addExpressionVariable("productCode", productCode);
    return lots.stream()
        .map(this::toEvents)
        .flatMap(Collection::stream)
        .filter(lot -> lot.getQuantity() != null)
        .filter(lot -> lot.getStockOnHand() != null)
        .collect(groupingBy(StockCardLotEventRequest::getLotNumber)).entrySet().stream()
        .allMatch(e -> checkConsistentByGroup(e.getKey(), e.getValue(), context));
  }

  private List<StockCardLotEventRequest> toEvents(StockCardCreateRequest request) {
    return request.getLotEvents().stream()
        .map(lot -> lot.setOccurredDate(request.getOccurredDate()))
        .map(lot -> lot.setCreatedAt(request.getCreatedAt()))
        .collect(Collectors.toList());
  }

}
