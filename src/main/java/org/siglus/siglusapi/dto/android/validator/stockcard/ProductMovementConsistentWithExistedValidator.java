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

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.minBy;
import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext;
import org.siglus.siglusapi.dto.android.LotMovement;
import org.siglus.siglusapi.dto.android.MovementDetail;
import org.siglus.siglusapi.dto.android.ProductMovement;
import org.siglus.siglusapi.dto.android.ProductMovementKey;
import org.siglus.siglusapi.dto.android.constraint.stockcard.ProductMovementConsistentWithExisted;
import org.siglus.siglusapi.dto.android.request.StockCardAdjustment;
import org.siglus.siglusapi.dto.android.request.StockCardCreateRequest;
import org.siglus.siglusapi.dto.android.request.StockCardLotEventRequest;
import org.siglus.siglusapi.service.android.SiglusMeService;
import org.siglus.siglusapi.service.android.SiglusMeService.MovementType;

@Slf4j
@RequiredArgsConstructor
@SuppressWarnings("PMD.TooManyMethods")
public class ProductMovementConsistentWithExistedValidator implements
    ConstraintValidator<ProductMovementConsistentWithExisted, List<StockCardCreateRequest>> {

  private static final String FAILED_BY_NOT_FOUND = "failedByNotFound";
  private static final String FAILED_BY_SAME_LOT = "failedBySameLot";
  private static final String FAILED_BY_SAME_PRODUCT = "failedBySameProduct";

  private static final String FIELD_NAME = "fieldName";
  private static final String VALUE_FROM_REQUEST = "valueFromRequest";
  private static final String VALUE_FROM_EXISTED = "valueFromExisted";
  private static final String OCCURRED_DATE = "occurredDate";
  private static final String RECORDED_AT = "recordedAt";
  private static final String INIT_INVENTORY = "initInventory";
  private static final String LOT_CODE = "lotCode";


  private final SiglusMeService service;

  @Override
  public void initialize(ProductMovementConsistentWithExisted constraintAnnotation) {
    // nothing to do
  }

  @Override
  public boolean isValid(List<StockCardCreateRequest> value, ConstraintValidatorContext context) {
    // this validator is supposed to running after the default group, so the value will not be null or empty
    HibernateConstraintValidatorContext actualContext = context.unwrap(HibernateConstraintValidatorContext.class);
    actualContext.addExpressionVariable(FAILED_BY_NOT_FOUND, false);
    actualContext.addExpressionVariable("failedByContinuity", false);
    actualContext.addExpressionVariable(FAILED_BY_SAME_LOT, false);
    actualContext.addExpressionVariable(FAILED_BY_SAME_PRODUCT, false);
    Map<String, List<ProductMovement>> existed = service.getLatestProductMovements().stream()
        .collect(groupingBy(ProductMovement::getProductCode));
    if (!validateToTheExisted(value, existed, actualContext)) {
      return false;
    }
    Set<String> existedProductCodes = existed.keySet();
    return value.stream()
        .filter(r -> !existedProductCodes.contains(r.getProductCode()))
        .collect(groupingBy(StockCardCreateRequest::getProductCode, minBy(StockCardAdjustment.ASCENDING)))
        .values().stream().map(Optional::get).allMatch(r -> validateNewProduct(r, actualContext));
  }

  private boolean validateToTheExisted(List<StockCardCreateRequest> value, Map<String, List<ProductMovement>> existed,
      HibernateConstraintValidatorContext actualContext) {
    Set<String> existedProductCodes = existed.keySet();
    Map<String, List<StockCardCreateRequest>> fromRequest = value.stream()
        .filter(r -> existedProductCodes.contains(r.getProductCode()))
        .collect(groupingBy(StockCardCreateRequest::getProductCode));
    return fromRequest.entrySet().stream()
        .allMatch(e -> validateOneByOne(e.getValue(), existed.get(e.getKey()), actualContext));
  }

  private boolean validateOneByOne(List<StockCardCreateRequest> value, List<ProductMovement> existed,
      HibernateConstraintValidatorContext actualContext) {
    List<ProductMovement> fromRequests = value.stream()
        .map(this::convertToProductMovement)
        .sorted(ProductMovement.ASCENDING)
        .collect(toList());
    existed.sort(ProductMovement.ASCENDING);
    ProductMovement firstInRequest = fromRequests.get(0);
    ProductMovement lastInExisted = existed.get(existed.size() - 1);
    int foundInExisted = findInExisted(firstInRequest.getProductMovementKey(), existed);
    if (foundInExisted < 0) {
      if (lastInExisted.getEventTime().compareTo(firstInRequest.getEventTime()) >= 0) {
        // violation 2 & 3 not same root
        actualContext.addExpressionVariable(FAILED_BY_NOT_FOUND, true);
        actualContext.addExpressionVariable(OCCURRED_DATE, firstInRequest.getEventTime().getOccurredDate());
        actualContext.addExpressionVariable(RECORDED_AT, firstInRequest.getEventTime().getRecordedAt());
        return false;
      }
      // valid 2 or violation 5
      return validateContinuity(lastInExisted, firstInRequest, actualContext);
    }
    for (int requestCursor = 0, existedCursor = foundInExisted; requestCursor < value.size();
        requestCursor++, existedCursor++) {
      if (existedCursor == existed.size()) {
        // valid 1
        break;
      }
      ProductMovement fromRequest = fromRequests.get(requestCursor);
      ProductMovement fromExisted = existed.get(existedCursor);
      if (!fromRequest.getProductMovementKey().equals(fromExisted.getProductMovementKey())) {
        // violation 1
        actualContext.addExpressionVariable(FAILED_BY_NOT_FOUND, true);
        actualContext.addExpressionVariable(OCCURRED_DATE, fromRequest.getEventTime().getOccurredDate());
        actualContext.addExpressionVariable(RECORDED_AT, fromRequest.getEventTime().getRecordedAt());
        return false;
      }
      if (!validateSame(fromRequest, fromExisted, actualContext)) {
        // violation 4
        return false;
      }
    }
    // valid 1 or valid 3
    return true;
  }

  private ProductMovement convertToProductMovement(StockCardCreateRequest request) {
    MovementType movementType = MovementType.valueOf(request.getType());
    MovementDetail movementDetail = new MovementDetail(request.getQuantity(), request.getStockOnHand(), movementType,
        request.getReasonName());
    List<LotMovement> lotMovements = request.getLotEvents().stream()
        .map(l -> convertToLotMovement(l, movementType))
        .sorted(comparing(LotMovement::getLotCode))
        .collect(toList());
    return ProductMovement.builder()
        .productCode(request.getProductCode())
        .eventTime(request.getEventTime())
        .requestedQuantity(request.getRequested())
        .movementDetail(movementDetail)
        .lotMovements(lotMovements)
        .build();
  }

  private LotMovement convertToLotMovement(StockCardLotEventRequest lotEventRequest, MovementType movementType) {
    MovementDetail movementDetail = new MovementDetail(lotEventRequest.getQuantity(), lotEventRequest.getStockOnHand(),
        movementType, lotEventRequest.getReasonName());
    return LotMovement.builder()
        .lotCode(lotEventRequest.getLotCode())
        .movementDetail(movementDetail)
        .build();
  }

  private int findInExisted(ProductMovementKey fromRequest, List<ProductMovement> existed) {
    for (int i = 0, existedSize = existed.size(); i < existedSize; i++) {
      ProductMovement productMovement = existed.get(i);
      if (productMovement.getProductMovementKey().equals(fromRequest)) {
        return i;
      }
    }
    return -1;
  }

  private boolean validateContinuity(ProductMovement former, ProductMovement latter,
      HibernateConstraintValidatorContext actualContext) {
    if (former.getEventTime().compareTo(latter.getEventTime()) >= 0) {
      throw new IllegalArgumentException("Incorrect argument sequence");
    }
    if (latter.getMovementDetail().isRightAfter(former.getMovementDetail())) {
      // valid 2
      return true;
    }
    // violation 5
    actualContext.addExpressionVariable("failedByContinuity", true);
    actualContext.addExpressionVariable(OCCURRED_DATE, latter.getEventTime().getOccurredDate());
    actualContext.addExpressionVariable(RECORDED_AT, latter.getEventTime().getRecordedAt());
    actualContext.addExpressionVariable(INIT_INVENTORY, latter.getMovementDetail().getInitInventory());
    actualContext.addExpressionVariable("formerInventory", former.getMovementDetail().getInventory());
    return false;
  }

  private boolean validateSame(ProductMovement fromRequest, ProductMovement fromExisted,
      HibernateConstraintValidatorContext actualContext) {
    actualContext.addExpressionVariable(OCCURRED_DATE, fromRequest.getEventTime().getOccurredDate());
    actualContext.addExpressionVariable(RECORDED_AT, fromRequest.getEventTime().getRecordedAt());
    actualContext.addExpressionVariable("productCode", fromRequest.getProductCode());
    actualContext.addExpressionVariable(LOT_CODE, null);
    MovementDetail movementDetailFromRequest = fromRequest.getMovementDetail();
    MovementDetail movementDetailFromExisted = fromExisted.getMovementDetail();
    if (!Objects.equals(fromRequest.getRequestedQuantity(), fromExisted.getRequestedQuantity())) {
      actualContext.addExpressionVariable(FAILED_BY_SAME_PRODUCT, true);
      actualContext.addExpressionVariable(FIELD_NAME, "requested");
      actualContext.addExpressionVariable(VALUE_FROM_REQUEST, fromRequest.getRequestedQuantity());
      actualContext.addExpressionVariable(VALUE_FROM_EXISTED, fromExisted.getRequestedQuantity());
      return false;
    }
    if (notSame(movementDetailFromRequest, movementDetailFromExisted, actualContext)) {
      actualContext.addExpressionVariable(FAILED_BY_SAME_PRODUCT, true);
      return false;
    }
    if (!Objects.equals(fromRequest.getLotMovements().size(), fromExisted.getLotMovements().size())) {
      actualContext.addExpressionVariable(FAILED_BY_SAME_PRODUCT, true);
      actualContext.addExpressionVariable(FIELD_NAME, "lotEvents.size");
      actualContext.addExpressionVariable(VALUE_FROM_REQUEST, fromRequest.getLotMovements().size());
      actualContext.addExpressionVariable(VALUE_FROM_EXISTED, fromExisted.getLotMovements().size());
      return false;
    }
    List<LotMovement> lotMovementsFromRequest = fromRequest.getLotMovements();
    List<LotMovement> lotMovementsFromExisted = fromExisted.getLotMovements();
    for (int i = 0; i < lotMovementsFromRequest.size(); i++) {
      LotMovement lotMovementFromRequest = lotMovementsFromRequest.get(i);
      LotMovement lotMovementFromExisted = lotMovementsFromExisted.get(i);
      actualContext.addExpressionVariable(LOT_CODE, lotMovementFromRequest.getLotCode());
      actualContext.addExpressionVariable("index", i);
      if (!Objects.equals(lotMovementFromRequest.getLotCode(), lotMovementFromExisted.getLotCode())) {
        actualContext.addExpressionVariable(FAILED_BY_SAME_LOT, true);
        actualContext.addExpressionVariable(FIELD_NAME, LOT_CODE);
        actualContext.addExpressionVariable(VALUE_FROM_REQUEST, lotMovementFromRequest.getLotCode());
        actualContext.addExpressionVariable(VALUE_FROM_EXISTED, lotMovementFromExisted.getLotCode());
        return false;
      }
      MovementDetail lotMovementDetailFromRequest = lotMovementFromRequest.getMovementDetail();
      MovementDetail lotMovementDetailFromExisted = lotMovementFromExisted.getMovementDetail();
      if (notSame(lotMovementDetailFromRequest, lotMovementDetailFromExisted, actualContext)) {
        actualContext.addExpressionVariable(FAILED_BY_SAME_LOT, true);
        return false;
      }
    }
    return true;
  }

  private boolean notSame(MovementDetail movementDetailFromRequest, MovementDetail movementDetailFromExisted,
      HibernateConstraintValidatorContext actualContext) {
    if (movementDetailFromRequest.getType() != movementDetailFromExisted.getType()) {
      actualContext.addExpressionVariable(FIELD_NAME, "type");
      actualContext.addExpressionVariable(VALUE_FROM_REQUEST, movementDetailFromRequest.getType());
      actualContext.addExpressionVariable(VALUE_FROM_EXISTED, movementDetailFromExisted.getType());
      return true;
    }
    if (movementDetailFromRequest.getReason() != null
        && !Objects.equals(movementDetailFromRequest.getReason(), movementDetailFromExisted.getReason())) {
      actualContext.addExpressionVariable(FIELD_NAME, "reasonName");
      actualContext.addExpressionVariable(VALUE_FROM_REQUEST, movementDetailFromRequest.getReason());
      actualContext.addExpressionVariable(VALUE_FROM_EXISTED, movementDetailFromExisted.getReason());
      return true;
    }
    if (!Objects.equals(movementDetailFromRequest.getInventory(), movementDetailFromExisted.getInventory())) {
      actualContext.addExpressionVariable(FIELD_NAME, "stockOnHand");
      actualContext.addExpressionVariable(VALUE_FROM_REQUEST, movementDetailFromRequest.getInventory());
      actualContext.addExpressionVariable(VALUE_FROM_EXISTED, movementDetailFromExisted.getInventory());
      return true;
    }
    if (!Objects.equals(movementDetailFromRequest.getAdjustment(), movementDetailFromExisted.getAdjustment())) {
      actualContext.addExpressionVariable(FIELD_NAME, "quantity");
      actualContext.addExpressionVariable(VALUE_FROM_REQUEST, movementDetailFromRequest.getAdjustment());
      actualContext.addExpressionVariable(VALUE_FROM_EXISTED, movementDetailFromExisted.getAdjustment());
      return true;
    }
    return false;
  }

  private boolean validateNewProduct(StockCardCreateRequest newProduct,
      HibernateConstraintValidatorContext actualContext) {
    actualContext.addExpressionVariable("productCode", newProduct.getProductCode());
    actualContext.addExpressionVariable(OCCURRED_DATE, newProduct.getOccurredDate());
    actualContext.addExpressionVariable(RECORDED_AT, newProduct.getRecordedAt());
    int initInventory = newProduct.getStockOnHand() - newProduct.getQuantity();
    if (initInventory != 0) {
      actualContext.addExpressionVariable("failedByNewProduct", true);
      actualContext.addExpressionVariable(INIT_INVENTORY, initInventory);
      return false;
    }
    return newProduct.getLotEvents().stream().allMatch(l -> validateNewLot(l, actualContext));
  }

  private boolean validateNewLot(StockCardLotEventRequest newLot, HibernateConstraintValidatorContext actualContext) {
    actualContext.addExpressionVariable("getLotCode", newLot.getLotCode());
    int initInventory = newLot.getStockOnHand() - newLot.getQuantity();
    if (initInventory != 0) {
      actualContext.addExpressionVariable("failedByNewProduct", true);
      actualContext.addExpressionVariable(INIT_INVENTORY, initInventory);
      return false;
    }
    return true;
  }

}
