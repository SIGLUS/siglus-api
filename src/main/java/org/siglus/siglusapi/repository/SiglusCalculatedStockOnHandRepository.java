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
import org.openlmis.stockmanagement.domain.event.CalculatedStockOnHand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public interface SiglusCalculatedStockOnHandRepository extends JpaRepository<CalculatedStockOnHand, UUID> {

  @Query(value = "select h.* from stockmanagement.calculated_stocks_on_hand h "
      + "inner join stockmanagement.stock_cards c "
      + "on c.id = h.stockcardid "
      + "where "
      + "c.facilityid = :facilityId "
      + "and h.processeddate > :startTime "
      + "and h.processeddate <= :endTime", nativeQuery = true)
  List<CalculatedStockOnHand> findByFacilityIdAndIdStartTimeEndTime(
      @Param("facilityId") UUID facilityId,
      @Param("startTime") String startTime,
      @Param("endTime") String endTime);
}