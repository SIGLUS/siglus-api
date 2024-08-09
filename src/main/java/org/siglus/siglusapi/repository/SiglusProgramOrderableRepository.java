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

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.openlmis.referencedata.domain.ProgramOrderable;
import org.siglus.siglusapi.repository.dto.ProgramOrderableDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SiglusProgramOrderableRepository extends JpaRepository<ProgramOrderable, UUID> {

  @Query(name = "ProgramOrderable.findProgramOrderableDto", nativeQuery = true)
  List<ProgramOrderableDto> findAllMaxVersionProgramOrderableDtos();

  List<ProgramOrderable> findByProgramIdIn(Collection<UUID> programIds);

  @Query(value = "select distinct on (po.orderableid) po.* from referencedata.program_orderables po "
      + "    where orderableid in :orderableIds order by po.orderableid, po.orderableversionnumber desc ",
      nativeQuery = true)
  List<ProgramOrderable> findByOrderableIdIn(@Param("orderableIds")Collection<UUID> orderIds);

  @Query(value = "select distinct on (po.orderableid) po.* from referencedata.program_orderables po "
      + "    where po.programid in :programIds order by po.orderableid, po.orderableversionnumber desc ",
      nativeQuery = true)
  List<ProgramOrderable> findMaxVersionOrderableByProgramIds(@Param("programIds")Collection<UUID> programIds);

  @Query(name = "ProgramOrderable.findMaxVersionByOrderableIds", nativeQuery = true)
  List<ProgramOrderableDto> findMaxVersionProgramOrderableDtosByOrderableIds(
      @Param("orderableIds")Collection<UUID> orderableIds);
}
