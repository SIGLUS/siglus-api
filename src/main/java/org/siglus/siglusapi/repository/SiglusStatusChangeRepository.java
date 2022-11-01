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
import org.openlmis.requisition.domain.requisition.StatusChange;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

public interface SiglusStatusChangeRepository
    extends PagingAndSortingRepository<StatusChange, UUID> {

  @Query(value = "select * from requisition.status_changes "
      + "where requisitionid = :requisitionId "
      + "and status = :status", nativeQuery = true)
  StatusChange findByRequisitionIdAndStatus(@Param("requisitionId") UUID requisitionId, @Param("status") String status);

  @Query(value = "select * from requisition.status_changes "
      + "where requisitionid = :requisitionId", nativeQuery = true)
  List<StatusChange> findByRequisitionId(@Param("requisitionId") UUID requisitionId);

  void deleteByRequisitionId(UUID requisitionId);
}
