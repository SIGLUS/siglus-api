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
import org.openlmis.requisition.domain.requisition.Requisition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Persistence repository for saving/finding {@link Requisition}.
 */
public interface RequisitionsRepository extends JpaRepository<Requisition, UUID> {

  @Query(value = "select count(*) from requisition.requisitions r "
      + "where r.facilityid = :facilityId "
      + "and r.programid = :programId "
      + "and r.processingperiodid = :processingPeriodId "
      + "and r.emergency = :emergency "
      + "and r.status in (:status) ",
      nativeQuery = true)
  long countByOrderInfo(@Param("facilityId") UUID facilityId, @Param("programId") UUID programId,
      @Param("processingPeriodId") UUID periodId, @Param("emergency") boolean emergency,
      @Param("status") List<String> status);
}
