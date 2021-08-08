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

package org.siglus.common.repository;

import static org.siglus.common.repository.RepositoryConstants.SELECT_PROGRAM_ORDERABLE;
import static org.siglus.common.repository.RepositoryConstants.WHERE_LATEST_PROGRAM_ORDERABLE;

import java.util.List;
import java.util.UUID;
import org.siglus.common.domain.referencedata.ProgramOrderable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProgramOrderableRepository extends JpaRepository<ProgramOrderable, UUID> {

  @Query(value = SELECT_PROGRAM_ORDERABLE
      + WHERE_LATEST_PROGRAM_ORDERABLE
      + "      AND po.programid = :programId",
      nativeQuery = true)
  List<ProgramOrderable> findByProgramId(@Param("programId") UUID programId);

  @Query(value = "Select count(*) FROM referencedata.program_orderables po "
      + WHERE_LATEST_PROGRAM_ORDERABLE
      + "AND po.programid = :programId",
      nativeQuery = true)
  Long countByProgramId(@Param("programId") UUID programId);
}