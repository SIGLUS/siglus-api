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

import org.openlmis.stockmanagement.domain.physicalinventory.PhysicalInventory;
import org.siglus.siglusapi.dto.SiglusPhysicalInventoryHistoryDto;
import org.siglus.siglusapi.dto.SiglusPhysicalInventoryHistoryLineItemDto;
import org.siglus.siglusapi.repository.dto.SiglusPhysicalInventoryBriefDto;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

public interface SiglusPhysicalInventoryRepository extends PagingAndSortingRepository<PhysicalInventory, UUID> {

  @Query(name = "PhysicalInventory.queryForAllProgram", nativeQuery = true)
  List<SiglusPhysicalInventoryBriefDto> queryForAllProgram(
      @Param("facilityId") UUID facilityId, @Param("isDraft") boolean isDraft);

  @Query(name = "PhysicalInventory.queryForOneProgram", nativeQuery = true)
  List<SiglusPhysicalInventoryBriefDto> queryForOneProgram(
      @Param("facilityId") UUID facilityId, @Param("programId") UUID programId, @Param("isDraft") boolean isDraft);

  @Query(name = "PhysicalInventoryHistory.queryPhysicalInventoryHistory", nativeQuery = true)
  List<SiglusPhysicalInventoryHistoryDto> queryPhysicalInventoryHistories(@Param("facilityId") UUID facilityId);

  @Query(name = "PhysicalInventoryHistoryLineItem.queryPhysicalInventoryHistoryLineItem", nativeQuery = true)
  List<SiglusPhysicalInventoryHistoryLineItemDto> queryPhysicalInventoryHistoriesLineItem(
      @Param("physicalInventoryId") UUID physicalInventoryId);
}
