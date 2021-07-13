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
import org.siglus.siglusapi.domain.RequisitionExtension;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RequisitionExtensionRepository extends JpaRepository<RequisitionExtension, UUID> {

  RequisitionExtension findByRequisitionId(UUID requisitionId);

  List<RequisitionExtension> findByRequisitionIdIn(Set<UUID> requisitionIds);

  @Query(value = "select * from siglusintegration.requisition_extension r"
      + " where (CHAR_LENGTH(CAST(r.requisitionnumber as varchar)) > 7 and"
      + " concat(r.requisitionnumberprefix, r.requisitionnumber) = :requisitionNumber) or"
      + " (CHAR_LENGTH(CAST(r.requisitionnumber as varchar)) < 7 and"
      + " concat(r.requisitionnumberprefix, "
      + "to_char(r.requisitionnumber, 'fm0000000')) = :requisitionNumber);", nativeQuery = true)
  RequisitionExtension findByRequisitionNumber(@Param("requisitionNumber") String requisitionNumber);

  @Query(value = "SELECT * FROM siglusintegration.requisition_extension e "
      + "WHERE e.facilityId = :facilityId and e.actualstartdate > :startDate", nativeQuery = true)
  List<RequisitionExtension> searchRequisitionIdByFacilityAndDate(@Param("facilityId") UUID facilityId,
      @Param("startDate") String startDate);
}
