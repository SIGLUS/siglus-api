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

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.openlmis.stockmanagement.domain.card.StockCard;
import org.openlmis.stockmanagement.domain.card.StockCardLineItem;
import org.siglus.siglusapi.repository.dto.StockCardLineItemDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public interface SiglusStockCardLineItemRepository extends JpaRepository<StockCardLineItem, UUID> {

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

  List<StockCardLineItem> findAllByStockCardIn(List<StockCard> stockCards);

  @Query(value = "select\n"
      + "  scli.occurreddate \n"
      + "from\n"
      + "  stockmanagement.stock_cards sc\n"
      + "left join stockmanagement.stock_card_line_items scli on\n"
      + "  sc.id = scli.stockcardid\n"
      + "where sc.facilityid = :facilityId\n"
      + "order by scli.occurreddate desc limit 1", nativeQuery = true)
  LocalDate findFacilityLastMovementDate(@Param("facilityId") UUID facilityId);

  @Query(name = "StockCard.findStockCardLineItemDto", nativeQuery = true)
  List<StockCardLineItemDto> findStockCardLineItemDtos(@Param("facilityId") UUID facilityId,
      @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}