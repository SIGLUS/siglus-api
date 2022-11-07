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
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.siglus.siglusapi.dto.UserDto;
import org.siglus.siglusapi.localmachine.repository.AgentInfoRepository;
import org.siglus.siglusapi.repository.FacilityExtensionRepository;
import org.siglus.siglusapi.util.SiglusAuthenticationHelper;
import org.springframework.core.env.Environment;

@RunWith(MockitoJUnitRunner.class)
public class MachineTest {
  @Mock private AgentInfoRepository agentInfoRepository;
  @Mock private SiglusAuthenticationHelper siglusAuthenticationHelper;
  @Mock private FacilityExtensionRepository facilityExtensionRepository;
  @Mock private Environment environment;
  @InjectMocks private Machine machine;

  @Before
  public void setup() {
    given(environment.getActiveProfiles()).willReturn(new String[]{"localmachine"});
  }

  @Test
  public void shouldParseSystemInfoCorrectlyWhenGetSystemInfo() throws IOException {
    // given
    String dmidecodeResult = "# dmidecode 3.2\n"
        + "Getting SMBIOS data from sysfs.\n"
        + "SMBIOS 2.7 present.\n"
        + "\n"
        + "Handle 0x0001, DMI type 1, 27 bytes\n"
        + "System Information\n"
        + "\tManufacturer: Amazon EC2\n"
        + "\tProduct Name: m5.large\n"
        + "\tVersion: Not Specified\n"
        + "\tSerial Number: ec2886be-249a-a171-88f6-f6648c7d03c0\n"
        + "\tUUID: ec2886be-249a-a171-88f6-f6648c7d03c0\n"
        + "\tWake-up Type: Power Switch\n"
        + "\tSKU Number: Not Specified\n"
        + "\tFamily: Not Specified\n";
    Machine machine = mock(Machine.class);
    Process process = mock(Process.class);
    given(machine.getSystemInfoProcess()).willReturn(process);
    given(machine.getSystemInfo()).willCallRealMethod();
    given(process.getInputStream()).willReturn(new ByteArrayInputStream(dmidecodeResult.getBytes()));
    // when
    String systemInfo = machine.getSystemInfo();
    // then
    assertThat(systemInfo).isEqualTo("Amazon EC2 m5.large");
  }

  @Test(expected = IllegalStateException.class)
  public void shouldThrowWhenGetLocalFacilityIdOnWeb() {
    machine.getLocalFacilityId();
  }

  @Test
  public void shouldBeActiveGivenSupportedFacilityIdsNotEmpty() {
    // given
    Machine machine = mock(Machine.class);
    given(machine.fetchSupportedFacilityIds()).willReturn(Collections.singleton("facility id"));
    when(machine.isActive()).thenCallRealMethod();
    // then
    assertThat(machine.isActive()).isTrue();
  }

  @Test
  public void shouldNotBeActiveGivenSupportedFacilityIdsIsEmpty() {
    // given
    Machine machine = mock(Machine.class);
    given(machine.fetchSupportedFacilityIds()).willReturn(Collections.emptySet());
    when(machine.isActive()).thenCallRealMethod();
    // then
    assertThat(machine.isActive()).isFalse();
  }

  @Test
  public void shouldReturn() {
    // given
    String nonLocalMachineFacilityId = UUID.randomUUID().toString();
    given(facilityExtensionRepository.findNonLocalMachineFacilityIds()).willReturn(
        Collections.singletonList(nonLocalMachineFacilityId));
    given(environment.getActiveProfiles()).willReturn(new String[]{});
    // then
    assertThat(machine.fetchSupportedFacilityIds()).containsExactly(nonLocalMachineFacilityId);
  }

  @Test
  public void shouldTouchMachineIdWhenCalledEnsureMachineInfoExistsGivenAgentInfoNotExists() {
    // given
    given(agentInfoRepository.getMachineId()).willReturn(Optional.empty());
    // when
    machine.ensureMachineInfoExists();
    // then
    verify(agentInfoRepository, times(1)).touchMachineId(any(UUID.class));
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
    assertThat(machine.evalEventSenderId()).isEqualTo(userDto.getHomeFacilityId());
  }

  @Test
  public void shouldReturnFirstSupportedFacilityWhenGetFacilityIdGivenCurrentAuthenticationNotExists() {
    // given
    given(siglusAuthenticationHelper.getCurrentUser()).willReturn(null);
    String facilityId = UUID.randomUUID().toString();
    given(agentInfoRepository.findRegisteredFacilityIds())
        .willReturn(Collections.singletonList(facilityId));
    // then
    assertThat(machine.evalEventSenderId()).isEqualTo(UUID.fromString(facilityId));
  }

  @Test(expected = IllegalStateException.class)
  public void shouldThrowWhenGetFacilityIdGivenNotSupportAnyFacility() {
    // given
    given(siglusAuthenticationHelper.getCurrentUser()).willReturn(null);
    given(agentInfoRepository.findRegisteredFacilityIds()).willReturn(Collections.emptyList());
    // when
    machine.evalEventSenderId();
  }

  @Test
  public void shouldGetDeviceInfoWhenMachineSettingUp() {
    // given
    given(agentInfoRepository.getMachineId()).willReturn(Optional.empty());

    // when
    machine.ensureMachineInfoExists();
  }
}
