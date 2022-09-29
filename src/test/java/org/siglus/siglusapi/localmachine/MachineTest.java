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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.siglus.siglusapi.dto.UserDto;
import org.siglus.siglusapi.localmachine.repository.AgentInfoRepository;
import org.siglus.siglusapi.util.SiglusAuthenticationHelper;

@RunWith(MockitoJUnitRunner.class)
public class MachineTest {
  @Mock private AgentInfoRepository agentInfoRepository;
  @Mock private SiglusAuthenticationHelper siglusAuthenticationHelper;
  @InjectMocks private Machine machine;

  @Test
  public void shouldSetMachineIdWhenCalledEnsureMachineInfoExistsGivenAgentInfoNotExists() {
    // given
    given(agentInfoRepository.getMachineId()).willReturn(Optional.empty());
    // when
    machine.ensureMachineInfoExists();
    // then
    assertThat(machine.getMachineId()).isNotNull();
  }

  @Test
  public void shouldSetMachineIdWhenCalledEnsureMachineInfoExistsGivenAgentInfoExists() {
    // given
    String machineId = UUID.randomUUID().toString();
    given(agentInfoRepository.getMachineId()).willReturn(Optional.of(machineId));
    // when
    machine.ensureMachineInfoExists();
    // then
    assertThat(machine.getMachineId()).isEqualTo(UUID.fromString(machineId));
  }

  @Test
  public void shouldReturnCurrentUserHomeFacilityWhenGetFacilityIdGivenCurrentAuthenticationExists() {
    // given
    UserDto userDto = new UserDto();
    userDto.setHomeFacilityId(UUID.randomUUID());
    given(siglusAuthenticationHelper.getCurrentUser()).willReturn(userDto);
    // then
    assertThat(machine.getFacilityId()).isEqualTo(userDto.getHomeFacilityId());
  }

  @Test
  public void shouldReturnFirstSupportedFacilityWhenGetFacilityIdGivenCurrentAuthenticationNotExists() {
    // given
    given(siglusAuthenticationHelper.getCurrentUser()).willReturn(null);
    String facilityId = UUID.randomUUID().toString();
    given(agentInfoRepository.findRegisteredFacilityIds())
        .willReturn(Collections.singletonList(facilityId));
    // then
    assertThat(machine.getFacilityId()).isEqualTo(UUID.fromString(facilityId));
  }

  @Test(expected = IllegalStateException.class)
  public void shouldThrowWhenGetFacilityIdGivenNotSupportAnyFacility() {
    // given
    given(siglusAuthenticationHelper.getCurrentUser()).willReturn(null);
    given(agentInfoRepository.findRegisteredFacilityIds()).willReturn(Collections.emptyList());
    // when
    machine.getFacilityId();
  }

  @Test
  public void shouldGetDeviceInfoWhenMachineSettingUp() {
    // given
    given(agentInfoRepository.getMachineId()).willReturn(Optional.empty());

    // when
    machine.ensureMachineInfoExists();
  }
}
