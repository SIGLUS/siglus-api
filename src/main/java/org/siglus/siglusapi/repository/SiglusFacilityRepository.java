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
import org.openlmis.referencedata.domain.Facility;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SiglusFacilityRepository extends JpaRepository<Facility, UUID>, JpaSpecificationExecutor<Facility> {

  Facility findFirstByTypeId(UUID typeId);

  @Query(value = "select f.* \n"
      + "from referencedata.facilities f \n"
      + "left join siglusintegration.facility_extension fe on f.id = fe.facilityid \n"
      + "where fe.isandroid = false \n"
      + "and fe.islocalmachine = false \n"
      + "and f.active = true \n"
      + "and f.enabled = true \n"
      + "or fe.id is null;", nativeQuery = true)
  List<Facility> findAllWebFacility();

  @Query(value = "select distinct(cast(rgm.facilityid as varchar)) \n"
      + "from referencedata.requisition_group_members rgm \n"
      + "left join referencedata.requisition_groups rg on rg.id = rgm.requisitiongroupid \n"
      + "left join referencedata.requisition_group_program_schedules rgps on rgps.requisitiongroupid = rg.id \n"
      + "left join referencedata.programs p on p.id = rgps.programid \n"
      + "left join referencedata.supervisory_nodes sn on sn.id = rg.supervisorynodeid \n"
      + "where sn.parentid is not null \n"
      + "and sn.facilityid = :facilityId \n"
      + "and rgps.programid = :programId ;", nativeQuery = true)
  List<String> findAllClientFacilityIds(@Param("facilityId") UUID facilityId, @Param("programId") UUID programId);

  @Query(value = "select f.* \n"
      + "from referencedata.facilities f \n"
      + "where f.id in (:facilityIds);", nativeQuery = true)
  List<Facility> findFacilityBasicInfoByIds(@Param("facilityIds") Collection<UUID> facilityIds);

  @Query(value = "select cast(u.id as varchar) id from referencedata.users u "
      + "left join referencedata.role_assignments ra on ra.userid = u.id "
      + "where ra.roleid = :roleId limit 1;", nativeQuery = true)
  String findAdminUserIdByAdminRoleId(@Param("roleId") UUID roleId);

  @Query(
      value =
          "select distinct code from referencedata.facilities where active=true and enabled=true",
      nativeQuery = true)
  List<String> findAllFacilityCodes();
}
