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
import org.siglus.siglusapi.localmachine.domain.AgentInfo;
import org.siglus.siglusapi.localmachine.repository.AgentInfoRepository;
import org.siglus.siglusapi.localmachine.webapi.LocalActivationRequest;

@RunWith(MockitoJUnitRunner.class)
public class LocalActivationServiceTest {

  private static final String FACILITY_CODE = "facility-code";
  private static final String ACTIVATION_CODE = "activation code";
  @InjectMocks private LocalActivationService localActivationService;
  @Mock private FacilityRepository facilityRepository;
  @Mock private AgentInfoRepository agentInfoRepository;
  @Mock private OnlineWebClient onlineWebClient;
  @Mock private Machine machine;

  @Test
  public void shouldActivateSuccessfully() {
    // given
    UUID machineId = UUID.randomUUID();
    Facility facility = new Facility(FACILITY_CODE);
    facility.setId(UUID.randomUUID());
    doNothing().when(onlineWebClient).activate(any());
    given(machine.getMachineId()).willReturn(machineId);
    given(agentInfoRepository.getLocalAgent()).willReturn(null);
    LocalActivationRequest localActivationRequest =
        new LocalActivationRequest(ACTIVATION_CODE, FACILITY_CODE);
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

  @Test
  public void shouldReturnTrueWhenCheckIfNeedToActivateGivenNotActivatedYet() {
    // given
    LocalActivationRequest localActivationRequest =
        new LocalActivationRequest(ACTIVATION_CODE, FACILITY_CODE);
    given(agentInfoRepository.getLocalAgent()).willReturn(null);
    // when
    boolean needToActivate = localActivationService.checkForActivation(localActivationRequest);
    // then
    assertThat(needToActivate).isTrue();
  }

  @Test
  public void shouldReturnTrueWhenCheckIfNeedToActivateGivenActivationCodeChanged() {
    // given
    LocalActivationRequest localActivationRequest =
        new LocalActivationRequest(ACTIVATION_CODE, FACILITY_CODE);
    given(agentInfoRepository.getLocalAgent())
        .willReturn(
            AgentInfo.builder()
                .activationCode("another activation code")
                .facilityCode(FACILITY_CODE)
                .build());
    // when
    boolean needToActivate = localActivationService.checkForActivation(localActivationRequest);
    // then
    assertThat(needToActivate).isTrue();
  }

  @Test(expected = IllegalStateException.class)
  public void shouldReturnTrueWhenCheckIfNeedToActivateGivenFacilityChanged() {
    // given
    String facilityCode = "facility-code";
    LocalActivationRequest localActivationRequest =
        new LocalActivationRequest(ACTIVATION_CODE, facilityCode);
    given(agentInfoRepository.getLocalAgent())
        .willReturn(
            AgentInfo.builder()
                .activationCode(localActivationRequest.getActivationCode())
                .facilityCode("another facility code")
                .build());
    // when
    localActivationService.checkForActivation(localActivationRequest);
  }

  @Test
  public void shouldReturnFalseWhenCheckIfNeedToActivateGivenActivatedInfoNotChanged() {
    // given
    LocalActivationRequest localActivationRequest =
        new LocalActivationRequest(ACTIVATION_CODE, FACILITY_CODE);
    given(agentInfoRepository.getLocalAgent())
        .willReturn(
            AgentInfo.builder()
                .activationCode(localActivationRequest.getActivationCode())
                .facilityCode(FACILITY_CODE)
                .build());
    // when
    boolean needToActivate = localActivationService.checkForActivation(localActivationRequest);
    // then
    assertThat(needToActivate).isFalse();
  }
}
