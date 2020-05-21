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
import org.openlmis.referencedata.domain.SupervisoryNode;
import org.openlmis.referencedata.repository.custom.SupervisoryNodeRepositoryCustom;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SupervisoryNodeRepository
    extends JpaRepository<SupervisoryNode, UUID>, SupervisoryNodeRepositoryCustom,
    BaseAuditableRepository<SupervisoryNode, UUID> {

  <S extends SupervisoryNode> S findByCode(String code);

  boolean existsByCode(String code);

  @Query(value = "SELECT\n"
      + "    sn.*\n"
      + "FROM\n"
      + "    referencedata.supervisory_nodes sn\n"
      + "WHERE\n"
      + "    id NOT IN (\n"
      + "        SELECT\n"
      + "            id\n"
      + "        FROM\n"
      + "            referencedata.supervisory_nodes sn\n"
      + "            INNER JOIN referencedata.jv_global_id g "
      + "ON CAST(sn.id AS varchar) = SUBSTRING(g.local_id, 2, 36)\n"
      + "            INNER JOIN referencedata.jv_snapshot s  ON g.global_id_pk = s.global_id_fk\n"
      + "    )\n"
      + " ORDER BY ?#{#pageable}",
      nativeQuery = true)
  Page<SupervisoryNode> findAllWithoutSnapshots(Pageable pageable);

  @Query(value = "SELECT\n"
          + "    sn\n"
          + "FROM\n"
          + "    SupervisoryNode sn\n"
          + " WHERE UPPER(sn.name) = UPPER(:name) and sn.id != :id ")
  Set<SupervisoryNode> findByNameIgnoreCaseContaining(@Param("name") String name,
      @Param("id") UUID id);

  @Query(value = "SELECT\n"
      + "    sn\n"
      + "FROM\n"
      + "    SupervisoryNode sn\n"
      + " WHERE UPPER(sn.code) = UPPER(:code) and sn.id != :id ")
  Set<SupervisoryNode> findByCodeCaseInsensetive(@Param("code") String code,
      @Param("id") UUID id);
}
