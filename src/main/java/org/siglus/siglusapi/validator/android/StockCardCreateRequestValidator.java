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

package org.siglus.siglusapi.validator.android;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import javax.validation.executable.ExecutableValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.internal.engine.ConstraintViolationImpl;
import org.siglus.siglusapi.dto.android.InvalidProduct;
import org.siglus.siglusapi.dto.android.ValidatedStockCards;
import org.siglus.siglusapi.dto.android.request.StockCardCreateRequest;
import org.siglus.siglusapi.dto.android.sequence.PerformanceSequence;
import org.siglus.siglusapi.service.android.StockCardSyncService;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Component("StockCardCreateRequestValidator")
@RequiredArgsConstructor
@Slf4j
public class StockCardCreateRequestValidator {

  private final Validator validator;

  private final StockCardSyncService stockCardSyncService;

  public ValidatedStockCards validateStockCardCreateRequest(List<StockCardCreateRequest> requests) {
    ExecutableValidator forExecutables = validator.forExecutables();
    Method method = null;
    try {
      method = StockCardSyncService.class.getDeclaredMethod("createStockCards", List.class);
    } catch (NoSuchMethodException e) {
      log.warn(e.getMessage());
    }
    Set<ConstraintViolation<StockCardSyncService>> violations;
    List<InvalidProduct> invalidProducts = new ArrayList<>();
    List<StockCardCreateRequest> originRequest = new ArrayList<>(requests);
    do {
      violations = forExecutables.validateParameters(
          stockCardSyncService, method, new List[]{originRequest}, PerformanceSequence.class);
      invalidProducts.addAll(getInvalidProducts(violations));
      originRequest.removeIf(
          r -> invalidProducts.stream().anyMatch(i -> i.getProductCode().equals(r.getProductCode())));
    } while (!CollectionUtils.isEmpty(violations) && !originRequest.isEmpty());
    return ValidatedStockCards.builder().validStockCardRequests(originRequest).invalidProducts(invalidProducts).build();
  }

  private List<InvalidProduct> getInvalidProducts(Set<ConstraintViolation<StockCardSyncService>> violations) {
    return violations.stream().map(this::buildInvalidProductByViolation).collect(Collectors.toList());
  }

  private InvalidProduct buildInvalidProductByViolation(ConstraintViolation<StockCardSyncService> violation) {
    String violationMessage = violation.getMessage();
    log.warn(violationMessage);
    return InvalidProduct.builder().productCode(
        (String) ((ConstraintViolationImpl<StockCardSyncService>) violation).getExpressionVariables()
            .get("productCode"))
        .errorMessage(violationMessage)
        .build();
  }
}
