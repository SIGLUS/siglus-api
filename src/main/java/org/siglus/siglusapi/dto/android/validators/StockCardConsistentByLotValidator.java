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

package org.siglus.siglusapi.dto.android.validators;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext;
import org.siglus.siglusapi.dto.android.constraints.StockCardConsistentByLot;
import org.siglus.siglusapi.dto.android.request.StockCardCreateRequest;

@Slf4j
public class StockCardConsistentByLotValidator implements
    ConstraintValidator<StockCardConsistentByLot, List<StockCardCreateRequest>> {

  @Override
  public void initialize(StockCardConsistentByLot constraintAnnotation) {
    // nothing to do
  }

  @Data
  @AllArgsConstructor
  private static class Entity {

    private String productCode;
    private LocalDate occurredDate;
    private String lotNumber;
    private Integer quantity;
    private Integer stockOnHand;
  }

  @Override
  public boolean isValid(List<StockCardCreateRequest> value, ConstraintValidatorContext context) {
    if (value == null || value.isEmpty()) {
      return true;
    }
    return value.stream()
        .filter(r -> isNotEmpty(r.getLotEvents()))
        .map(r -> r.getLotEvents().stream()
            .filter(lot -> lot.getQuantity() != null)
            .filter(lot -> lot.getStockOnHand() != null)
            .map(
                lot -> new Entity(r.getProductCode(), r.getOccurredDate(), lot.getLotNumber(),
                    lot.getQuantity(), lot.getStockOnHand())).collect(toList()))
        .flatMap(Collection::stream)
        .sorted(Comparator.comparing(Entity::getOccurredDate))
        .collect(groupingBy(Entity::getProductCode)).entrySet().stream()
        .allMatch(e -> checkConsistent(e.getKey(), e.getValue(), context));
  }

  private boolean checkConsistent(String productCode, List<Entity> lots,
      ConstraintValidatorContext context) {
    return lots.stream()
        .collect(groupingBy(Entity::getLotNumber)).entrySet().stream()
        .allMatch(e -> checkConsistent1(productCode, e.getKey(), e.getValue(), context));
  }

  private boolean checkConsistent1(String productCode, String lotNumber, List<Entity> lots,
      ConstraintValidatorContext context) {

    HibernateConstraintValidatorContext actualContext = context
        .unwrap(HibernateConstraintValidatorContext.class);
    actualContext.addExpressionVariable("productCode", productCode);
    actualContext.addExpressionVariable("lotNumber", lotNumber);
    int initStockOnHand = lots.stream()
        .min(Comparator.comparing(Entity::getOccurredDate))
        .map(r -> r.getStockOnHand() - r.getQuantity())
        .orElse(0);
    int lastStockOnHand = lots.stream()
        .max(Comparator.comparing(Entity::getOccurredDate))
        .map(Entity::getStockOnHand)
        .orElse(0);
    int calculatedGap = lots.stream()
        .sorted(Comparator.comparing(Entity::getOccurredDate))
        .reduce(0, (gap, req) -> gap + req.getQuantity(), Integer::sum);
    if (lastStockOnHand != initStockOnHand + calculatedGap) {
      log.warn("Inconsistent lot gap on {}", productCode);
      actualContext.addExpressionVariable("failedByGap", true);
      return false;
    }
    actualContext.addExpressionVariable("failedByGap", false);
    int stock = initStockOnHand;
    for (Entity request : lots) {
      if (request.getStockOnHand() != stock + request.getQuantity()) {
        log.warn("Inconsistent stock card for lot {} on {}", lotNumber,
            request.getOccurredDate());
        actualContext.addExpressionVariable("date", request.getOccurredDate());
        return false;
      }
      stock = request.getStockOnHand();
    }
    return true;
  }

}
