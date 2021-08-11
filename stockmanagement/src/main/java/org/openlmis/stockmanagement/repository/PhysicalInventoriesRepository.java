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

import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.openlmis.stockmanagement.domain.physicalinventory.PhysicalInventory;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

public interface PhysicalInventoriesRepository
    extends PagingAndSortingRepository<PhysicalInventory, UUID> {

  List<PhysicalInventory> findByProgramIdAndFacilityIdAndIsDraft(
      @Param("programId") UUID programId,
      @Param("facilityId") UUID facilityId,
      @Param("isDraft") boolean isDraft);

  List<PhysicalInventory> findByProgramIdAndFacilityId(
      @Param("programId") UUID programId,
      @Param("facilityId") UUID facilityId);

  // [SIGLUS change start]
  // [change reason]: performance optimization
  @Query(value = "select cast(id as varchar) id from stockmanagement.physical_inventories "
      + "where programid = :programId "
      + "and facilityid = :facilityId "
      + "and isdraft = :isDraft", nativeQuery = true)
  String findIdByProgramIdAndFacilityIdAndIsDraft(
      @Param("programId") UUID programId,
      @Param("facilityId") UUID facilityId,
      @Param("isDraft") boolean isDraft);
  // [SIGLUS change end]

  // [SIGLUS change start]
  // [change reason]: add new methods.
  PhysicalInventory findTopByFacilityIdAndIsDraftOrderByOccurredDateDesc(UUID facilityId,
      boolean isDraft);

  @Query(value = "SELECT * FROM stockmanagement.physical_inventories pi "
      + "WHERE pi.facilityid = :facility "
      + "      AND pi.occurreddate >= :startDate "
      + "      AND pi.occurreddate <= :endDate ", nativeQuery = true)
  List<PhysicalInventory> findByFacilityIdAndStartDateAndEndDate(
      @Param("facility") UUID facility,
      @Param("startDate") String startDate,
      @Param("endDate") String endDate);

  @Query(value = "SELECT * FROM stockmanagement.physical_inventories pi "
      + "inner join stockmanagement.stock_card_line_items scli "
      + "on scli.origineventid = pi.stockeventid "
      + "inner join stockmanagement.stock_cards sc "
      + "on sc.id = scli.stockcardid "
      + "WHERE sc.facilityid = :facility "
      + "AND sc.orderableid in :orderableIds) ", nativeQuery = true)
  List<PhysicalInventory> findByFacilityIdAndOrderableIds(
      @Param("facility") UUID facility, @Param("orderableIds") Set<UUID> orderableIds);
  // [SIGLUS change end]

}
