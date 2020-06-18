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

package org.openlmis.requisition.service.stockmanagement;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.openlmis.requisition.dto.ObjectReferenceDto;
import org.openlmis.requisition.dto.stockmanagement.StockCardSummaryDto;
import org.openlmis.requisition.service.referencedata.ApproveProductsAggregator;
import org.openlmis.stockmanagement.service.StockCardSummaries;
import org.openlmis.stockmanagement.service.StockCardSummariesService;
import org.openlmis.stockmanagement.service.StockCardSummariesV2SearchParams;
import org.openlmis.stockmanagement.web.stockcardsummariesv2.StockCardSummariesV2DtoBuilder;
import org.openlmis.stockmanagement.web.stockcardsummariesv2.StockCardSummaryV2Dto;

@AllArgsConstructor
final class StandardStockOnHandRetriever implements StockOnHandRetriever {

  // [SIGLUS change start]
  // [change reason]: call our modify stock card.
  // private StockCardSummariesStockManagementService stockCardSummariesService;
  private StockCardSummariesService stockCardSummariesService;
  private StockCardSummariesV2DtoBuilder stockCardSummariesV2DtoBuilder;
  // [SIGLUS change end]

  private ApproveProductsAggregator products;
  private UUID programId;
  private UUID facilityId;
  private LocalDate asOfDate;

  public Map<UUID, Integer> get() {
    List<StockCardSummaryDto> cards = getCards();
    return convert(cards);
  }

  private List<StockCardSummaryDto> getCards() {
    // [SIGLUS change start]
    // [change reason]: 1. call ourself stockCardSummariesService to make sure that
    //                  orderableFulfillService can get by virtual program.
    //                  2. support no product section
    // return stockCardSummariesService
    //     .search(programId, facilityId, products.getFullSupplyOrderableIds(), asOfDate);
    // StockCardSummariesV2SearchParams
    if (products.getFullSupplyProducts().isEmpty()) {
      return Collections.emptyList();
    }
    StockCardSummariesV2SearchParams v2SearchParams = StockCardSummariesV2SearchParams.builder()
        .programId(programId)
        .facilityId(facilityId)
        .asOfDate(asOfDate)
        .build();
    StockCardSummaries summaries = stockCardSummariesService
        .findStockCards(v2SearchParams);
    List<StockCardSummaryV2Dto> dtos = stockCardSummariesV2DtoBuilder.build(
        summaries.getStockCardsForFulfillOrderables().stream()
            .collect(Collectors.toList()),
        summaries.getOrderableFulfillMap(),
        false);
    return dtos.stream()
        .map(stockCardSummaryV2Dto -> {
          StockCardSummaryDto summaryDto = new StockCardSummaryDto();
          summaryDto.setOrderable(
              new ObjectReferenceDto(stockCardSummaryV2Dto.getOrderable().getId(),
                  stockCardSummaryV2Dto.getOrderable().getHref()));
          summaryDto.setStockOnHand(stockCardSummaryV2Dto.getStockOnHand());
          return summaryDto;
        }).collect(Collectors.toList());
    // [SIGLUS change end]
  }

  private Map<UUID, Integer> convert(List<StockCardSummaryDto> cards) {
    Map<UUID, Integer> stockCardsMap = new HashMap<>();
    cards.forEach(card -> stockCardsMap.put(card.getOrderable().getId(), card.getStockOnHand()));
    return stockCardsMap;
  }

}
