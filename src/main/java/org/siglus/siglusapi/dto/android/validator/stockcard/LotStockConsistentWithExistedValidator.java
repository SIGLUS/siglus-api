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

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext;
import org.siglus.siglusapi.dto.android.ProductLotCode;
import org.siglus.siglusapi.dto.android.ProductLotStock;
import org.siglus.siglusapi.dto.android.StockOnHand;
import org.siglus.siglusapi.dto.android.constraint.stockcard.LotStockConsistentWithExisted;
import org.siglus.siglusapi.dto.android.request.StockCardAdjustment;
import org.siglus.siglusapi.dto.android.request.StockCardCreateRequest;
import org.siglus.siglusapi.dto.android.request.StockCardLotEventRequest;
import org.siglus.siglusapi.service.android.StockCardSyncService;

@Slf4j
@RequiredArgsConstructor
@SuppressWarnings("PMD.TooManyMethods")
public class LotStockConsistentWithExistedValidator implements
    ConstraintValidator<LotStockConsistentWithExisted, List<StockCardCreateRequest>> {

  private final StockCardSyncService service;

  @Override
  public void initialize(LotStockConsistentWithExisted constraintAnnotation) {
    // nothing to do
  }

  @Override
  public boolean isValid(List<StockCardCreateRequest> value, ConstraintValidatorContext context) {
    // this validator is supposed to running after the default group, so the value will not be null or empty
    HibernateConstraintValidatorContext actualContext = context.unwrap(HibernateConstraintValidatorContext.class);
    StockOnHand stockOnHand = service.getLatestStockOnHand();
    Set<ProductLotCode> verified = new HashSet<>();
    value.sort(StockCardAdjustment.ASCENDING);
    for (StockCardCreateRequest request : value) {
      request.getLotEvents().sort(Comparator.comparing(StockCardLotEventRequest::getLotCode));
      for (StockCardLotEventRequest lotEvent : request.getLotEvents()) {
        ProductLotCode productLotCode = ProductLotCode.of(request.getProductCode(), lotEvent.getLotCode());
        if (verified.contains(productLotCode)) {
          continue;
        }
        actualContext.addExpressionVariable("productCode", productLotCode.getProductCode());
        actualContext.addExpressionVariable("lotCode", productLotCode.getLotCode());
        actualContext.addExpressionVariable("occurredDate", lotEvent.getEventTime().getOccurredDate());
        actualContext.addExpressionVariable("recordedAt", lotEvent.getEventTime().getRecordedAt());
        int initInventory = lotEvent.getStockOnHand() - lotEvent.getQuantity();
        actualContext.addExpressionVariable("initInventory", initInventory);
        ProductLotStock inventory = stockOnHand.findInventory(productLotCode);
        if (inventory == null) {
          if (initInventory != 0) {
            actualContext.addExpressionVariable("existedInventory", 0);
            return false;
          }
          verified.add(productLotCode);
          continue;
        }
        if (inventory.getEventTime().compareTo(lotEvent.getEventTime()) < 0) {
          if (inventory.getInventory() != initInventory) {
            actualContext.addExpressionVariable("existedInventory", inventory.getInventory());
            return false;
          }
          verified.add(productLotCode);
        }
      }
    }
    return true;
  }

}
