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

import java.util.List;
import java.util.UUID;
import org.openlmis.referencedata.domain.StockAdjustmentReason;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

public interface StockAdjustmentReasonRepository
        extends PagingAndSortingRepository<StockAdjustmentReason, UUID>,
        BaseAuditableRepository<StockAdjustmentReason, UUID> {

  @Override
  <S extends StockAdjustmentReason> S save(S entity);

  @Override
  <S extends StockAdjustmentReason> Iterable<S> save(Iterable<S> entities);

  @Query("SELECT r FROM StockAdjustmentReason r WHERE r.program.id = :programId")
  List<StockAdjustmentReason> findByProgramId(@Param("programId") UUID programId);

  @Query(value = "SELECT\n"
      + "    sar.*\n"
      + "FROM\n"
      + "    referencedata.stock_adjustment_reasons sar\n"
      + "WHERE\n"
      + "    id NOT IN (\n"
      + "        SELECT\n"
      + "            id\n"
      + "        FROM\n"
      + "            referencedata.stock_adjustment_reasons sar\n"
      + "            INNER JOIN referencedata.jv_global_id g "
      + "ON CAST(sar.id AS varchar) = SUBSTRING(g.local_id, 2, 36)\n"
      + "            INNER JOIN referencedata.jv_snapshot s  ON g.global_id_pk = s.global_id_fk\n"
      + "    )\n"
      + " ORDER BY ?#{#pageable}",
      nativeQuery = true)
  Page<StockAdjustmentReason> findAllWithoutSnapshots(Pageable pageable);
}

