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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.openlmis.requisition.dto.stockmanagement.StockCardSummaryDto;
import org.openlmis.requisition.service.referencedata.ApproveProductsAggregator;

@AllArgsConstructor
final class StandardStockOnHandRetriever implements StockOnHandRetriever {
  private StockCardSummariesStockManagementService stockCardSummariesService;
  private ApproveProductsAggregator products;
  private UUID programId;
  private UUID facilityId;
  private LocalDate asOfDate;

  public Map<UUID, Integer> get() {
    List<StockCardSummaryDto> cards = getCards();
    return convert(cards);
  }

  private List<StockCardSummaryDto> getCards() {
    return stockCardSummariesService
        .search(programId, facilityId, products.getFullSupplyOrderableIds(), asOfDate);
  }

  private Map<UUID, Integer> convert(List<StockCardSummaryDto> cards) {
    Map<UUID, Integer> stockCardsMap = new HashMap<>();
    cards.forEach(card -> stockCardsMap.put(card.getOrderable().getId(), card.getStockOnHand()));
    return stockCardsMap;
  }

}
