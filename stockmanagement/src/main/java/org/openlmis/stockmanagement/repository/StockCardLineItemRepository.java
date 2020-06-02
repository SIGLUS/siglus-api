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

package org.openlmis.stockmanagement.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.openlmis.stockmanagement.domain.card.StockCardLineItem;
import org.openlmis.stockmanagement.domain.event.StockEvent;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

public interface StockCardLineItemRepository
    extends PagingAndSortingRepository<StockCardLineItem, UUID> {

  // [SIGLUS change start]
  // [change reason]: calculate kit opened
  @Query(value = "SELECT sum(scli.quantity)"
      + " FROM stockmanagement.stock_card_line_items scli"
      + "   JOIN stockmanagement.stock_cards sc ON sc.id = scli.stockcardid"
      + " WHERE sc.facilityid = :facilityId"
      + "   AND sc.orderableid in :orderableIds"
      + "   AND scli.reasonid = '9b4b653a-f319-4a1b-bb80-8d6b4dd6cc12'"
      + "   AND scli.occurreddate BETWEEN :startDate AND :endDate",
      nativeQuery = true)
  Integer findByFacilityIdAndOrderableIdAndStartDateAndEndDate(
      @Param("facilityId") UUID facilityId,
      @Param("orderableIds") List<UUID> orderableIds,
      @Param("startDate") LocalDate startDate,
      @Param("endDate") LocalDate endDate);
  // [SIGLUS change end]

  // [SIGLUS change start]
  // [change reason]: add method.
  List<StockCardLineItem> findByOriginEvent(StockEvent stockEvent);
  // [SIGLUS change end]

}
