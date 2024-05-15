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

import java.util.Set;
import java.util.UUID;
import org.openlmis.referencedata.domain.SupervisoryNode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SupervisoryNodeRepository extends JpaRepository<SupervisoryNode, UUID> {

  @Query(value = "select s.* from referencedata.supervisory_nodes s, referencedata.facilities f"
      + " where s.facilityid = f.id and f.typeid = :facilityTypeId", nativeQuery = true)
  Set<SupervisoryNode> findAllByFacilityTypeId(@Param("facilityTypeId") UUID facilityTypeId);

  Set<SupervisoryNode> findAllByFacilityId(UUID facilityId);

  @Query(value = "select "
      + "rgm.facilityid as cilentfacilityid "
      + "from referencedata.supervisory_nodes sn "
      + "left join referencedata.requisition_groups rg on sn.id = rg.supervisorynodeid "
      + "left join referencedata.requisition_group_members rgm on rg.id = rgm.requisitiongroupid "
      + "where sn.facilityid = :supplyFacilityId "
      + "and sn.parentid is not null "
      + "and split_part(sn.code, '.', 4) in "
      + "( "
      + "  select "
      + "  case "
      + "    when m.reportname = 'Malaria' then 'AL' "
      + "    when m.reportname = 'Requisição Balancete' then 'VIA' "
      + "  else m.reportname "
      + "  end as reportcode "
      + "  from referencedata.programs p "
      + "  left join siglusintegration.program_report_name_mapping m on p.id = m.programid "
      + "  where programid = :programId "
      + ")", nativeQuery = true)
  Set<UUID> findAllClientFacilityIdsBySupplyFacilityIdAndProgramId(@Param("supplyFacilityId") UUID supplyFacilityId,
      @Param("programId") UUID programId);
}
