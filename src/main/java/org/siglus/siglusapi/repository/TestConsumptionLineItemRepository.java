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
import org.siglus.siglusapi.domain.TestConsumptionLineItem;
import org.siglus.siglusapi.dto.TestConsumptionOutcomeDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TestConsumptionLineItemRepository
    extends JpaRepository<TestConsumptionLineItem, UUID> {

  List<TestConsumptionLineItem> findByRequisitionId(UUID requisitionId);

  List<TestConsumptionLineItem> findByRequisitionIdIn(Set<UUID> requisitionIds);

  void deleteByRequisitionId(UUID requisitionId);

  @Query(name = "TestConsumptionLineItem.sumValueRequisitionsUnderHighLevelFacility", nativeQuery = true)
  List<TestConsumptionOutcomeDto> sumValueRequisitionsUnderHighLevelFacility(@Param("facilityId") UUID facilityId,
      @Param("periodId") UUID periodId, @Param("programId") UUID programId);

  @Query(name = "TestConsumptionLineItem.maxValueRequisitionsInLastPeriods", nativeQuery = true)
  List<TestConsumptionOutcomeDto> maxValueRequisitionsInLastPeriods(@Param("facilityId") UUID facilityId,
      @Param("periodId") UUID periodId, @Param("programId") UUID programId);

}
