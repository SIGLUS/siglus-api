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

package org.siglus.siglusapi.localmachine;

import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import javax.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.siglus.siglusapi.dto.UserDto;
import org.siglus.siglusapi.localmachine.repository.AgentInfoRepository;
import org.siglus.siglusapi.util.SiglusAuthenticationHelper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class Machine {
  private final AgentInfoRepository agentInfoRepository;
  private final SiglusAuthenticationHelper siglusAuthenticationHelper;

  @Getter private UUID machineId;

  public Set<String> fetchSupportedFacilityIds() {
    return new HashSet<>(agentInfoRepository.findRegisteredFacilityIds());
  }

  @PostConstruct
  public void ensureMachineInfoExists() {
    machineId = agentInfoRepository.getMachineId().map(UUID::fromString).orElse(null);
    if (Objects.nonNull(machineId)) {
      return;
    }
    UUID tempMachineId = UUID.randomUUID();
    log.info("touch machine id:{}", tempMachineId);
    agentInfoRepository.touchMachineId(tempMachineId);
    machineId = tempMachineId;
  }

  public UUID getFacilityId() {
    return Optional.ofNullable(siglusAuthenticationHelper.getCurrentUser())
        .map(UserDto::getHomeFacilityId)
        .orElseGet(
            () ->
                UUID.fromString(
                    this.fetchSupportedFacilityIds().stream()
                        .findFirst()
                        .orElseThrow(
                            () -> new IllegalStateException("can not resolve local facility id"))));
  }
}
