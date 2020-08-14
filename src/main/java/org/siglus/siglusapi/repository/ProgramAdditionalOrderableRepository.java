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

import static org.siglus.common.repository.RepositoryConstants.AND_ORDERABLE_ORIGIN_PROGRAM_ID;
import static org.siglus.common.repository.RepositoryConstants.FROM_ADDITIONAL_ORDERABLE;
import static org.siglus.common.repository.RepositoryConstants.SELECT_PROGRAM_ADDITIONAL_ORDERABLE;
import static org.siglus.common.repository.RepositoryConstants.WHERE_QUERY_ADDITIONAL_ORDERABLE;

import java.util.List;
import java.util.UUID;
import org.siglus.siglusapi.domain.ProgramAdditionalOrderable;
import org.siglus.siglusapi.dto.ProgramAdditionalOrderableDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProgramAdditionalOrderableRepository extends
    JpaRepository<ProgramAdditionalOrderable, UUID> {

  @Query(
      value = SELECT_PROGRAM_ADDITIONAL_ORDERABLE
          + FROM_ADDITIONAL_ORDERABLE
          + WHERE_QUERY_ADDITIONAL_ORDERABLE
          + AND_ORDERABLE_ORIGIN_PROGRAM_ID,
      countQuery = "select count(ao.id) "
          + FROM_ADDITIONAL_ORDERABLE
          + WHERE_QUERY_ADDITIONAL_ORDERABLE
          + AND_ORDERABLE_ORIGIN_PROGRAM_ID)
  Page<ProgramAdditionalOrderableDto> search(@Param("programId") UUID programId,
      @Param("code") String code, @Param("name") String name,
      @Param("orderableOriginProgramId") UUID orderableOriginProgramId, Pageable pageable);

  @Query(
      value = SELECT_PROGRAM_ADDITIONAL_ORDERABLE
          + FROM_ADDITIONAL_ORDERABLE
          + WHERE_QUERY_ADDITIONAL_ORDERABLE,
      countQuery = "select count(ao.id) "
          + FROM_ADDITIONAL_ORDERABLE
          + WHERE_QUERY_ADDITIONAL_ORDERABLE)
  Page<ProgramAdditionalOrderableDto> search(@Param("programId") UUID programId,
      @Param("code") String code, @Param("name") String name, Pageable pageable);

  List<ProgramAdditionalOrderable> findByProgramId(UUID programId);
}
