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

package org.openlmis.requisition.repository;

import java.util.List;
import java.util.UUID;
import org.javers.spring.annotation.JaversSpringDataAuditable;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.repository.custom.RequisitionRepositoryCustom;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.EntityGraph.EntityGraphType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@JaversSpringDataAuditable
public interface RequisitionRepository extends
    JpaRepository<Requisition, UUID>,
    RequisitionRepositoryCustom,
    BaseAuditableRepository<Requisition, UUID> {
  List<Requisition> findByTemplateId(@Param("templateId") UUID templateId);

  @EntityGraph(attributePaths = { "requisitionLineItems" }, type = EntityGraphType.LOAD)
  List<Requisition> readDistinctByIdIn(Iterable<UUID> ids);

  @Query(value = "SELECT\n"
      + "    r.*\n"
      + "FROM\n"
      + "    requisition.requisitions r\n"
      + "WHERE\n"
      + "    id NOT IN (\n"
      + "        SELECT\n"
      + "            id\n"
      + "        FROM\n"
      + "            requisition.requisitions r\n"
      + "            INNER JOIN requisition.jv_global_id g "
      + "ON CAST(r.id AS varchar) = SUBSTRING(g.local_id, 2, 36)\n"
      + "            INNER JOIN requisition.jv_snapshot s  ON g.global_id_pk = s.global_id_fk\n"
      + "    )\n"
      + " ORDER BY ?#{#pageable}",
      nativeQuery = true)
  Page<Requisition> findAllWithoutSnapshots(Pageable pageable);

  @Query(value = "SELECT CASE WHEN count(r) > 0 THEN true ELSE false END"
      + " FROM requisition.requisitions r"
      + " WHERE (r.extradata ->> 'originalRequisition')\\:\\:uuid = :originalRequisitionId",
      nativeQuery = true
  )
  boolean existsByOriginalRequisitionId(@Param("originalRequisitionId") UUID originalRequisitionId);
}
