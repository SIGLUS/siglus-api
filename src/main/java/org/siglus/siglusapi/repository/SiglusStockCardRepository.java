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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.persistence.criteria.Predicate;

import org.openlmis.stockmanagement.domain.card.StockCard;
import org.openlmis.stockmanagement.dto.StockEventLineItemDto;
import org.siglus.siglusapi.repository.dto.StockOnHandDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public interface SiglusStockCardRepository extends JpaRepository<StockCard, UUID>, JpaSpecificationExecutor<StockCard> {

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

  default List<StockCard> findByFacilityIdAndLineItems(UUID facilityId, List<StockEventLineItemDto> lineItems) {
    return findAll((root, query, cb) -> {
      Predicate byFacility = cb.equal(root.get("facilityId"), facilityId);
      List<Predicate> predicates = new ArrayList<>();
      lineItems.forEach(lineItem -> predicates.add(cb.and(cb.equal(root.get("orderableId"), lineItem.getOrderableId()),
              cb.equal(root.get("lotId"), lineItem.getLotId()))));
      Predicate byOrderableIdAndLotId = predicates.stream().reduce(cb.conjunction(), cb::or);
      return cb.and(byFacility, byOrderableIdAndLotId);
    });
  }

  @Query(name = "StockCard.findStockOnHandDto", nativeQuery = true)
  List<StockOnHandDto> findStockCardDtos(@Param("facilityId") UUID facilityId, @Param("startDate") LocalDate startDate,
      @Param("endDate") LocalDate endDate);
}
