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
import org.siglus.siglusapi.repository.dto.StockOnHandDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public interface SiglusStockCardRepository extends JpaRepository<StockCard, UUID>, JpaSpecificationExecutor<StockCard> {

  List<StockCard> findByFacilityIdAndProgramId(UUID facilityId, UUID programId);

  List<StockCard> findByFacilityIdAndOrderableId(
      @Param("facilityId") UUID facilityId,
      @Param("orderableId") UUID orderableId);

  List<StockCard> findByFacilityIdAndProgramIdAndOrderableIdAndLotId(
      @Param("facilityId") UUID facilityId,
      @Param("programId") UUID programId,
      @Param("orderableId") UUID orderableId,
      @Param("lotId") UUID lotId);

  void deleteStockCardsByFacilityIdAndOrderableIdIn(@Param("facilityId") UUID facilityId,
      @Param("orderableId") Set<UUID> orderableIds);

  @Query(value = "select * from stockmanagement.stock_cards where facilityid=:facilityId "
      + "and concat(cast(orderableid as varchar),cast(lotid as varchar)) in :pairs",
      nativeQuery = true)
  List<StockCard> findByFacilityIdAndOrderableLotIdPairs(
      @Param("facilityId") UUID facilityId,
      @Param("pairs") Set<String> orderableLotIdPairs);

  @Query(name = "StockCard.findStockOnHandDto", nativeQuery = true)
  List<StockOnHandDto> findStockCardDtos(@Param("facilityId") UUID facilityId, @Param("startDate") LocalDate startDate,
      @Param("endDate") LocalDate endDate);
}
