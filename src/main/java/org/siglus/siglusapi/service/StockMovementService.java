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

package org.siglus.siglusapi.service;

import static org.siglus.siglusapi.constant.FacilityTypeConstants.OUTROS;
import static org.siglus.siglusapi.constant.FieldConstants.ADJUSTMENT_KEY;
import static org.siglus.siglusapi.constant.FieldConstants.INITIAL_INVENTORY_KEY;
import static org.siglus.siglusapi.constant.FieldConstants.ISSUE_TYPE;
import static org.siglus.siglusapi.constant.FieldConstants.PHYSICAL_INVENTORY;
import static org.siglus.siglusapi.constant.FieldConstants.PHYSICAL_INVENTORY_KEY;
import static org.siglus.siglusapi.constant.FieldConstants.UNPACK_KIT_REASON;
import static org.siglus.siglusapi.constant.FieldConstants.UNPACK_KIT_TYPE;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections.CollectionUtils;
import org.siglus.siglusapi.dto.MovementTypeHandlerResultDto;
import org.siglus.siglusapi.dto.StockMovementResDto;
import org.siglus.siglusapi.dto.android.PeriodOfProductMovements;
import org.siglus.siglusapi.dto.android.ProductMovement;
import org.siglus.siglusapi.repository.StockManagementRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StockMovementService {

  @Autowired
  private StockManagementRepository stockManagementRepository;

  public List<StockMovementResDto> getProductMovements(
      Set<UUID> orderableIds, UUID facilityId, LocalDate since, LocalDate till) {
    PeriodOfProductMovements productMovements =
        stockManagementRepository.getAllProductMovements(facilityId, orderableIds, since, till);
    if (productMovements == null) {
      return new ArrayList<>();
    }
    return simplifyStockMovementResponse(productMovements);
  }

  public List<StockMovementResDto> getMovementsByProduct(UUID facilityId, UUID orderableId) {
    HashSet<UUID> orderableIdsSet = new HashSet<>();
    if (orderableId != null) {
      orderableIdsSet.add(orderableId);
    }
    PeriodOfProductMovements allProductMovements = stockManagementRepository.getAllProductMovements(facilityId,
        orderableIdsSet);
    List<ProductMovement> productMovements = allProductMovements.getProductMovements();
    if (CollectionUtils.isEmpty(productMovements)) {
      return new ArrayList<>();
    }
    LinkedList<StockMovementResDto> stockMovementResDtos = new LinkedList<>();
    boolean initialFlag = true;
    for (ProductMovement productMovement : productMovements) {
      MovementTypeHandlerResultDto movementTypeHandlerResultDto = movementTypeHandler(productMovement, initialFlag);
      initialFlag = false;
      StockMovementResDto stockMovementResDto = StockMovementResDto.builder()
          .movementQuantity(movementTypeHandlerResultDto.getCount())
          .productSoh(movementTypeHandlerResultDto.getSoh())
          .dateOfMovement(productMovement.getEventTime().getOccurredDate())
          .type(productMovement.getMovementDetail().getType().toString())
          .productCode(productMovement.getProductCode())
          .reason(movementTypeHandlerResultDto.getReason())
          .documentNumber(productMovement.getDocumentNumber())
          .sourceName(movementTypeHandlerResultDto.getSourceName())
          .destinationName(movementTypeHandlerResultDto.getDestinationName())
          .sourceFreeText(movementTypeHandlerResultDto.getSourceFreeText())
          .destinationFreeText(movementTypeHandlerResultDto.getDestinationFreeText())
          .signature(productMovement.getSignature())
          .build();
      stockMovementResDtos.add(stockMovementResDto);
    }
    Collections.reverse(stockMovementResDtos);
    StockMovementResDto first = stockMovementResDtos.get(stockMovementResDtos.size() - 1);
    StockMovementResDto initial = StockMovementResDto.builder().reason(INITIAL_INVENTORY_KEY)
        .dateOfMovement(first.getDateOfMovement()).productSoh(0).build();
    stockMovementResDtos.add(initial);
    return stockMovementResDtos;
  }

  MovementTypeHandlerResultDto movementTypeHandler(ProductMovement productMovement, boolean initialFlag) {
    String destinationName = "";
    String destinationFreeText = "";
    String sourceName = "";
    String sourceFreeText = "";
    String reason = "";
    int soh = 0;
    int count = 0;
    switch (productMovement.getMovementDetail().getType()) {
      case ISSUE:
        count -= productMovement.getMovementDetail().getAdjustment();
        soh += productMovement.getStockQuantity();
        destinationName = OUTROS.equals(productMovement.getMovementDetail().getReason()) ? OUTROS
            : productMovement.getMovementDetail().getReason();
        destinationFreeText = productMovement.getDestinationfreetext();
        break;
      case RECEIVE:
        count += productMovement.getMovementDetail().getAdjustment();
        soh += productMovement.getStockQuantity();
        sourceName = OUTROS.equals(productMovement.getMovementDetail().getReason()) ? OUTROS
            : productMovement.getMovementDetail().getReason();
        sourceFreeText = productMovement.getSourcefreetext();
        break;
      case UNPACK_KIT:
        count += productMovement.getMovementDetail().getAdjustment();
        soh += productMovement.getStockQuantity();
        reason = productMovement.getMovementDetail().getReason();
        break;
      case PHYSICAL_INVENTORY:
        count += productMovement.getMovementDetail().getAdjustment();
        soh += productMovement.getStockQuantity();
        reason = initialFlag ? INITIAL_INVENTORY_KEY : PHYSICAL_INVENTORY_KEY;
        break;
      case ADJUSTMENT:
        count += productMovement.getMovementDetail().getAdjustment();
        soh += productMovement.getStockQuantity();
        reason = ADJUSTMENT_KEY;
        break;
      default:
    }
    return MovementTypeHandlerResultDto.builder()
        .count(count)
        .soh(soh)
        .sourceName(sourceName)
        .sourceFreeText(sourceFreeText)
        .destinationName(destinationName)
        .destinationFreeText(destinationFreeText)
        .reason(reason)
        .build();
  }

  private List<StockMovementResDto> simplifyStockMovementResponse(
      PeriodOfProductMovements periodOfProductMovements) {
    List<StockMovementResDto> stockMovementResDtoList = new LinkedList<>();
    periodOfProductMovements.getProductMovements().forEach(productMovement -> {
      StockMovementResDto stockMovementResDto = StockMovementResDto.builder()
          .productCode(productMovement.getProductCode())
          .dateOfMovement(productMovement.getEventTime().getOccurredDate())
          .type(UNPACK_KIT_TYPE.equals(productMovement.getMovementDetail().getType().toString()) ? ISSUE_TYPE
              : productMovement.getMovementDetail().getType().toString())
          .reason(UNPACK_KIT_TYPE.equals(productMovement.getMovementDetail().getType().toString()) ? UNPACK_KIT_REASON
              : productMovement.getMovementDetail().getReason())
          .movementQuantity(productMovement.getMovementDetail().getAdjustment())
          .productSoh(productMovement.getStockQuantity()).requested(productMovement.getRequestedQuantity())
          .signature(productMovement.getSignature())
          .documentNumber(productMovement.getDocumentNumber())
          .build();
      stockMovementResDtoList.add(stockMovementResDto);
    });
    return stockMovementResDtoList;
  }
}
