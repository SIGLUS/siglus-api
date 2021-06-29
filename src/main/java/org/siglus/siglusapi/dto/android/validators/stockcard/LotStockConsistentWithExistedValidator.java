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
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static org.siglus.siglusapi.dto.android.request.EventTime.ASCENDING;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext;
import org.siglus.siglusapi.dto.android.LotStockOnHand;
import org.siglus.siglusapi.dto.android.constraints.stockcard.LotStockConsistentWithExisted;
import org.siglus.siglusapi.dto.android.request.StockCardCreateRequest;
import org.siglus.siglusapi.dto.android.request.StockCardLotEventRequest;
import org.siglus.siglusapi.service.android.SiglusMeService;

@Slf4j
@RequiredArgsConstructor
public class LotStockConsistentWithExistedValidator implements
    ConstraintValidator<LotStockConsistentWithExisted, List<StockCardCreateRequest>> {

  private final SiglusMeService service;

  @Override
  public void initialize(LotStockConsistentWithExisted constraintAnnotation) {
    // nothing to do
  }

  @Override
  public boolean isValid(List<StockCardCreateRequest> value, ConstraintValidatorContext context) {
    // this validator is supposed to running after the default group, so the value will not be null or empty
    HibernateConstraintValidatorContext actualContext = context.unwrap(HibernateConstraintValidatorContext.class);
    Map<LotStockOnHand, LotStockOnHand> fromRequests = value.stream()
        .filter(r -> isNotEmpty(r.getLotEvents()))
        .sorted(ASCENDING)
        .collect(groupingBy(StockCardCreateRequest::getProductCode)).entrySet().stream()
        .map(e -> toInitLotStocks(e.getKey(), e.getValue()))
        .flatMap(Collection::stream)
        .collect(toMap(Function.identity(), Function.identity()));
    List<LotStockOnHand> fromDbs = service.getLotStockOnHands();
    if (fromDbs.isEmpty()) {
      return true;
    }
    actualContext.addExpressionVariable("failedByNewLot", false);
    actualContext.addExpressionVariable("failedByDate", false);
    for (LotStockOnHand fromDb : fromDbs) {
      LotStockOnHand fromRequest = fromRequests.remove(fromDb);
      if (fromRequest == null) {
        continue;
      }
      actualContext.addExpressionVariable("productCode", fromRequest.getProductCode());
      actualContext.addExpressionVariable("lotCode", fromRequest.getLotCode());
      actualContext.addExpressionVariable("date", fromRequest.getOccurredDate());
      if (fromRequest.getOccurredDate().isBefore(fromDb.getOccurredDate())) {
        actualContext.addExpressionVariable("failedByDate", true);
        actualContext.addExpressionVariable("existedDate", fromDb.getOccurredDate());
        return false;
      }
      Integer stockOnHand = fromRequest.getStockOnHand();
      if (!stockOnHand.equals(fromDb.getStockOnHand())) {
        actualContext.addExpressionVariable("existedSoh", fromDb.getStockOnHand());
        actualContext.addExpressionVariable("soh", fromRequest.getStockOnHand());
        return false;
      }
    }
    for (LotStockOnHand newLot : fromRequests.values()) {
      if (newLot.getStockOnHand() != 0) {
        actualContext.addExpressionVariable("failedByNewLot", true);
        actualContext.addExpressionVariable("productCode", newLot.getProductCode());
        actualContext.addExpressionVariable("lotCode", newLot.getLotCode());
        actualContext.addExpressionVariable("date", newLot.getOccurredDate());
        actualContext.addExpressionVariable("soh", newLot.getStockOnHand());
        return false;
      }
    }
    return true;
  }

  private List<LotStockOnHand> toInitLotStocks(String productCode, List<StockCardCreateRequest> requests) {
    return requests.stream()
        .map(this::toEvents)
        .flatMap(Collection::stream)
        .filter(lot -> lot.getLotCode() != null)
        .collect(groupingBy(StockCardLotEventRequest::getLotCode)).values().stream()
        .map(lot -> lot.stream().min(ASCENDING).orElse(null))
        .filter(Objects::nonNull)
        .map(lot -> buildLotStock(productCode, lot))
        .collect(Collectors.toList());
  }

  private List<StockCardLotEventRequest> toEvents(StockCardCreateRequest request) {
    return request.getLotEvents().stream()
        .map(lot -> lot.setOccurredDate(request.getOccurredDate()))
        .map(lot -> lot.setCreatedAt(request.getCreatedAt()))
        .collect(Collectors.toList());
  }

  private LotStockOnHand buildLotStock(String productCode, StockCardLotEventRequest lot) {
    return LotStockOnHand.builder().productCode(productCode).lotCode(lot.getLotCode())
        .occurredDate(lot.getOccurredDate()).stockOnHand(lot.getStockOnHand() - lot.getQuantity()).build();
  }

}
