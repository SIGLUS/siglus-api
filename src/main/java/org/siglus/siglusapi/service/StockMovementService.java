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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.siglus.siglusapi.dto.StockMovementResDto;
import org.siglus.siglusapi.dto.android.PeriodOfProductMovements;
import org.siglus.siglusapi.dto.android.ProductMovement;
import org.siglus.siglusapi.repository.StockManagementRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StockMovementService {

  private static final String UNPACK_KIT_TYPE = "UNPACK_KIT";

  private static final String UNPACK_KIT_REASON = "Unpack Kit";
  private static final String ISSUE_TYPE = "ISSUE";

  private static final String OUTROS = "Outros";
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
    LinkedList<StockMovementResDto> stockMovementResDtos = new LinkedList<>();
    for (ProductMovement productMovement : productMovements) {
      String destinationName = "";
      String destinationFreeText = "";
      String sourceName = "";
      String sourceFreeText = "";
      String reason = null;
      int soh = 0;
      int count = 0;
      switch (productMovement.getMovementDetail().getType()) {
        case ISSUE:
          count -= productMovement.getMovementDetail().getAdjustment();
          soh += productMovement.getStockQuantity();
          destinationName = productMovement.getMovementDetail().getReason();
          if (OUTROS.equals(productMovement.getMovementDetail().getReason())) {
            destinationName = OUTROS;
            destinationFreeText = productMovement.getDestinationfreetext();
          }
          break;
        case RECEIVE:
          count += productMovement.getMovementDetail().getAdjustment();
          soh += productMovement.getStockQuantity();
          sourceName = productMovement.getMovementDetail().getReason();
          if (OUTROS.equals(productMovement.getMovementDetail().getReason())) {
            sourceName = OUTROS;
            sourceFreeText = productMovement.getSourcefreetext();
          }
          break;
        case UNPACK_KIT:
        case PHYSICAL_INVENTORY:
        case ADJUSTMENT:
          count += productMovement.getMovementDetail().getAdjustment();
          soh += productMovement.getStockQuantity();
          reason = productMovement.getMovementDetail().getType().toString();
          if (productMovement.getMovementDetail().getType().toString().equals("UNPACK_KIT")) {
            reason = productMovement.getMovementDetail().getReason();
          }
          break;
        default:
          break;
      }
      StockMovementResDto stockMovementResDto = StockMovementResDto.builder()
          .movementQuantity(count)
          .productSoh(soh)
          .dateOfMovement(productMovement.getEventTime().getOccurredDate())
          .type(productMovement.getMovementDetail().getType().toString())
          .productCode(productMovement.getProductCode())
          .reason(reason)
          .documentNumber(productMovement.getDocumentNumber())
          .sourceName(sourceName)
          .destinationName(destinationName)
          .sourceFreeText(sourceFreeText)
          .destinationFreeText(destinationFreeText)
          .signature(productMovement.getSignature())
          .build();
      stockMovementResDtos.add(stockMovementResDto);
    }
    return stockMovementResDtos;
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
