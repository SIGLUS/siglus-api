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

package org.siglus.siglusapi.localmachine.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.siglus.siglusapi.localmachine.domain.AgentInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface AgentInfoRepository extends JpaRepository<AgentInfo, UUID> {

  AgentInfo findOneByMachineIdAndFacilityId(UUID agentId, UUID facilityId);

  AgentInfo findFirstByFacilityCode(String facilityCode);

  @Query(
      value =
          "select a.* from localmachine.machine m inner join localmachine.agents a on m.id=a.machineid limit 1",
      nativeQuery = true)
  AgentInfo getLocalAgent();

  @Query(
      value = "select cast(id as varchar) as id from localmachine.machine limit 1",
      nativeQuery = true)
  Optional<String> getMachineId();

  @Query(
      value =
          "select cast(ag.facilityid as varchar) as id from localmachine.machine m "
              + "INNER JOIN localmachine.agents ag on m.id=ag.machineid",
      nativeQuery = true)
  List<String> findRegisteredFacilityIds();

  @Transactional
  @Modifying
  @Query(
      value = "insert into localmachine.machine(id) values (?1) ON CONFLICT DO NOTHING",
      nativeQuery = true)
  void touchMachineId(UUID machineId);

  void deleteByFacilityId(UUID facilityId);

  @Modifying
  @Query(
      value = "delete from localmachine.agents where 1 = 1",
      nativeQuery = true)
  void deleteLocalMachineAgents();
}
