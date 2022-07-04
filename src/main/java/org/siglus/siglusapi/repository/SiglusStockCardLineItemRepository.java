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
import java.util.Set;
import java.util.UUID;
import org.openlmis.stockmanagement.domain.card.StockCardLineItem;
import org.siglus.siglusapi.repository.dto.FacillityStockCardDateDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SiglusStockCardLineItemRepository extends JpaRepository<StockCardLineItem, UUID> {

  @Query(value = "select i.* from stockmanagement.stock_card_line_items i "
      + "inner join stockmanagement.stock_cards c  "
      + "on c.id = i.stockcardid "
      + "where "
      + "c.facilityid = :facilityId "
      + "and i.occurreddate > :startTime "
      + "and i.occurreddate <= :endTime", nativeQuery = true)
  List<StockCardLineItem> findByFacilityIdAndIdStartTimeEndTime(
      @Param("facilityId") UUID facilityId,
      @Param("startTime") String startTime,
      @Param("endTime") String endTime);

  @Query(value = "select scli.* from stockmanagement.stock_card_line_items scli "
      + "inner join stockmanagement.stock_cards sc  "
      + "on sc.id = scli.stockcardid "
      + "where "
      + "sc.facilityid = :facilityId "
      + "and sc.orderableid in (:orderableIds) ", nativeQuery = true)
  List<StockCardLineItem> findByFacilityIdAndOrderableIdIn(
      @Param("facilityId") UUID facilityId,
      @Param("orderableIds") Set<UUID> orderableIds);

  @Query(value = "select scli.* from stockmanagement.stock_card_line_items scli "
      + "inner join stockmanagement.stock_cards sc "
      + "on scli.stockcardid = sc.id "
      + "where "
      + "sc.facilityid = :facilityId "
      + "and scli.documentnumber = :originOrderCode "
      + "and scli.sourceid is not null", nativeQuery = true)
  List<StockCardLineItem> findByFacilityIdAndLotIdIn(@Param("facilityId") UUID facilityId,
      @Param("originOrderCode") String originOrderCode);

  @Query(value = "SELECT DISTINCT ON (sc.facilityid, sc.programid) "
      + "            MIN(scli.occurreddate) OVER (PARTITION BY sc.facilityid, sc.programid) AS occurreddate, "
      + "            sc.facilityid, "
      + "            sc.programid "
      + "FROM stockmanagement.stock_card_line_items scli "
      + "         LEFT JOIN stockmanagement.stock_cards sc ON scli.stockcardid = sc.id", nativeQuery = true)
  List<FacillityStockCardDateDto> findFirstStockCardGroupByFacility();
}