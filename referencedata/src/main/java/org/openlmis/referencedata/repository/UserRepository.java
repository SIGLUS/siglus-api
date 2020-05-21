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
import java.util.Set;
import java.util.UUID;
import org.openlmis.referencedata.domain.Facility;
import org.openlmis.referencedata.domain.Right;
import org.openlmis.referencedata.domain.User;
import org.openlmis.referencedata.repository.custom.UserRepositoryCustom;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public interface UserRepository extends
    JpaRepository<User, UUID>,
    UserRepositoryCustom,
    BaseAuditableRepository<User, UUID> {

  User findOneByUsernameIgnoreCase(@Param("username") String username);

  @Query(value = "SELECT u.*"
      + " FROM referencedata.users u"
      + " WHERE u.extradata @> (:extraData)\\:\\:jsonb",
      nativeQuery = true
  )
  List<User> findByExtraData(@Param("extraData") String extraData);

  @Query(value = "SELECT DISTINCT u.*"
      + " FROM referencedata.users u"
      + "   JOIN referencedata.role_assignments ra ON ra.userid = u.id" 
      + "   JOIN referencedata.roles r ON r.id = ra.roleid" 
      + "   JOIN referencedata.role_rights rr ON rr.roleid = r.id"
      + " WHERE rr.rightid = :rightId"
      + "   AND ra.supervisorynodeid = :supervisoryNodeId"
      + "   AND ra.programid = :programId",
      nativeQuery = true)
  Set<User> findUsersBySupervisionRight(@Param("rightId") UUID rightId,
      @Param("supervisoryNodeId") UUID supervisoryNodeId,
      @Param("programId") UUID programId);

  @Query(value = "SELECT DISTINCT u.*"
      + " FROM referencedata.users u"
      + "   JOIN referencedata.role_assignments ra ON ra.userid = u.id"
      + "   JOIN referencedata.roles r ON r.id = ra.roleid"
      + "   JOIN referencedata.role_rights rr ON rr.roleid = r.id"
      + " WHERE rr.rightid = :rightId"
      + "   AND ra.supervisorynodeid IS NULL"
      + "   AND ra.programid = :programId",
      nativeQuery = true)
  Set<User> findUsersBySupervisionRight(@Param("rightId") UUID rightId,
      @Param("programId") UUID programId);

  @Query(value = "SELECT DISTINCT u.*"
      + " FROM referencedata.users u"
      + "   JOIN referencedata.role_assignments ra ON ra.userid = u.id"
      + "   JOIN referencedata.roles r ON r.id = ra.roleid"
      + "   JOIN referencedata.role_rights rr ON rr.roleid = r.id"
      + " WHERE rr.rightid = :right"
      + "   AND ra.warehouseid = :warehouse",
      nativeQuery = true)
  Set<User> findUsersByFulfillmentRight(@Param("right") Right right,
                        @Param("warehouse") Facility warehouse);

  @Query(value = "SELECT DISTINCT u.*"
      + " FROM referencedata.users u"
      + "   JOIN referencedata.role_assignments ra ON ra.userid = u.id"
      + "   JOIN referencedata.roles r ON r.id = ra.roleid"
      + "   JOIN referencedata.role_rights rr ON rr.roleid = r.id"
      + " WHERE rr.rightid = :right",
      nativeQuery = true)
  Set<User> findUsersByDirectRight(@Param("right") Right right);

  @Query(value = "SELECT\n"
      + "    u.*\n"
      + "FROM\n"
      + "    referencedata.users u\n"
      + "WHERE\n"
      + "    id NOT IN (\n"
      + "        SELECT\n"
      + "            id\n"
      + "        FROM\n"
      + "            referencedata.users u\n"
      + "            INNER JOIN referencedata.jv_global_id g "
      + "ON CAST(u.id AS varchar) = SUBSTRING(g.local_id, 2, 36)\n"
      + "            INNER JOIN referencedata.jv_snapshot s  ON g.global_id_pk = s.global_id_fk\n"
      + "    )\n"
      + " ORDER BY ?#{#pageable}",
      nativeQuery = true)
  Page<User> findAllWithoutSnapshots(Pageable pageable);
}
