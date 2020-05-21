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

package org.openlmis.referencedata.repository;

import java.util.Set;
import java.util.UUID;
import org.openlmis.referencedata.domain.RightAssignment;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

public interface RightAssignmentRepository extends
    PagingAndSortingRepository<RightAssignment, UUID> {

  @Query(value = "SELECT" 
      + "   CASE WHEN ra.programid IS NULL AND ra.facilityid IS NULL THEN ra.rightname"
      + "        WHEN ra.programid IS NULL THEN ra.rightname || '|' || ra.facilityid"
      + "        ELSE ra.rightname || '|' || ra.facilityid || '|' || ra.programid"
      + "   END AS permissionstring"
      + " FROM referencedata.right_assignments ra"
      + " WHERE ra.userid = :userId",
      nativeQuery = true)
  Set<String> findByUser(@Param("userId") UUID userId);

  boolean existsByUserIdAndRightName(UUID user, String rightName);
  
  boolean existsByUserIdAndAndRightNameAndFacilityId(UUID user, String rightName, UUID facilityId);

  boolean existsByUserIdAndAndRightNameAndFacilityIdAndProgramId(UUID user, String rightName,
      UUID facilityId, UUID programId);
}
