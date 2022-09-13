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

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.siglus.siglusapi.domain.StockCardLocationMovementLineItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StockCardLocationMovementLineItemRepository extends
    JpaRepository<StockCardLocationMovementLineItem, UUID> {

  List<StockCardLocationMovementLineItem> findByStockCardId(UUID stockCardId);

  List<StockCardLocationMovementLineItem> findAllByStockCardIdIn(Set<UUID> stockCardIds);

  @Query(value = "select * from siglusintegration.stock_card_location_movement_line_items "
      + "         where (stockcardid, processeddate) in ( "
      + "         select stockcardid, max(processeddate) "
      + "         from siglusintegration.stock_card_location_movement_line_items c "
      + "         where c.stockcardid in :stockCardIds "
      + "         group by c.stockcardid)", nativeQuery = true)
  List<StockCardLocationMovementLineItem> findPreviousRecordByStockCardId(
      @Param("stockCardIds") Collection<UUID> stockCardIds);
}
