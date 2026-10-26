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

import org.siglus.siglusapi.domain.RightAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RightAssignmentRepository extends JpaRepository<RightAssignment, UUID> {

  /**
   * Retrieves all distinct facility IDs assigned to a specific user.
   */
  @Query("SELECT DISTINCT r.facilityId FROM RightAssignment r WHERE r.userId = :userId")
  List<UUID> findDistinctFacilityIdsByUserId(@Param("userId") UUID userId);

  /**
   * Bulk deletes all right assignments for a given user in a single SQL DELETE query.
   * Note: Requires an active @Transactional context in the calling service.
   */
  @Modifying(clearAutomatically = true)
  @Query("DELETE FROM RightAssignment r WHERE r.userId = :userId")
  int deleteByUserId(@Param("userId") UUID userId);
}
