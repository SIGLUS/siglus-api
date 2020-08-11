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

  String SELECT_PROGRAM_ADDITIONAL_ORDERABLE =
      "select new org.siglus.siglusapi.dto.ProgramAdditionalOrderableDto(ao.id, ao.programId, "
          + "o.productCode, o.fullProductName, o.description, ao.orderableOriginProgramId) ";
  String FROM = "from ProgramAdditionalOrderable ao, Orderable o ";
  String WHERE_QUERY = "where o.identity.versionNumber = "
      + "(select max(oo.identity.versionNumber) from Orderable oo "
      + "where o.identity.id = oo.identity.id) "
      + "and ao.additionalOrderableId = o.identity.id "
      + "and ao.programId = :programId "
      + "and upper(o.productCode.code) like :code "
      + "and upper(o.fullProductName) like :name ";
  String AND_ORDERABLE_ORIGIN_PROGRAM_ID = "and ao.orderableOriginProgramId "
      + "= :orderableOriginProgramId";

  @Query(
      value = SELECT_PROGRAM_ADDITIONAL_ORDERABLE
          + FROM
          + WHERE_QUERY
          + AND_ORDERABLE_ORIGIN_PROGRAM_ID,
      countQuery = "select count(ao.id) "
          + FROM
          + WHERE_QUERY
          + AND_ORDERABLE_ORIGIN_PROGRAM_ID)
  Page<ProgramAdditionalOrderableDto> search(@Param("programId") UUID programId,
      @Param("code") String code, @Param("name") String name,
      @Param("orderableOriginProgramId") UUID orderableOriginProgramId, Pageable pageable);

  @Query(
      value = SELECT_PROGRAM_ADDITIONAL_ORDERABLE
          + FROM
          + WHERE_QUERY,
      countQuery = "select count(ao.id) "
          + FROM
          + WHERE_QUERY)
  Page<ProgramAdditionalOrderableDto> search(@Param("programId") UUID programId,
      @Param("code") String code, @Param("name") String name, Pageable pageable);

}
