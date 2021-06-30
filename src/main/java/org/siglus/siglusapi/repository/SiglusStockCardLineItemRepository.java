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

package org.siglus.siglusapi.repository;

import java.util.List;
import java.util.UUID;
import org.openlmis.stockmanagement.domain.card.StockCardLineItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SiglusStockCardLineItemRepository extends JpaRepository<StockCardLineItem, UUID> {

  @Query(value = "select i.* from stockmanagement.stock_card_line_items i "
      + "inner join stockmanagement.stock_cards c  "
      + "on c.id = i.stockcardid "
      + "where "
      + "c.facilityid = :facilityId "
      + "and i.processeddate > :startTime "
      + "and i.processeddate <= :endTime", nativeQuery = true)
  List<StockCardLineItem> findByFacilityIdAndIdStartTimeEndTime(
      @Param("facilityId") UUID facilityId,
      @Param("startTime") String startTime,
      @Param("endTime") String endTime);
}