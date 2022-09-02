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

package org.siglus.siglusapi.localmachine.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;

import java.util.Optional;
import java.util.UUID;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.referencedata.domain.Facility;
import org.openlmis.referencedata.repository.FacilityRepository;
import org.siglus.siglusapi.localmachine.Machine;
import org.siglus.siglusapi.localmachine.server.AgentInfo;
import org.siglus.siglusapi.localmachine.server.AgentInfoRepository;
import org.siglus.siglusapi.localmachine.webapi.LocalActivationRequest;

@RunWith(MockitoJUnitRunner.class)
public class LocalActivationServiceTest {
  @InjectMocks private LocalActivationService localActivationService;
  @Mock private FacilityRepository facilityRepository;
  @Mock private AgentInfoRepository agentInfoRepository;
  @Mock private OnlineWebClient onlineWebClient;
  @Mock private Machine machine;

  @Test
  public void shouldActivateSuccessfully() {
    // given
    String facilityCode = "facility-code";
    UUID machineId = UUID.randomUUID();
    Facility facility = new Facility(facilityCode);
    facility.setId(UUID.randomUUID());
    doNothing().when(onlineWebClient).activate(any());
    given(machine.getMachineId()).willReturn(machineId);
    given(agentInfoRepository.getFirstAgentInfo()).willReturn(null);
    LocalActivationRequest localActivationRequest =
        new LocalActivationRequest("activation code", facilityCode);
    given(facilityRepository.findByCode(localActivationRequest.getFacilityCode()))
        .willReturn(Optional.of(facility));
    ArgumentCaptor<AgentInfo> agentInfoCapture = ArgumentCaptor.forClass(AgentInfo.class);
    // when
    localActivationService.activate(localActivationRequest);
    // then
    verify(agentInfoRepository).save(agentInfoCapture.capture());
    assertThat(agentInfoCapture.getValue().getActivationCode())
        .isEqualTo(localActivationRequest.getActivationCode());
    assertThat(agentInfoCapture.getValue().getMachineId()).isEqualTo(machineId);
    assertThat(agentInfoCapture.getValue().getPrivateKey()).isNotNull();
    assertThat(agentInfoCapture.getValue().getPublicKey()).isNotNull();
    assertThat(agentInfoCapture.getValue().getMachineId()).isNotNull();
  }
}
