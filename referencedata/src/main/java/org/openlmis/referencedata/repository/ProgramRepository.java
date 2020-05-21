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

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.openlmis.referencedata.domain.Code;
import org.openlmis.referencedata.domain.Program;
import org.openlmis.referencedata.repository.custom.ProgramRepositoryCustom;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProgramRepository
    extends JpaRepository<Program, UUID>, ProgramRepositoryCustom,
    BaseAuditableRepository<Program, UUID> {
  // Add custom Program related members here. See UserRepository.java for examples.

  @Override
  <S extends Program> S save(S entity);

  <S extends Program> S findByCode(Code code);

  @Query(value = "SELECT DISTINCT p.*"
      + " FROM referencedata.programs p"
      + "   JOIN referencedata.right_assignments ra ON ra.programid = p.id"
      + " WHERE ra.userid = :userId",
      nativeQuery = true)
  Set<Program> findSupervisionProgramsByUser(@Param("userId") UUID userId);

  @Query(value = "SELECT DISTINCT p.*"
      + " FROM referencedata.programs p"
      + "   JOIN referencedata.right_assignments ra ON ra.programid = p.id" 
      + "   JOIN referencedata.users u ON u.homefacilityid = ra.facilityid" 
      + "   JOIN referencedata.supported_programs sp ON sp.facilityid = ra.facilityid" 
      + "     AND sp.programid = ra.programid" 
      + "     AND sp.active = TRUE"
      + " WHERE p.active = TRUE" 
      + "   AND ra.userid = :userId",
      nativeQuery = true)
  Set<Program> findHomeFacilitySupervisionProgramsByUser(@Param("userId") UUID userId);

  boolean existsByCode(Code programCode);

  List<Program> findByNameIgnoreCaseContaining(String name);

  List<Program> findByIdInAndNameIgnoreCaseContaining(Collection<UUID> ids, String name);

  @Query(value = "SELECT\n"
      + "    p.*\n"
      + "FROM\n"
      + "    referencedata.programs p\n"
      + "WHERE\n"
      + "    id NOT IN (\n"
      + "        SELECT\n"
      + "            id\n"
      + "        FROM\n"
      + "            referencedata.programs p\n"
      + "            INNER JOIN referencedata.jv_global_id g "
      + "ON CAST(p.id AS varchar) = SUBSTRING(g.local_id, 2, 36)\n"
      + "            INNER JOIN referencedata.jv_snapshot s  ON g.global_id_pk = s.global_id_fk\n"
      + "    )\n"
      + " ORDER BY ?#{#pageable}",
      nativeQuery = true)
  Page<Program> findAllWithoutSnapshots(Pageable pageable);
}
