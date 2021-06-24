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
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext;
import org.siglus.siglusapi.dto.android.constraints.StockCardConsistentByLot;
import org.siglus.siglusapi.dto.android.request.StockCardCreateRequest;
import org.siglus.siglusapi.dto.android.request.StockCardLotEventRequest;

@Slf4j
public class StockCardConsistentByLotValidator implements
    ConstraintValidator<StockCardConsistentByLot, List<StockCardCreateRequest>> {

  @Override
  public void initialize(StockCardConsistentByLot constraintAnnotation) {
    // nothing to do
  }

  @Override
  public boolean isValid(List<StockCardCreateRequest> value, ConstraintValidatorContext context) {
    if (value == null || value.isEmpty()) {
      return true;
    }
    HibernateConstraintValidatorContext actualContext = context
        .unwrap(HibernateConstraintValidatorContext.class);
    return value.stream()
        .filter(r -> isNotEmpty(r.getLotEvents()))
        .map(this::toLotEvents)
        .flatMap(Collection::stream)
        .sorted(Comparator.comparing(LotEvent::getOccurredDate))
        .collect(groupingBy(LotEvent::getProductCode)).entrySet().stream()
        .allMatch(e -> checkConsistentByProduct(e.getKey(), e.getValue(), actualContext));
  }

  private List<LotEvent> toLotEvents(StockCardCreateRequest product) {
    return product.getLotEvents().stream()
        .filter(lot -> lot.getQuantity() != null)
        .filter(lot -> lot.getStockOnHand() != null)
        .map(lot -> new LotEvent(product, lot))
        .collect(toList());
  }

  private boolean checkConsistentByProduct(String productCode, List<LotEvent> lots,
      HibernateConstraintValidatorContext context) {
    context.addExpressionVariable("productCode", productCode);
    return lots.stream()
        .collect(groupingBy(LotEvent::getLotNumber)).entrySet().stream()
        .allMatch(e -> checkConsistentByLot(e.getKey(), e.getValue(), context));
  }

  private boolean checkConsistentByLot(String lotNumber, List<LotEvent> lots,
      HibernateConstraintValidatorContext context) {
    context.addExpressionVariable("lotNumber", lotNumber);
    int initStockOnHand = lots.stream()
        .min(Comparator.comparing(LotEvent::getOccurredDate))
        .map(r -> r.getStockOnHand() - r.getQuantity())
        .orElse(0);
    int lastStockOnHand = lots.stream()
        .max(Comparator.comparing(LotEvent::getOccurredDate))
        .map(LotEvent::getStockOnHand)
        .orElse(0);
    int calculatedGap = lots.stream()
        .sorted(Comparator.comparing(LotEvent::getOccurredDate))
        .reduce(0, (gap, req) -> gap + req.getQuantity(), Integer::sum);
    if (lastStockOnHand != initStockOnHand + calculatedGap) {
      log.warn("Inconsistent lot by gap on {}", lotNumber);
      context.addExpressionVariable("failedByGap", true);
      return false;
    }
    context.addExpressionVariable("failedByGap", false);
    int stock = initStockOnHand;
    for (LotEvent request : lots) {
      if (request.getStockOnHand() != stock + request.getQuantity()) {
        log.warn("Inconsistent lot {} on {}", lotNumber,
            request.getOccurredDate());
        context.addExpressionVariable("date", request.getOccurredDate());
        return false;
      }
      stock = request.getStockOnHand();
    }
    return true;
  }

  @Data
  private static class LotEvent {

    private String productCode;
    private LocalDate occurredDate;
    private String lotNumber;
    private Integer quantity;
    private Integer stockOnHand;

    LotEvent(StockCardCreateRequest product, StockCardLotEventRequest lot) {
      productCode = product.getProductCode();
      occurredDate = product.getOccurredDate();
      lotNumber = lot.getLotNumber();
      quantity = lot.getQuantity();
      stockOnHand = lot.getStockOnHand();
    }

  }

}
